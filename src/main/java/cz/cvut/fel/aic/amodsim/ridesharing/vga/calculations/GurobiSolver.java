/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

//import gurobi.*;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.CollectionUtil;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VehiclePlanList;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author LocalAdmin
 */
@Singleton
public class GurobiSolver {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GurobiSolver.class);
	
	
	private final PlanCostComputation planCostComputation;
	
	private GRBEnv env;

	
	int iteration;
	
	
	
	
	@Inject
    public GurobiSolver(PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
		env = null;
		try {
			env = new GRBEnv("mip.log");
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		iteration = 1;
	}
	
	public Map<VGAVehicle, VGAVehiclePlan> assignOptimallyFeasiblePlans(
			List<VehiclePlanList> feasiblePlans, LinkedHashSet<VGARequest> requests) {
		
		try {
			// solver init
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
			
			// map for solution processing
			Map<GRBVar,VGAVehiclePlan> variablePlanMap = new HashMap<>();
			
			// map for request constraint generation
			Map<VGARequest,List<GRBVar>> requestVariableMap = new HashMap<>();
			
			int vehicleCounter = 0;
			for (VehiclePlanList vehicleEntry : feasiblePlans) {
				
				GRBLinExpr vehicleConstraint = new GRBLinExpr();
				String vehicleId = vehicleEntry.vGAVehicle.getRidesharingVehicle().getId();
				
				int groupCounter = 0;
				for (VGAVehiclePlan plan : vehicleEntry.feasibleGroupPlans) {
					
					// variables
					String newVarName = String.format("vehicle: %s, group: %s", 
							vehicleId, groupCounter);
					GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
					variablePlanMap.put(newVar, plan);
					
					// objective
					double cost = planCostComputation.calculatePlanCost(plan);
					objetive.addTerm(cost, newVar);
					
					// constraint 1 - exactly one plan per vehicle
					vehicleConstraint.addTerm(1.0, newVar);
					
					// filling map for constraint 2 
					for (VGARequest request: plan.getRequests()) {
						CollectionUtil.addToListInMap(requestVariableMap, request, newVar);
					}
					
					groupCounter++;
				}
				
				String vehicleConstraintName = String.format("One plan per vehicle - vehicle %s", vehicleId);
				model.addConstr(vehicleConstraint, GRB.EQUAL, 1.0, vehicleConstraintName);
				
				vehicleCounter++;
			}
			
			// dropping variables generation (y_r)
			int requestCounter = 0;
			for (VGARequest request : requests) {
				
				// variables
				String newVarName = String.format("droping request %s", request.id);
				GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
				
				// objective
				objetive.addTerm(10000, newVar);
				
				// filling map for constraint 2 
				CollectionUtil.addToListInMap(requestVariableMap, request, newVar);
				
				requestCounter++;
			}
			
			// constraint 2 - exactly one plan for each request
			requestCounter = 0;
			for (Map.Entry<VGARequest, List<GRBVar>> entry : requestVariableMap.entrySet()) {
				VGARequest request = entry.getKey();
				List<GRBVar> planVariableList = entry.getValue();
				
				GRBLinExpr requestConstraint = new GRBLinExpr();
				
				for (GRBVar planVariable : planVariableList) {
					requestConstraint.addTerm(1.0, planVariable);
				}
				
				String requestConstraintName = String.format("One plan per request - request %s", request.id);
				model.addConstr(requestConstraint, GRB.EQUAL, 1.0, requestConstraintName);
				
				requestCounter++;
			}
			
			
			model.setObjective(objetive, GRB.MINIMIZE);
			LOGGER.info("solving start");
			model.optimize();
			LOGGER.info("solving finished");
			
			LOGGER.info("Objective function value: {}", model.get(GRB.DoubleAttr.ObjVal));
			
			// create output from solution
			Map<VGAVehicle, VGAVehiclePlan> optimalPlans = new LinkedHashMap<>();
			
			for (Map.Entry<GRBVar, VGAVehiclePlan> entry : variablePlanMap.entrySet()) {
				GRBVar variable = entry.getKey();
				VGAVehiclePlan plan = entry.getValue();
				
				if(variable.get(GRB.DoubleAttr.X) == 1.0){
					optimalPlans.put(plan.vgaVehicle, plan);
				}
			}
			
			model.dispose();
			
			return optimalPlans;
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}
}
