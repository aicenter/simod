package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.quantego.clp.CLP;
import com.quantego.clp.CLPVariable;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;

import java.util.*;

public class VGAILPSolver {

    //https://developers.google.com/optimization/install/java/
    static {
        System.loadLibrary("jniortools");
    }

    private VGAILPSolver() {}

    public static Map<VGAVehicle, VGAVehiclePlan> assignOptimallyFeasiblePlans(Map<VGAVehicle, Set<VGAVehiclePlan>> feasiblePlans, List<VGARequest> requests){

        //Calculating size of the model

        Map<VGAVehicle, VGAVehiclePlan> optimalPlans = new LinkedHashMap<>();
        int size = 0, noOfVehicles = feasiblePlans.size() - 1;

        for(Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
            size += entry.getValue().size();
        }

        System.out.println("Total number of feasible groups is: " + size + "\n");
        System.out.println("Creating ILP model");

        //Calculating costs

        double[] costs = new double[size];
        double avgCost = 0;

        int i = 0;
        for(Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
            int j = 0;
            for(VGAVehiclePlan plan : entry.getValue()){
                int id = entry.getKey().getId();
                if(id < noOfVehicles) {
                    costs[i] = plan.calculateCost();
                    avgCost += costs[i];

                    if(id == noOfVehicles - 1 && j == entry.getValue().size() - 1){
                        avgCost /= size - noOfVehicles - requests.size();
                        avgCost = avgCost == 0 ? 1 : avgCost;
                    }

                } else {
                    if (VGAVehiclePlan.getCostType() == VGAVehiclePlan.CostType.STANDARD) {
                        costs[i] = 100 * avgCost;
                    } else if (VGAVehiclePlan.getCostType() == VGAVehiclePlan.CostType.SUM_OF_DROPOFF_TIMES) {
                        costs[i] = 1000;
                    }
                }
                i++;
                j++;
            }
        }

        //CLP and Google OR Tools initializing variables and the objective function

        CLP solver = new CLP();
        CLPVariable variables[] = new CLPVariable[size];

        MPSolver solver1 = new MPSolver("solver", MPSolver.OptimizationProblemType.BOP_INTEGER_PROGRAMMING);
        MPVariable[] mpVariables = new MPVariable[size];

        for (int j = 0; j < size; j++) {
            variables[j] = solver.addVariable().bounds(0, 1);
            mpVariables[j] = solver1.makeBoolVar(Integer.toString(j));
        }

        solver.createExpression().add(costs, variables).asObjective();

        MPObjective objective = solver1.objective();
        for (int j = 0; j < size; j++) {
            objective.setCoefficient(mpVariables[j], costs[j]);
        }

        //Creating and adding the constraints

        i = 0;
        for (Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            double[] constraint = new double[size];

            for(VGAVehiclePlan ignored : entry.getValue()){
                constraint[i++] = 1;
            }

            int id = entry.getKey().getId();
            if(id != noOfVehicles) {
                solver.createExpression().add(constraint, variables).eq(1);
                MPConstraint constraint1 = solver1.makeConstraint(1.0, 1.0);
                for (int k = 0; k < size; k++) {
                    constraint1.setCoefficient(mpVariables[k], constraint[k]);
                }
            }
        }

        for (VGARequest request : requests) {
            i = 0;
            double[] constraint = new double[size];

            for(Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
                for(VGAVehiclePlan plan : entry.getValue()){
                    if(plan.getRequests().contains(request)){
                        constraint[i] = 1;
                    }
                    i++;
                }
            }
            solver.createExpression().add(constraint, variables).eq(1);

            MPConstraint constraint1 = solver1.makeConstraint(1.0, 1.0);
            for (int k = 0; k < size; k++) {
                constraint1.setCoefficient(mpVariables[k], constraint[k]);
            }
        }

        System.out.println("Solving ILP...");

        //Solving with CLP first

        boolean clpSolved = true;
        double results[] = new double[size];
        double results1[] = new double[size];

        solver.minimize();

        for (int j = 0; j < size; j++) {
            results[j] = variables[j].getSolution();
            if (results[j] != 0 && results[j] != 1) {
                clpSolved = false;
            }
        }

        //Solving with Google OR if needed

        if(clpSolved) {
            System.out.println("CLP solved successfully.");
        } else {
            System.out.println("CLP was not able to produce a binary solution, Google OR tools, will find one.");
            objective.setMinimization();

            solver1.setTimeLimit(10000);
            MPSolver.ResultStatus status = solver1.solve();
            if(status == MPSolver.ResultStatus.OPTIMAL) {
                System.out.println("Google optimization tools found an optimal solution.");
            } else if (status == MPSolver.ResultStatus.FEASIBLE) {
                System.out.println("Google optimization tools found a solution, but it was not able to prove in the given time limit, that it is optimal.");
            } else if (status == MPSolver.ResultStatus.INFEASIBLE) {
                System.out.println("Oops, the model is infeasible, it was probably created in a wrong way.");
            }

            for (int j = 0; j < size; j++) {
                results1[j] = mpVariables[j].solutionValue();
            }
        }

        //Printing the results

        System.out.println();

        System.out.println("CLP results:       " + Arrays.toString(results) + System.getProperty("line.separator"));
        if(!clpSolved) {
            System.out.println("Google OR results: " + Arrays.toString(results1));
        }

        System.out.println("The optimal vehicle plans are:");

        double totalDiscomfort = 0, totalTimeInOperation = 0;

        i = 0;
        for(Map.Entry<VGAVehicle, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            for(VGAVehiclePlan plan : entry.getValue()) {
                if((clpSolved ? results[i] : results1[i]) == 1) {
                    optimalPlans.put(entry.getKey(), plan);

                    if (!plan.toString().equals("")) {
                        int id = entry.getKey().getId();
                        if(id < noOfVehicles) {
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

        return optimalPlans;
    }

}
