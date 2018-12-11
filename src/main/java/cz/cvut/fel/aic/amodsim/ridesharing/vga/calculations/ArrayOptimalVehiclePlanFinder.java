/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.PlanActionData;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanPickup;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 * @param <V>
 */
@Singleton
public class ArrayOptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> extends OptimalVehiclePlanFinder<V>{

	@Inject
	public ArrayOptimalVehiclePlanFinder(PlanCostComputation planCostComputation) {
		super(planCostComputation);
	}
	
	
	@Override
	public Plan<V> getOptimalVehiclePlanForGroup(V vehicle, List<VGAVehiclePlanAction> actions, 
			int startTime, boolean ignoreTime){
		
		// prepare possible actions
		PlanActionData[] availableActions = new PlanActionData[actions.size()];
		int counter = 0;
		for (VGAVehiclePlanAction action: actions) {
			boolean open = action instanceof VGAVehiclePlanPickup 
					|| action.getRequest().isOnboard();
			availableActions[counter] = new PlanActionData(action, counter, open);
			counter++;
		}
		
		// plan
		PlanActionData[] plan = new PlanActionData[actions.size()];
		
		// best plan
		VGAVehiclePlanAction[] bestPlan = null;
		int bestPlanCost = Integer.MAX_VALUE;
		
		// indexes
		int planPositionIndex = 0;
		int actionIndex = 0;
		
		// global stats
		int onBoardCount = vehicle.getRequestsOnBoard().size();
		
		int endTime = startTime;
		SimulationNode lastPosition = vehicle.getPosition();
		int totalDiscomfort = 0;
		
		while(true){
			boolean goDeeper = false;
			boolean infeasibleDueTime = false;
			PlanActionData newActionData = availableActions[actionIndex];
			
			// check if action is not in the plan already
			if(newActionData.isOpen() && !newActionData.isUsed()){
				VGAVehiclePlanAction newAction = newActionData.getAction();
			
				/**
				 * Feasibility checks
				 */
				// free capacity check
				if(newAction instanceof VGAVehiclePlanDropoff || onBoardCount < vehicle.getCapacity()){

					// max pick up / drop off time check
					int duration = (int) (MathUtils.getTravelTimeProvider().getExpectedTravelTime(
							lastPosition, newAction.getPosition()) / 1000.0);		
//					if((newAction instanceof VGAVehiclePlanPickup 
//							&& newAction.getRequest().getMaxPickupTime() >= endTime + duration)
//							|| (newAction instanceof VGAVehiclePlanDropoff 
//							&& (newAction.getRequest().getMaxDropoffTime() >= endTime + duration || ignoreTime))){
						
//					 actions feasibility check
					boolean allActionsFeasible = true;
					for (int i = 0; i < availableActions.length; i++) {
						PlanActionData actionData = availableActions[i];
						if(!actionData.isUsed()){
							if((newAction instanceof VGAVehiclePlanPickup 
								&& newAction.getRequest().getMaxPickupTime() < endTime + duration)
							|| (newAction instanceof VGAVehiclePlanDropoff 
								&& (newAction.getRequest().getMaxDropoffTime() < endTime + duration && !ignoreTime))){
								allActionsFeasible = false;
								break;
							}
						}
					}
					
					if(allActionsFeasible){
						// completion check
						if(planPositionIndex == plan.length - 1){
							
							// compute necessary variables as if going deep
							int endTimeTemp = endTime + duration;
							PlanComputationRequest request = newAction.getRequest();
							int discomfort = endTimeTemp - request.getOriginTime() - request.getMinTravelTime();
							
							//TODO add onboard vehicles previous discomfort
							
							int totalDuration = (int) (endTimeTemp - startTime);
							int planCost = planCostComputation.calculatePlanCost(totalDiscomfort + discomfort,
									totalDuration);

							if(planCost < bestPlanCost){
								bestPlanCost = planCost;

								// save best plan
								if(bestPlan == null){
									bestPlan = new VGAVehiclePlanAction[actions.size()];
								}
								
								// actions from previous steps
								for (int i = 0; i < plan.length - 1; i++) {
									bestPlan[i] = plan[i].getAction();
								}
								
								// current action
								bestPlan[bestPlan.length - 1] = newAction;
							}
						}
						// go deeper
						else{
							// we add new action to plan
							newActionData.setDurationFromPreviousAction(duration);
							endTime += duration;
							lastPosition = newAction.getPosition();
							if(newAction instanceof VGAVehiclePlanDropoff){
								PlanComputationRequest request = newAction.getRequest();
								int discomfort = endTime - request.getOriginTime() - request.getMinTravelTime();
								newActionData.setDiscomfort(discomfort);
								totalDiscomfort += discomfort;
							}
							else{
								onBoardCount++;
								availableActions[actionIndex + 1].setOpen(true);
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
							lastPosition = vehicle.getPosition();
						}
						if(lastActionData.getAction() instanceof VGAVehiclePlanDropoff){
							totalDiscomfort -= lastActionData.getDiscomfort();
						}
						else{
							onBoardCount--;
							availableActions[actionIndex].setOpen(false);
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
			return null;
		}
		
		// convert to Plan
		List<VGAVehiclePlanAction> bestPlanActions = new ArrayList<>(bestPlan.length);
		for (int i = 0; i < bestPlan.length; i++) {
			bestPlanActions.add(bestPlan[i]);
			
		}

        return new Plan((int) startTime, (int) endTime, (int) bestPlanCost, bestPlanActions, vehicle);
    }
}
