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
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.PlanActionData;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.PlanActionDataPickup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F.I.D.O.
 * @param <V>
 */
@Singleton
public class ArrayOptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> 
		extends OptimalVehiclePlanFinder<V>{


	@Inject
	public ArrayOptimalVehiclePlanFinder(StandardPlanCostProvider planCostComputation, AmodsimConfig config,
			TravelTimeProvider travelTimeProvider) {
		super(planCostComputation, config, travelTimeProvider);
	}
	
	
	@Override
	public Plan<V> computeOptimalVehiclePlanForGroup(V vehicle, LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, boolean onboardRequestsOnly){
		
		// prepare possible actions
		List<PlanActionData> availableActionsList = new ArrayList(requests.size() * 2);
		int counter = 0;
//		if(startTime == 360 && vehicle.getId().equals("VGA vehicle 22-567")){
//			int a = 1; // TO REMOVE
//		}
		if(onboardRequestsOnly){
			RideSharingOnDemandVehicle rVehicle = (RideSharingOnDemandVehicle) vehicle.getRealVehicle();
			DriverPlan currentPlan = rVehicle.getCurrentPlan();
			Map<PlanComputationRequest,PlanActionDataPickup> pickupDataMpa = new HashMap<>();
			for(PlanAction action: currentPlan.plan){
				if(action instanceof PlanRequestAction){
					PlanComputationRequest request = ((PlanRequestAction) action).getRequest();
					if(!request.isOnboard()){
						if(action instanceof PlanActionPickup){
							PlanActionDataPickup pickupActionData = new PlanActionDataPickup(request.getPickUpAction(), counter, true);
							availableActionsList.add(pickupActionData);
							counter++;
							pickupDataMpa.put(request, pickupActionData);
						}
						else{
							PlanActionData dropoffActionData = new PlanActionData(request.getDropOffAction(), counter, false);
							availableActionsList.add(dropoffActionData);
							counter++;
							pickupDataMpa.get(request).setDropoffActionData(dropoffActionData);
						}
					}
					else{
						availableActionsList.add(new PlanActionData((PlanRequestAction) action, counter, true));
						counter++;
					}
				}
			}
		}
		else{
			for (PlanComputationRequest request: requests) {
				if(!request.isOnboard()){
					PlanActionDataPickup pickupActionData = new PlanActionDataPickup(request.getPickUpAction(), counter, true);
					availableActionsList.add(pickupActionData);
					counter++;
					PlanActionData dropoffActionData = new PlanActionData(request.getDropOffAction(), counter, false);
					availableActionsList.add(dropoffActionData);
					counter++;
					pickupActionData.setDropoffActionData(dropoffActionData);
				}
				else{
					availableActionsList.add(new PlanActionData(request.getDropOffAction(), counter, true));
					counter++;
				}
			}
		}
		
		PlanActionData[] availableActions = availableActionsList.toArray(new PlanActionData[0]);
		
		// plan
		PlanActionData[] plan = new PlanActionData[availableActions.length];
		
		// best plan
		PlanRequestAction[] bestPlan = null;
		int bestPlanCost = Integer.MAX_VALUE;
		long bestPlanEndTime = 0;
		
		// indexes
		int planPositionIndex = 0;
		int actionIndex = 0;
		
		// global stats
		int onBoardCount = vehicle.getRequestsOnBoard().size();
		long endTime = startTime * 1000;
		SimulationNode lastPosition = vehicle.getPosition();
		int totalDiscomfort = 0;
		
		while(true){
			boolean goDeeper = false;
			boolean infeasibleDueTime = false;
			PlanActionData newActionData = availableActions[actionIndex];
			
			// check if action is not in the plan already
			if(newActionData.isOpen() && !newActionData.isUsed()){
				PlanRequestAction newAction = newActionData.getAction();
			
				/**
				 * Feasibility checks
				 */
				// free capacity check
				if(newAction instanceof PlanActionDropoff || onBoardCount < vehicle.getCapacity()){

					// max pick up / drop off time check
					int durationMs;
					if(planPositionIndex == 0){
						durationMs = (int) (travelTimeProvider.getTravelTime(
								vehicle.getRealVehicle(), newAction.getPosition()));
					}
					else{
						durationMs = (int) (travelTimeProvider.getExpectedTravelTime(
								lastPosition, newAction.getPosition()));
					}
					
					// actions feasibility check
					boolean allActionsFeasible = true;
					int roundingExtraTime = 1;
					for (int i = 0; i < availableActions.length; i++) {
						PlanActionData actionData = availableActions[i];
						PlanRequestAction action = actionData.getAction();
						int durationS = (int) Math.round((float) durationMs / 1000);
						
						if(!actionData.isUsed()){
							int endTimeS = (int) Math.round((float) endTime / 1000);
							if((action instanceof PlanActionPickup 
								&& action.getRequest().getMaxPickupTime() < endTimeS + durationS
								&& (!onboardRequestsOnly 
										|| action.getRequest().getMaxPickupTime() + roundingExtraTime < endTimeS + durationS))
							|| (action instanceof PlanActionDropoff 
								&& (action.getRequest().getMaxDropoffTime() < endTimeS + durationS)
								&& (!onboardRequestsOnly 
										|| action.getRequest().getMaxDropoffTime() + roundingExtraTime < endTimeS + durationS)
									)){
								allActionsFeasible = false;
								if(onboardRequestsOnly){
									int a = 1;
								}
								break;
							}
						}
					}
					
					if(allActionsFeasible){
						// completion check
						if(planPositionIndex == plan.length - 1){
							
							// compute necessary variables as if going deep
							int endTimeTemp = (int) Math.round((float) (endTime + durationMs) / 1000);
							PlanComputationRequest request = newAction.getRequest();
							int discomfort = endTimeTemp - request.getOriginTime() - request.getMinTravelTime();
							
							//TODO add onboard vehicles previous discomfort
							
							int totalDuration = endTimeTemp - startTime;
							int planCost = planCostComputation.calculatePlanCost(totalDiscomfort + discomfort,
									totalDuration);

							if(planCost < bestPlanCost){
								bestPlanCost = planCost;
								bestPlanEndTime = endTime + durationMs;

								// save best plan
								if(bestPlan == null){
									bestPlan = new PlanRequestAction[availableActions.length];
								}
								
								// actions from previous steps
								for (int i = 0; i < plan.length - 1; i++) {
									bestPlan[i] = plan[i].getAction();
								}
								
								// current action
								bestPlan[bestPlan.length - 1] = newAction;
								
								if(onboardRequestsOnly){
									break;
								}
							}
						}
						// go deeper
						else{
							// we add new action to plan
							newActionData.setDurationFromPreviousAction(durationMs);
							endTime += durationMs;
							lastPosition = newAction.getPosition();
							if(newAction instanceof PlanActionDropoff){
								PlanComputationRequest request = newAction.getRequest();
								int endTimeS = (int) Math.round((float) endTime / 1000);
								int discomfort = endTimeS - request.getOriginTime() - request.getMinTravelTime();
								newActionData.setDiscomfort(discomfort);
								totalDiscomfort += discomfort;
								onBoardCount--;
							}
							else{
								onBoardCount++;
								((PlanActionDataPickup) newActionData).openDropOff(true);
							}
							plan[planPositionIndex] = newActionData;
							newActionData.setUsed(true);
							
							planPositionIndex++;
							if(onboardRequestsOnly){
								actionIndex += 1;
							}
							else{
								actionIndex = 0;
							}
							goDeeper = true;
						}	
					}
					else{
						infeasibleDueTime = true;
					}
				}
			}
			
			// current don't go deeper (we wont continue to next action)
			if(!goDeeper){
				
				if(onboardRequestsOnly){
					try {
						throw new Exception(String.format("Vehicle %s: Last plan is not feasible!", vehicle));
					} catch (Exception ex) {
						Logger.getLogger(ArrayOptimalVehiclePlanFinder.class.getName()).log(Level.SEVERE, null, ex);
						return null;
					}
				}
				
				// last action - we have to go back
				if(actionIndex == availableActions.length - 1){
					int boundIndex = availableActions.length - 1;
					while((actionIndex >= boundIndex || infeasibleDueTime) && planPositionIndex > 0){
						// we remove the last action from plan
						PlanActionData lastActionData = plan[--planPositionIndex];
						actionIndex = lastActionData.getActionIndex() + 1;
						endTime -= lastActionData.getDurationFromPreviousAction();
						if(planPositionIndex > 0){
							lastPosition = plan[planPositionIndex - 1].getAction().getPosition();
						}
						else{
							lastPosition = vehicle.getPosition();
						}
						if(lastActionData.getAction() instanceof PlanActionDropoff){
							totalDiscomfort -= lastActionData.getDiscomfort();
							onBoardCount++;
						}
						else{
							onBoardCount--;
							((PlanActionDataPickup) lastActionData).openDropOff(false);
						}
						lastActionData.setUsed(false);
						
						infeasibleDueTime = false;
						boundIndex = availableActions.length;
					}
					
					// end condition - nothing left to search for
					if((actionIndex == boundIndex || infeasibleDueTime) && planPositionIndex == 0){
						break;
					}
				}
				
				// try next action
				else{
					actionIndex++;
				}
			}
		}
		
		if(bestPlan == null){
			if(onboardRequestsOnly){
				try {
					throw new Exception(String.format("Previous plan is not feasible for vehicle %s",
							vehicle));
				} catch (Exception ex) {
					Logger.getLogger(ArrayOptimalVehiclePlanFinder.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			return null;
		}
		
		// convert to Plan
		List<PlanRequestAction> bestPlanActions = new ArrayList<>(bestPlan.length);
		for (int i = 0; i < bestPlan.length; i++) {
			bestPlanActions.add(bestPlan[i]);
			
		}
		int endTimeS = (int) Math.round((float) bestPlanEndTime / 1000);
		return new Plan((int) startTime, endTimeS, (int) bestPlanCost, bestPlanActions, vehicle);
	}
	
	@Override
	public boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, int vehicleCapacity){
		
		// prepare possible actions
		PlanActionData[] availableActions = new PlanActionData[requests.size() * 2];
		int counter = 0;
		for (PlanComputationRequest request: requests) {
			availableActions[counter] = new PlanActionData(request.getPickUpAction(), counter, true);
			counter++;
			availableActions[counter] = new PlanActionData(request.getDropOffAction(), counter, false);
			counter++;
		}
		
		// plan
		PlanActionData[] plan = new PlanActionData[availableActions.length];
		
		// indexes
		int planPositionIndex = 0;
		int actionIndex = 0;
		
		// global stats
		int onBoardCount = 0;
		int endTime = startTime;
		SimulationNode lastPosition = null;
		
		while(true){
			boolean goDeeper = false;
			boolean infeasibleDueTime = false;
			PlanActionData newActionData = availableActions[actionIndex];
			
			// check if action is not in the plan already
			if(newActionData.isOpen() && !newActionData.isUsed()){
				PlanRequestAction newAction = newActionData.getAction();
			
				/**
				 * Feasibility checks
				 */
				// free capacity check
				if(newAction instanceof PlanActionDropoff || onBoardCount < vehicleCapacity){

					// max pick up / drop off time check
					int duration;
					if(planPositionIndex > 0){
						duration = (int) (travelTimeProvider.getExpectedTravelTime(
								lastPosition, newAction.getPosition()) / 1000.0);
					}
					else{
						duration = 0;
					}

					boolean allActionsFeasible = true;
					for (int i = 0; i < availableActions.length; i++) {
						PlanActionData actionData = availableActions[i];
						if(!actionData.isUsed()){
							if((newAction instanceof PlanActionPickup 
								&& newAction.getRequest().getMaxPickupTime() < endTime + duration)
							|| (newAction instanceof PlanActionDropoff 
								&& (newAction.getRequest().getMaxDropoffTime() < endTime + duration))){
								allActionsFeasible = false;
								break;
							}
						}
					}
					
					if(allActionsFeasible){
						// completion check
						if(planPositionIndex == plan.length - 1){
							return true;
						}
						// go deeper
						else{
							// we add new action to plan
							newActionData.setDurationFromPreviousAction(duration);
							endTime += duration;
							lastPosition = newAction.getPosition();
							if(newAction instanceof PlanActionDropoff){
								PlanComputationRequest request = newAction.getRequest();
								int discomfort = endTime - request.getOriginTime() - request.getMinTravelTime();
								newActionData.setDiscomfort(discomfort);
							}
							else{
								onBoardCount++;
								((PlanActionDataPickup) newActionData).openDropOff(true);
							}
							plan[planPositionIndex] = newActionData;
							newActionData.setUsed(true);
							
							planPositionIndex++;
							actionIndex = 0;
							goDeeper = true;
						}	
					}
					else{
						infeasibleDueTime = true;
					}
				}
			}
			
			// current don't go deeper (we wont continue to next action)
			if(!goDeeper){
				
				// last action - we have to go back
				if(actionIndex == availableActions.length - 1 || infeasibleDueTime){
					int boundIndex = availableActions.length - 1;
					while((actionIndex >= boundIndex || infeasibleDueTime) && planPositionIndex > 0){
						// we remove the last action from plan
						PlanActionData lastActionData = plan[--planPositionIndex];
						actionIndex = lastActionData.getActionIndex() + 1;
						endTime -= lastActionData.getDurationFromPreviousAction();
						if(planPositionIndex > 0){
							lastPosition = plan[planPositionIndex - 1].getAction().getPosition();
						}
						else{
							lastPosition = null;
						}
						if(lastActionData.getAction() instanceof PlanActionPickup){
							onBoardCount--;
							((PlanActionDataPickup) lastActionData).openDropOff(false);
						}
						lastActionData.setUsed(false);
						
						infeasibleDueTime = false;
						boundIndex = availableActions.length;
					}
					
					// end condition - nothing left to search for
					if((actionIndex == boundIndex || infeasibleDueTime) && planPositionIndex == 0){
						break;
					}
				}
				// try next action
				else{
					actionIndex++;
				}
			}
		}

		return false;
	}
}
