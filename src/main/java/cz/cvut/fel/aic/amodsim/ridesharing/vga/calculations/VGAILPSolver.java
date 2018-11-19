package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;

import java.util.*;

@Singleton
public class VGAILPSolver {

    //https://developers.google.com/optimization/install/java/
    static {
        System.loadLibrary("jniortools");
    }
	
	
	private final PlanCostComputation planCostComputation;
	

	
	
	@Inject
    public VGAILPSolver(PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
	}

    public Map<VGAVehicle, VGAVehiclePlan> assignOptimallyFeasiblePlans(
			Map<VGAVehicle, Set<VGAVehiclePlan>> feasiblePlans, Set<VGARequest> requests) {

        //Calculating size of the model
        Map<VGAVehicle, VGAVehiclePlan> optimalPlans = new LinkedHashMap<>();
        int size = 0, noOfVehicles = feasiblePlans.size() - 1;

        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            size += entry.getValue().size();
        }

        System.out.println("Total number of feasible groups is: " + size + "\n");
        System.out.println("Creating ILP model");

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
        MPSolver solver = new MPSolver("solver", MPSolver.OptimizationProblemType.BOP_INTEGER_PROGRAMMING);
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
            System.out.println();
        }

        System.out.println("Solving ILP...");

        double results[] = new double[size];

        //Solving with Google OR

        objective.setMinimization();

        solver.setTimeLimit(10000);
        MPSolver.ResultStatus status = solver.solve();
        if (status == MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("Google optimization tools found an optimal solution.");
        } else if (status == MPSolver.ResultStatus.FEASIBLE) {
            System.out.println("Google optimization tools found a solution, but it was not able to prove in the given time limit, that it is optimal.");
        } else if (status == MPSolver.ResultStatus.INFEASIBLE) {
            System.out.println("Oops, the model is infeasible, it was probably created in a wrong way.");
        }

        for (int j = 0; j < size; j++) {
            results[j] = mpVariables[j].solutionValue();
        }

        //Printing the results

        System.out.println();
        System.out.println("Google OR results: " + Arrays.toString(results));
        System.out.println("The optimal vehicle plans are:");

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

        return optimalPlans;
    }

}
