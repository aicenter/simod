/*
 * Copyright (C) 2021 Czech Technical University in Prague.
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
package cz.cvut.fel.aic.simod.ridesharing.vga.calculations;

import com.google.inject.Inject;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.Plan;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 *
 * @author Fido
 * @param <V>
 */
public class InsertionHeuristicSingleVehicleDARPSolver<V extends IOptimalPlanVehicle> 
		extends SingleVehicleDARPSolver<V>{
	
	private final InsertionHeuristicSolver insertionHeuristicSolver;

	@Inject
	public InsertionHeuristicSingleVehicleDARPSolver(
			StandardPlanCostProvider planCostComputation, 
			SimodConfig config, 
			TravelTimeProvider travelTimeProvider,
			InsertionHeuristicSolver insertionHeuristicSolver
	) {
		super(planCostComputation, config, travelTimeProvider);
		this.insertionHeuristicSolver = insertionHeuristicSolver;
	}
	
	public Plan<V> computeOptimalVehiclePlanForGroup(
			V vehicle, 
			Plan currentPlan,
			PlanComputationRequest request,
			int startTime) {
		insertionHeuristicSolver.resetBestPlan();
		insertionHeuristicSolver.tryToAddRequestToPlan(request, (RideSharingOnDemandVehicle) vehicle.getRealVehicle(), 
				currentPlan.toDriverPlan());
		DriverPlan bestPlan = insertionHeuristicSolver.getBestPlan();
		if(bestPlan == null){
			return null;
		}
		else{
			return driverPlanToPlan(bestPlan, startTime, vehicle);
		}
	}

	@Override
	public Plan<V> computeOptimalVehiclePlanForGroup(
			V vehicle, 
			LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, 
			boolean ignoreTime) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, int startTime, int vehicleCapacity) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private Plan<V> driverPlanToPlan(DriverPlan bestPlan, int startTime, V vehicle) {
		int endTime = startTime + (int) Math.round((float) bestPlan.totalTime / 1000);

		List<PlanRequestAction> bestPlanActions = new ArrayList<>(bestPlan.getLength() - 1);
		for(int i = 1; i < bestPlan.getLength(); i++){
			PlanRequestAction action = (PlanRequestAction) bestPlan.plan.get(i);
			bestPlanActions.add(action);
		}
		
		
		return new Plan(startTime, endTime, (int) Math.round(bestPlan.cost), bestPlanActions, vehicle);
	}
	
}
