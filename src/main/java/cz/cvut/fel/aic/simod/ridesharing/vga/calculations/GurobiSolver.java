/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.ridesharing.vga.calculations;

//import gurobi.*;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.amodsim.SimodException;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VehiclePlanList;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VirtualVehicle;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
	
	
	private final StandardPlanCostProvider planCostComputation;
	
	private final int timeLimit;

	private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;
	
	private GRBEnv env;

	
	int iteration;
	
	double gap;

	private SimodConfig config;
	
	
	public double getGap() {
		return gap;
	}
	
	
	
	@Inject
	public GurobiSolver(StandardPlanCostProvider planCostComputation, SimodConfig config, 
			DroppedDemandsAnalyzer droppedDemandsAnalyzer) {
		this.planCostComputation = planCostComputation;
		this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
                this.config = config;
		iteration = 1;
		timeLimit = config.ridesharing.vga.solverTimeLimit;
		
		// env init
		env = null;           
		try {                       
			env = new GRBEnv(config.simodExperimentDir +"/log/mip.log");
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}
	
	public List<Plan<IOptimalPlanVehicle>> assignOptimallyFeasiblePlans(
			List<VehiclePlanList> feasiblePlans, LinkedHashSet<PlanComputationRequest> requests, 
			int[] usedVehiclesPerStation) {
		
		Collections.sort(feasiblePlans);
		
//		// If the number of trips/vehicles/groups per request is limited
//		if(config.ridesharing.vga.solverMaxTripsPerRequest > 0){
//			
//		}
		
		try {
			// solver init
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
			
			// map for solution processing
			Map<GRBVar,Plan<IOptimalPlanVehicle>> variablePlanMap = new LinkedHashMap<>();
			
			// map for request constraint generation
			Map<PlanComputationRequest,List<GRBVar>> requestVariableMap = new LinkedHashMap<>();
			
			int vehicleCounter = 0;
			for (VehiclePlanList vehicleEntry : feasiblePlans) {
				
				GRBLinExpr vehicleConstraint = new GRBLinExpr();
				String vehicleId = vehicleEntry.optimalPlanVehicle.getId();
				
				int groupCounter = 0;
				for (Plan<IOptimalPlanVehicle> plan : vehicleEntry.feasibleGroupPlans) {
					
					// variables
					String newVarName = String.format("vehicle: %s, group: %s", 
							vehicleId, groupCounter);
					GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
					variablePlanMap.put(newVar, plan);
					
					// objective
//					double cost = planCostComputation.calculatePlanCost(plan);
					objetive.addTerm(plan.getCost(), newVar);
					
					// constraint 1 - exactly one plan per vehicle
					vehicleConstraint.addTerm(1.0, newVar);
					
					// filling map for constraint 2 
					for (PlanRequestAction action: plan.getActions()) {
						
						// add variable once to each request list
						if(action instanceof PlanActionDropoff){
							CollectionUtil.addToListInMap(requestVariableMap, (DefaultPlanComputationRequest) action.getRequest(), newVar);
						}
					}
					
					groupCounter++;
				}
				
				String vehicleConstraintName = String.format("One plan per vehicle - vehicle %s", vehicleId);
				
				/* Adding constraint 1 into model */
				
				// normal vehicles 
				if(vehicleEntry.optimalPlanVehicle instanceof VGAVehicle){
					model.addConstr(vehicleConstraint, GRB.EQUAL, 1.0, vehicleConstraintName);
				}
				// virtual vehicles
				else{
					int limit = ((VirtualVehicle) vehicleEntry.optimalPlanVehicle).getCarLimit();
					model.addConstr(vehicleConstraint, GRB.LESS_EQUAL, limit, vehicleConstraintName);
				}
				
				
				vehicleCounter++;
			}
			
			// dropping variables generation (y_r)
			int requestCounter = 0;
			Map<GRBVar,PlanComputationRequest> droppingVarsMap = new LinkedHashMap<>();
			for (PlanComputationRequest request : requests) {
				if(!request.isOnboard()){
					// variables
					String newVarName = String.format("droping request %s", request.getId());
					GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);

					// objective
					objetive.addTerm(1_000_000_000, newVar);

					// filling map for constraint 2 
					CollectionUtil.addToListInMap(requestVariableMap, request, newVar);

					droppingVarsMap.put(newVar, request);

					requestCounter++;
				}
			}
			
			// constraint 2 - exactly one plan for each request
			requestCounter = 0;
			for (Map.Entry<PlanComputationRequest, List<GRBVar>> entry : requestVariableMap.entrySet()) {
				PlanComputationRequest request = entry.getKey();
				List<GRBVar> planVariableList = entry.getValue();
				
				GRBLinExpr requestConstraint = new GRBLinExpr();
				
				for (GRBVar planVariable : planVariableList) {
					requestConstraint.addTerm(1.0, planVariable);
				}
				
				String requestConstraintName = String.format("One plan per request - request %s", request.getId());
				model.addConstr(requestConstraint, GRB.EQUAL, 1.0, requestConstraintName);
				
				requestCounter++;
			}
			
			
			model.setObjective(objetive, GRB.MINIMIZE);
			
			/* MODEL CONFIG AND RUN */
			
//			// solution can be 1% worse than the optimal solution
//			model.set(GRB.DoubleParam.MIPGap, 0.01);
		
			// time limit
			if(timeLimit > 0){
				model.set(GRB.DoubleParam.TimeLimit, timeLimit);
			}
			
			// min gap
			if(config.ridesharing.vga.solverMinGap > 0){
				model.set(GRB.DoubleParam.MIPGap, config.ridesharing.vga.solverMinGap);
			}
			
			LOGGER.info("solving start");
                        
			model.optimize();                                                
                        
			LOGGER.info("solving finished in state {}", model.get(GRB.IntAttr.Status));
			
			LOGGER.info("Objective function value: {}", model.get(GRB.DoubleAttr.ObjVal));
			
			
			// create output from solution
			List<Plan<IOptimalPlanVehicle>> optimalPlans = new ArrayList<>();
			
			for (Map.Entry<GRBVar, Plan<IOptimalPlanVehicle>> entry : variablePlanMap.entrySet()) {
				GRBVar variable = entry.getKey();
				Plan<IOptimalPlanVehicle> plan = entry.getValue();
				
				if(Math.round(variable.get(GRB.DoubleAttr.X)) == 1){
					optimalPlans.add(plan);
				}
			}
			
            int droppedCount = 0;
			// debug dropped demands
			for (Map.Entry<GRBVar, PlanComputationRequest> entry : droppingVarsMap.entrySet()) {
				GRBVar variable = entry.getKey();
				PlanComputationRequest request = entry.getValue();
				if(Math.round(variable.get(GRB.DoubleAttr.X)) == 1){     
                    droppedCount++;
					droppedDemandsAnalyzer.debugFail(request, usedVehiclesPerStation);
					LOGGER.debug("The request was part of {} group plans", requestVariableMap.get(request).size() - 1);
				}
			}
                        
			if(droppedCount > 0 || model.get(GRB.IntAttr.Status) == 3){ //3 = INFEASIBLE state 2 = OPTIMAL
				if(model.get(GRB.IntAttr.Status) == 3){
					LOGGER.error("Failed to solve the passenger-vehicle asssignment, the problem is infeasible!");
				}
				else{
					LOGGER.warn("{} demands were dropped", droppedCount);
				}
				LOGGER.info("Exporting model to: {}", config.ridesharing.vga.modelExportFilePath);
				model.write(config.ridesharing.vga.modelExportFilePath);
			}
			
			// check 1 plan for vehicle
//			checkOnePlanPerVehicle(feasiblePlans, variablePlanMap);
			
			gap = model.get(GRB.DoubleAttr.MIPGap);
			
			model.dispose();
			
			return optimalPlans;
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}

	private void checkOnePlanPerVehicle(List<VehiclePlanList> feasiblePlans, 
			Map<GRBVar, Plan<IOptimalPlanVehicle>> variablePlanMap) {
		Set<VGAVehicle> vGAVehicles = new HashSet<>();
		for (Map.Entry<GRBVar, Plan<IOptimalPlanVehicle>> entry : variablePlanMap.entrySet()) {
			try {
				GRBVar variable = entry.getKey();
				Plan<IOptimalPlanVehicle> plan = entry.getValue();
				if(Math.round(variable.get(GRB.DoubleAttr.X)) == 1 && plan.getVehicle() instanceof VGAVehicle){
					if(vGAVehicles.contains(plan.getVehicle())){
						try {
							throw new SimodException(String.format("More than one plan per vehicle %s", plan.getVehicle()));
						} catch (Exception ex) {
							Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					vGAVehicles.add((VGAVehicle) plan.getVehicle());
				}
			} catch (GRBException ex) {
				Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		for (VehiclePlanList vehiclePlanList : feasiblePlans) {
			if(vehiclePlanList.optimalPlanVehicle instanceof VGAVehicle 
					&& !vGAVehicles.contains(vehiclePlanList.optimalPlanVehicle)){
				try {
						throw new SimodException(String.format("No plan per vehicle %s", vehiclePlanList.optimalPlanVehicle));
					} catch (Exception ex) {
						Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
					}
			}
		}
	}
}
