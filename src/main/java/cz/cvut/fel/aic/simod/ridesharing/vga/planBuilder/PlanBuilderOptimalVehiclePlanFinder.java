/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod.ridesharing.vga.planBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.SingleVehicleDARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.Plan;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author F.I.D.O.
 * @param <V>
 */
@Singleton
public class PlanBuilderOptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> extends SingleVehicleDARPSolver<V>{

	@Inject
	public PlanBuilderOptimalVehiclePlanFinder(StandardPlanCostProvider planCostComputation, SimodConfig config,
			TravelTimeProvider travelTimeProvider) {
		super(planCostComputation, config, travelTimeProvider);
	}

	
	
	@Override
	public Plan<V> computeOptimalVehiclePlanForGroup(V vehicle, LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, boolean ignoreTime) {
		Stack<VGAVehiclePlan> toCheck = new Stack<>();
		
		// group reconstruction
		Set<PlanComputationRequest> group = new LinkedHashSet<>();
		for (PlanComputationRequest request : requests) {
			group.add(request);
		}
		
		VGAVehiclePlan emptyPlan = new VGAVehiclePlan(vehicle, group, startTime, travelTimeProvider);
		toCheck.push(emptyPlan);

		double upperBound = Double.POSITIVE_INFINITY;
		VGAVehiclePlan bestPlan = null;

		/* In each iteration, we try to add all reamining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
		while(!toCheck.empty()){
			
			VGAVehiclePlan plan = toCheck.pop();
			
			// dropoff actions
			for(PlanComputationRequest request : plan.getOnboardRequests()){

				VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
				simplerPlan.add(request.getDropOffAction());

				if(request.getMaxDropoffTime() > simplerPlan.getCurrentTime() || ignoreTime) {
					double currentCost = planCostComputation.calculatePlanCost(simplerPlan);
					if (currentCost < upperBound) {
						if (simplerPlan.getWaitingRequests().isEmpty() && simplerPlan.getOnboardRequests().isEmpty()) {
							upperBound = currentCost;
							bestPlan = simplerPlan;
						} else {
							toCheck.push(simplerPlan);
						}
					}
				}
			}

			// pickup actions
			if(plan.vehicleHasFreeCapacity()){
				for (PlanComputationRequest request : plan.getWaitingRequests()) {

					VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);

					// pick up time == demand time
					simplerPlan.add(request.getPickUpAction());

					if(request.getMaxPickupTime() > simplerPlan.getCurrentTime()) {
						toCheck.push(simplerPlan);
					}
				}
			}
		}
		
		if(bestPlan == null){
			return null;
		}
		
		// convert to Plan
		List<PlanRequestAction> bestPlanActions = bestPlan.getActions();
		
		return new Plan((int) startTime, (int) bestPlan.getCurrentTime(), (int) upperBound, bestPlanActions, vehicle);
	}

	@Override
	public boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, int startTime, int vehicleCapacity) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
