package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import com.google.ortools.linearsolver.MPVariable;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VGAILPSolver {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VGAILPSolver.class);

    //https://developers.google.com/optimization/install/java/
    static {
        System.loadLibrary("jniortools");
    }
	
	
	private final PlanCostComputation planCostComputation;
	

	
	int iteration;
	
	
	@Inject
    public VGAILPSolver(PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
		iteration = 1;
	}

    public Map<VGAVehicle, VGAVehiclePlan> assignOptimallyFeasiblePlans(
			Map<VGAVehicle, Set<VGAVehiclePlan>> feasiblePlans, LinkedHashSet<VGARequest> requests) {

        //Calculating size of the model
        Map<VGAVehicle, VGAVehiclePlan> optimalPlans = new LinkedHashMap<>();
        int size = 0, noOfVehicles = feasiblePlans.size() - 1;

        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            size += entry.getValue().size();
        }

        LOGGER.info("Total number of feasible groups is: " + size + "\n");
        LOGGER.info("Creating ILP model");

        //Calculating costs
        boolean once = true;
        double[] costs = new double[size];
        double avgCost = 0;

        int i = 0;
        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            for (VGAVehiclePlan plan : entry.getValue()) {
                if (entry.getKey().getRidesharingVehicle() != null) {
                    costs[i] = planCostComputation.calculatePlanCost(plan);
                    avgCost += costs[i];
                } 
				// this else block currently works only if there are some vehicles !!
				else {
                    if (once) {
                        once = false;
                        avgCost /= size - noOfVehicles - requests.size();
                        avgCost = avgCost == 0 ? 1 : avgCost;
                    }

                    if (PlanCostComputation.COST_TYPE == VGAVehiclePlan.CostType.STANDARD) {
                        costs[i] = 100 * avgCost;
                    } else if (PlanCostComputation.COST_TYPE == VGAVehiclePlan.CostType.SUM_OF_DROPOFF_TIMES) {
                        costs[i] = 1000;
                    }
                }
                i++;
            }
        }

        //Initializing variables and the objective function
        MPSolver solver = new MPSolver("solver", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
        MPVariable[] mpVariables = new MPVariable[size];

        for (int j = 0; j < size; j++) {
            mpVariables[j] = solver.makeBoolVar(Integer.toString(j));
        }

        MPObjective objective = solver.objective();
        for (int j = 0; j < size; j++) {
            objective.setCoefficient(mpVariables[j], costs[j]);
        }

        //Creating and adding the constraints
        //Each vehicle needs to have exactly one plan assigned
        i = 0;
        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            if (entry.getValue().size() == 0) continue;
            double[] constraint = new double[size];

            for (VGAVehiclePlan ignored : entry.getValue()) {
                constraint[i++] = 1;
            }

            if (entry.getKey().getRidesharingVehicle() != null) {
                MPConstraint constraint1 = solver.makeConstraint(1.0, 1.0);
                for (int k = 0; k < size; k++) {
                    constraint1.setCoefficient(mpVariables[k], constraint[k]);
                }
            }
        }

        //Each request needs to be assigned to exactly one vehicle
        //Only one constraint for the one new request is generated,
        // all of the ones before are already assigned to some vehicle,
        //which means that they will not show up in other vehicles' plans.
        for (VGARequest request : requests) {
            i = 0;
            double[] constraint = new double[size];

            for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
                for (VGAVehiclePlan plan : entry.getValue()) {
                    if (plan.getRequests().contains(request)) {
                        constraint[i] = 1;
                    }
                    i++;
                }
            }

            MPConstraint constraint1 = solver.makeConstraint(1.0, 1.0);
            for (int k = 0; k < size; k++) {
                constraint1.setCoefficient(mpVariables[k], constraint[k]);
            }
        }

        LOGGER.info("Solving ILP...");

        double results[] = new double[size];

        //Solving with Google OR

        objective.setMinimization();

        solver.setTimeLimit(10000000);
//		MPSolverParameters params = new MPSolverParameters();
		boolean sucess = solver.setSolverSpecificParametersAsString("preprocess off");
		if(!sucess){
			LOGGER.error("Setting solver param was not succesful");
		}
//		solver.enableOutput();
		String problemDefinition = solver.exportModelAsLpFormat(false);
		long startTime = System.nanoTime();
        MPSolver.ResultStatus status = solver.solve();
		long totalTime = System.nanoTime() - startTime;
		exportSolver(problemDefinition, totalTime);
        if (status == MPSolver.ResultStatus.OPTIMAL) {
            LOGGER.info("Google optimization tools found an optimal solution.");
        } else if (status == MPSolver.ResultStatus.FEASIBLE) {
            LOGGER.info("Google optimization tools found a solution, but it was not able to prove in the given time limit, that it is optimal.");
        } else if (status == MPSolver.ResultStatus.INFEASIBLE) {
            LOGGER.info("Oops, the model is infeasible, it was probably created in a wrong way.");
        }

        for (int j = 0; j < size; j++) {
            results[j] = mpVariables[j].solutionValue();
        }

        //Printing the results
        LOGGER.info("Google OR results: " + Arrays.toString(results));
        LOGGER.info("The optimal vehicle plans are:");

        double totalDiscomfort = 0, totalTimeInOperation = 0;

        i = 0;
        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            for (VGAVehiclePlan plan : entry.getValue()) {
                if (results[i] == 1) {
                    optimalPlans.put(entry.getKey(), plan);

                    if (!plan.toString().equals("")) {
                        if (entry.getKey().getRidesharingVehicle() != null) {
                            System.out.println(entry.getKey().getRidesharingVehicle().toString());
                        }
                        System.out.print(plan.toString());

                        totalDiscomfort += plan.getDiscomfort();
                        totalTimeInOperation += plan.getCurrentTime();
                    }

                }
                i++;
            }
        }

        System.out.println(String.format(System.getProperty("line.separator") + "Total discomfort: %.2fs", totalDiscomfort));
        System.out.println(String.format("Total time in operation: %.2fs", totalTimeInOperation));
        System.out.println();
        System.out.println();

		iteration++;
		
        return optimalPlans;
    }

	private void exportSolver(String problemDefinition, long totalTime) {
		String firstLine = String.format("/* Totaltime: %s */\n", totalTime);
		String content = firstLine + problemDefinition;
		
		String filename = String.format("%s_vga-iteration_%s.lp", getCurrentTimeStamp(), iteration);
		String filepath = "C:/AIC data/Shared/amod-data/agentpolis-experiment/Prague/experiment/test/vga-problems/" 
				+ filename;
		
		File outputFile = new File(filepath);
		
		try {
			Files.asCharSink(outputFile, Charset.forName("utf-8")).write(content);
		} catch (IOException ex) {
			Logger.getLogger(VGAILPSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSS");//dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

}
