package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.optimizers.BIPLokbaTableMethod;
import com.joptimizer.optimizers.BIPOptimizationRequest;
import com.quantego.clp.CLP;
import com.quantego.clp.CLPVariable;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VGAILPSolver {

    private VGAILPSolver() {}

    public static Set<VGAVehiclePlan> assignOptimallyFeasiblePlans(Map<AgentPolisEntity, Set<VGAVehiclePlan>> feasiblePlans, List<VGARequest> requests){

        //Calculating size of the model

        Set<VGAVehiclePlan> optimalPlans = new LinkedHashSet<>();
        int size = 0, noOfVehicles = feasiblePlans.size() - 1;

        for(Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
            size += entry.getValue().size();
        }

        //Putting the feasible plans in an array (only for debugging purposes)

        VGAVehiclePlan[] plans = new VGAVehiclePlan[size];

        int i = 0;
        for(Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            for(VGAVehiclePlan plan : entry.getValue()) {
                plans[i++] = plan;
            }
        }

        System.out.println("Total number of feasible groups is: " + size + "\n");
        System.out.println("Creating ILP model");

        //Calculating costs

        double[] costs = new double[size];
        double avgCost = 0;

        i = 0;
        for(Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
            int j = 0;
            for(VGAVehiclePlan plan : entry.getValue()){
                //TODO next line parsing may be a major flaw
                int id = Integer.parseInt(entry.getKey().getId());
                if(id < noOfVehicles) {
                    costs[i] = plan.calculateCost();
                    avgCost += costs[i];

                    if(id == noOfVehicles - 1 && j == entry.getValue().size() - 1){
                        avgCost /= size - requests.size();
                        avgCost = avgCost == 0 ? 1 : avgCost;
                    }

                } else {
                    if(j == 0) {
                        costs[i] = 0;
                    } else {
                        if (VGAVehiclePlan.getCostType() == VGAVehiclePlan.CostType.STANDARD) {
                            costs[i] = 100 * avgCost;
                        } else if (VGAVehiclePlan.getCostType() == VGAVehiclePlan.CostType.SUM_OF_DROPOFF_TIMES) {
                            costs[i] = 1000;
                        }
                    }
                }
                i++;
                j++;
            }
        }

        //CLP Initializing variables and objective

        CLP solver = new CLP();
        CLPVariable variables[] = new CLPVariable[size];
        for (int j = 0; j < size; j++) {
            variables[j] = solver.addVariable().bounds(0, 1);
        }

        solver.createExpression().add(costs, variables).asObjective();

        //JOptimizer setting the objective and right hand side of the constraints

        double constraints[][] = new double[(noOfVehicles + requests.size()) * 2][size];
        double h[] = new double[(noOfVehicles + requests.size()) * 2];
        for (int j = 0; j < 2 * (noOfVehicles + requests.size()); j+=2) {
            h[j] = 1;
            h[j + 1] = -1;
        }

        BIPOptimizationRequest bipor = new BIPOptimizationRequest();
        bipor.setC(costs);
        bipor.setH(h);

        //Creating the constraints

        int j;
        i = j = 0;
        for (Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            double[] constraint = new double[size];

            for(VGAVehiclePlan ignored : entry.getValue()){
                constraint[i++] = 1;
            }

            //TODO next line parsing may be a major flaw
            int id = Integer.parseInt(entry.getKey().getId());
            if(id != noOfVehicles) {
                solver.createExpression().add(constraint, variables).eq(1);
                constraints[j++] = constraint;
                constraints[j++] = constraint.clone();
                for (int k = 0; k < size; k++) {
                    constraints[j-1][k] *= -1;
                }
            }
        }

        for (VGARequest request : requests) {
            i = 0;
            double[] constraint = new double[size];

            for(Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()){
                for(VGAVehiclePlan plan : entry.getValue()){
                    if(plan.getRequests().contains(request)){
                        constraint[i] = 1;
                    }
                    i++;
                }
            }
            solver.createExpression().add(constraint, variables).eq(1);
            constraints[j++] = constraint;
            constraints[j++] = constraint.clone();
            for (int k = 0; k < size; k++) {
                constraints[j - 1][k] *= -1;
            }
        }

        System.out.println("Solving ILP...");

        bipor.setG(constraints);

        //Solving

        boolean warn = false;
        double results[] = new double[size];

        solver.minimize();

        for (j = 0; j < size; j++) {
            results[j] = variables[j].getSolution();
            if (results[j] != 0 && results[j] != 1) {
                System.out.println("j: " + j + " value: " + results[j] + " ");
                warn = true;
            }
        }

        if(warn){
            Logger.getLogger(VGAILPSolver.class.getName()).log(Level.WARNING,
                    "Some ILP variable results are not binary." + System.getProperty("line.separator") +
                            "Retrying the calculation with another ILP solver. " + System.getProperty("line.separator") +
                            "This will take a lot more time.");

            BIPLokbaTableMethod opt = new BIPLokbaTableMethod();
            opt.setBIPOptimizationRequest(bipor);
            try {
                opt.optimize();
            } catch (JOptimizerException e) {
                e.printStackTrace();
            }

            j = 0;
            for (Integer integer : opt.getBIPOptimizationResponse().getSolution()) {
                results[j++] = integer;
            }

        }

        //Printing the results

        System.out.println();

        System.out.println("BILP solved, groups assigned.");
        System.out.println(Arrays.toString(results) + "\n");

        System.out.println("The optimal vehicle plans are:");

        double totalDiscomfort = 0, totalTimeInOperation = 0;

        i = 0;
        for(Map.Entry<AgentPolisEntity, Set<VGAVehiclePlan>> entry : feasiblePlans.entrySet()) {
            for(VGAVehiclePlan plan : entry.getValue()) {
                if(variables[i].getSolution() == 1) {
                    optimalPlans.add(plan);

                    if (!plan.toString().equals("")) {
                        int id = Integer.parseInt(entry.getKey().getId());
                        if(id < noOfVehicles) {
                            System.out.println("Vehicle id: " + id);
                        }
                        System.out.print(plan.toString());

                        totalDiscomfort += plan.getDiscomfort();
                        totalTimeInOperation += plan.getCurrentTime();
                    }

                }
                i++;
            }
        }

        System.out.println(String.format("\nTotal discomfort: %.2fs", totalDiscomfort));
        System.out.println(String.format("Total time in operation: %.2fs", totalTimeInOperation));

        return optimalPlans;
    }

}
