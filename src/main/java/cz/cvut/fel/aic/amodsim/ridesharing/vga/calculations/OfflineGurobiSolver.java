/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

//import gurobi.*;

import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.OfflineVirtualVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VehiclePlanList;
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
public class OfflineGurobiSolver {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineGurobiSolver.class);
	
	
	private final StandardPlanCostProvider planCostComputation;
	
	private final int timeLimit;

	private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;
	
	private GRBEnv env;
	
	int iteration;
	
	double gap;

	
	
	
	public double getGap() {
		return gap;
	}
	
	
	
	@Inject
	public OfflineGurobiSolver(StandardPlanCostProvider planCostComputation, AmodsimConfig config, 
			DroppedDemandsAnalyzer droppedDemandsAnalyzer) {
		this.planCostComputation = planCostComputation;
		this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
		iteration = 1;
		timeLimit = config.ridesharing.vga.solverTimeLimit;
		
		// env init
		env = null;           
		try {            
			env = new GRBEnv(config.amodsimExperimentDir +"/log/mip.log");
		} catch (GRBException ex) {
			Logger.getLogger(OfflineGurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}
	
	public List<Plan<IOptimalPlanVehicle>> assignOptimallyFeasiblePlans(
			List<VehiclePlanList> feasiblePlans, LinkedHashSet<PlanComputationRequest> requests, 
			int[] usedVehiclesPerStation) {
		
        LOGGER.debug("Feasible plans " + feasiblePlans.size());
		Collections.sort(feasiblePlans);
		
		try {
			// solver init
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
			// map for solution processing
			Map<GRBVar,Plan<IOptimalPlanVehicle>> variablePlanMap = new LinkedHashMap<>();
			
			// map for request constraint generation
			Map<PlanComputationRequest,List<GRBVar>> requestVariableMap = new LinkedHashMap<>();
			
			for (VehiclePlanList vehicleEntry : feasiblePlans) {
 				GRBLinExpr vehicleConstraint = new GRBLinExpr();
				String vehicleId = vehicleEntry.optimalPlanVehicle.getId();
				
				int groupCounter = 0;

				for (Plan<IOptimalPlanVehicle> plan : vehicleEntry.feasibleGroupPlans) {
					
					// variables
					String newVarName = String.format("%s %s", vehicleId, groupCounter);
					GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
					variablePlanMap.put(newVar, plan);
					
					// objective
					objetive.addTerm(plan.getCost(), newVar);
					
					// constraint 1 - exactly one plan per vehicle
					vehicleConstraint.addTerm(1.0, newVar);
					
					// filling map for constraint 2 
					for (PlanRequestAction action: plan.getActions()) {
					// add variable once to each request list
       					if(action instanceof PlanActionDropoff){
							CollectionUtil.addToListInMap(requestVariableMap, 
                                (DefaultPlanComputationRequest) action.getRequest(), newVar);
						}
					}
  					groupCounter++;
				}//plan
                //Plans are already in the map
                vehicleEntry.feasibleGroupPlans.clear();
                
				String vehicleConstraintName = String.format("v%s", vehicleId);
				
				/* Adding constraint 1 into model */
				
				// normal vehicles 
				if(vehicleEntry.optimalPlanVehicle instanceof VGAVehicle){
					model.addConstr(vehicleConstraint, GRB.EQUAL, 1.0, vehicleConstraintName);

				}
				// virtual vehicles
				else{
					int limit = ((OfflineVirtualVehicle) vehicleEntry.optimalPlanVehicle).getCarLimit();
					model.addConstr(vehicleConstraint, GRB.LESS_EQUAL, limit, vehicleConstraintName);
  				}
	
			}//vehicle
							
			// constraint 2 - exactly one plan for each request
			for (Map.Entry<PlanComputationRequest, List<GRBVar>> entry : requestVariableMap.entrySet()) {
				PlanComputationRequest request = entry.getKey();
				List<GRBVar> planVariableList = entry.getValue();
				
				GRBLinExpr requestConstraint = new GRBLinExpr();
				
                planVariableList.forEach((planVariable) -> {
                    requestConstraint.addTerm(1.0, planVariable);
                });
				
				String requestConstraintName = String.format("req %s", request.getId());
				model.addConstr(requestConstraint, GRB.EQUAL, 1.0, requestConstraintName);
			}
			
			
			model.setObjective(objetive, GRB.MINIMIZE);
			
			/* MODEL CONFIG AND RUN */
			
//			// solution can be 1% worse than the optimal solution
//			model.set(GRB.DoubleParam.MIPGap, 0.01);
		
			// time limit
			model.set(GRB.DoubleParam.TimeLimit, timeLimit);
			
			LOGGER.info("solving start");
			model.optimize();
			LOGGER.info("solving finished");
			
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

			gap = model.get(GRB.DoubleAttr.MIPGap);
		
			model.dispose();
			
			return optimalPlans;
		} catch (GRBException ex) {
			Logger.getLogger(OfflineGurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}
	
}
