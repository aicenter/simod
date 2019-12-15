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
package cz.cvut.fel.aic.amodsim.ridesharing.offline.vga;

//import gurobi.*;

import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.*;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

	private GRBEnv env;
	
	int iteration;
	
	double gap;

	
	
	
	public double getGap() {
		return gap;
	}
	
	
	
	@Inject
	public OfflineGurobiSolver(StandardPlanCostProvider planCostComputation, AmodsimConfig config) {
		this.planCostComputation = planCostComputation;

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
	
    
	public List<int[]> assignOptimallyFeasiblePlans(List<int[]> plans) {
		
        LOGGER.debug(plans.size() +" plans ");
		int maxCar = plans.size();
        // sort by cost?
        //		Collections.sort(feasiblePlans);  
        
		try {
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
  		    // map for request constraint generation
			Map<Integer, List<GRBVar>> requestVariableMap = new LinkedHashMap<>();
            
            // map for solution processing
			Map<GRBVar, int[]> variablePlanMap = new LinkedHashMap<>();
            
            GRBLinExpr vehicleConstraint = new GRBLinExpr();
            
            
            //for (int i = 0; i < plans.size(); i++) {
            int planCounter = 0;
			while(!plans.isEmpty()){
            //    int[] plan = plans.get(i);
                int[] plan = plans.remove(0);
                int planCost = plan[0];	
             //  LOGGER.debug(Arrays.toString(plan) + ", Plan cost "+planCost);
                
				/* plan variables */
                String newVarName = String.format("%s", planCounter);
 
				GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName );	
               // if( i%100000 == 0) LOGGER.debug("Grb vars ome " + i);
            
                variablePlanMap.put(newVar, plan);
                /*  objective */
                objetive.addTerm(planCost, newVar);		
				/* constraint 1 - exactly one plan per vehicle??? */
				vehicleConstraint.addTerm(1.0, newVar);
				/* Adding constraint 1 into model */
	
                // constraint 2 - exactly one plan for each request
               
                //remove cost and dropoffs
                plan = Arrays.copyOfRange(plan, 1, plan.length/2 + 1);
              // LOGGER.debug("requests " +Arrays.toString(plan));
                
                for (int request: plan) {
                    CollectionUtil.addToListInMap(requestVariableMap, request, newVar);
                }
            }//plans
            
            String vehicleConstraintName = "vehicle";
			model.addConstr(vehicleConstraint, GRB.LESS_EQUAL, maxCar, vehicleConstraintName);
               
            for (Map.Entry<Integer, List<GRBVar>> entry : requestVariableMap.entrySet()) {
                Integer request = entry.getKey();
                List<GRBVar> planVariableList = entry.getValue();
                GRBLinExpr requestConstraint = new GRBLinExpr();
                for (GRBVar planVariable : planVariableList) {
                        requestConstraint.addTerm(1.0, planVariable);
                    }
                String requestConstraintName = String.format("p%s", request);
                //LOGGER.debug("1 plan per request constraint");
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
			

			List<int[]> optimalPlans = new ArrayList<>();
			
			for (Map.Entry<GRBVar, int[]> entry : variablePlanMap.entrySet()) {
				GRBVar variable = entry.getKey();
				int[] plan = entry.getValue();
			
				if(Math.round(variable.get(GRB.DoubleAttr.X)) == 1){
                    //LOGGER.info("optimal plan " + Arrays.toString(plan));
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
