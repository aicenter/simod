package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.slf4j.LoggerFactory;



@Singleton
public class VGAGroupGenerator {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VGAGroupGenerator.class);
	
	
	
	
	private final double maximumRelativeDiscomfort;
	
	private final PlanCostComputation planCostComputation;

	@Inject
    public VGAGroupGenerator(AmodsimConfig amodsimConfig, PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
		maximumRelativeDiscomfort = amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort;
	}

    public List<Plan> generateGroupsForVehicle(VGAVehicle vehicle, LinkedHashSet<VGARequest> requests, double startTime) {
		// F_v^{k - 1} - groupes for request adding
		List<GroupData> currentGroups = new ArrayList<>();
		
		// F_v^{1}
        List<VGARequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
        List<Plan> groupPlans = new ArrayList<>();

		
		if(vehicle.getRequestsOnBoard().isEmpty()){
			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
			groupPlans.add(new Plan((int) startTime, vehicle));
		}
		else{
			// BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
			LinkedHashSet<VGARequest> group = vehicle.getRequestsOnBoard();
			
			// actions - only drop off actions are generated for on board vehicles
			List<VGAVehiclePlanAction> actions = new ArrayList<>();
			for(VGARequest request: group){
				actions.add(new VGAVehiclePlanDropoff(request));
			}
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan plan = getOptimalPlanOptimized(vehicle, actions, startTime, true);
			groupPlans.add(plan);
			
			// onboard request composes the single base group
			currentGroups.add(new GroupData(group, actions));
		}
		
		// groups of size 1
		for (VGARequest request : requests) {
			LinkedHashSet<VGARequest> group = new LinkedHashSet<>();
			group.add(request);
			
			// actions
			List<VGAVehiclePlanAction> actions = new ArrayList<>();
			actions.add(new VGAVehiclePlanPickup(request));
			actions.add(new VGAVehiclePlanDropoff(request));

			Plan plan;
			if((plan = getOptimalPlanOptimized(vehicle, actions, startTime, false)) != null) {
				feasibleRequests.add(request);
				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
				if(vehicle.getRequestsOnBoard().isEmpty()){
					currentGroups.add(new GroupData(group, actions));
					groupPlans.add(plan);
				}			
			}
		}
		
		
		// generate other groups
		int currentGroupSize = 1;
        while(!currentGroups.isEmpty()) {

			// current groups for the next iteration
            List<GroupData> newCurrentGroups = new ArrayList<>();
			
			// set of groups that were already checked
			Set<Set<VGARequest>> currentCheckedGroups = new LinkedHashSet<>();

            for (GroupData groupData : currentGroups) {
                for (VGARequest request : feasibleRequests) {
                    if (groupData.requests.contains(request)){
						continue;
					}
					
					// G'
                    LinkedHashSet<VGARequest> newGroupToCheck = new LinkedHashSet<>(groupData.requests);
                    newGroupToCheck.add(request);
					
                    if (currentCheckedGroups.contains(newGroupToCheck)){
						continue;
					}
                    currentCheckedGroups.add(newGroupToCheck);

					// actions
					List<VGAVehiclePlanAction> actions = new ArrayList<>(groupData.actions);
					actions.add(new VGAVehiclePlanPickup(request));
					actions.add(new VGAVehiclePlanDropoff(request));
					
					
                    Plan plan;
                    if((plan = getOptimalPlanOptimized(vehicle, actions, startTime, false)) != null) {
                        newCurrentGroups.add(new GroupData(newGroupToCheck, actions));
                        groupPlans.add(plan);
//                        if(groups.size() > 50){
//                            return groups;
//                        }
                    }
                }
            }

            currentGroups = newCurrentGroups;
			currentGroupSize++;
//			LOGGER.debug("{} groups of the size {} generated", currentGroups.size(), currentGroupSize);
			if(currentGroupSize >= 2){
				break;
			}
        }

//		LOGGER.debug("Groups generated, total number of groups is {}", groups.size());
		
        return groupPlans;
    }

    private VGAVehiclePlan getOptimalPlan(VGAVehicle vehicle, Set<VGARequest> group, boolean ignoreTime){
        Stack<VGAVehiclePlan> toCheck = new Stack<>();
		VGAVehiclePlan emptyPlan = new VGAVehiclePlan(vehicle, group);
        toCheck.push(emptyPlan);

        double upperBound = Double.POSITIVE_INFINITY;
        VGAVehiclePlan bestPlan = null;

		/* In each iteration, we try to add all reamining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
        while(!toCheck.empty()){
			
            VGAVehiclePlan plan = toCheck.pop();
			
			// dropoff actions
            for(VGARequest request : plan.getOnboardRequests()){

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(new VGAVehiclePlanDropoff(request));

                if(request.maxDropOffTime > simplerPlan.getCurrentTime() || ignoreTime) {
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
				for (VGARequest request : plan.getWaitingRequests()) {

					VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);

					// pick up time == demand time
					simplerPlan.add(new VGAVehiclePlanPickup(request));

					if(request.maxPickUpTime > simplerPlan.getCurrentTime()) {
						toCheck.push(simplerPlan);
					}
				}
			}
        }

        return bestPlan;
    }
	
	private Plan getOptimalPlanOptimized(VGAVehicle vehicle, List<VGAVehiclePlanAction> actions, double startTime,
			boolean ignoreTime){
		RideSharingOnDemandVehicle rideSharingVehicle = vehicle.getRidesharingVehicle();
		
		// prepare possible actions
		PlanActionData[] availableActions = new PlanActionData[actions.size()];
		int counter = 0;
		for (VGAVehiclePlanAction action: actions) {
			boolean open = action instanceof VGAVehiclePlanPickup 
					|| vehicle.getRequestsOnBoard().contains(action.getRequest());
			availableActions[counter] = new PlanActionData(action, counter, open);
			counter++;
		}
		
		// plan
		PlanActionData[] plan = new PlanActionData[actions.size()];
		
		// best plan
		VGAVehiclePlanAction[] bestPlan = new VGAVehiclePlanAction[actions.size()];
		double bestPlanCost = Double.MAX_VALUE;
		
		// indexes
		int planPositionIndex = 0;
		int actionIndex = 0;
		
		// global stats
		int onBoardCount = 0;
		
		double endTime = startTime;
		SimulationNode lastPosition = rideSharingVehicle.getPosition();
		double totalDiscomfort = 0;
		
		while(true){
			boolean goDeeper = false;
			boolean infeasibleDueTime = false;
			PlanActionData newActionData = availableActions[actionIndex];
			
			// check if action is not in the plan already
			if(newActionData.open && !newActionData.used){
				VGAVehiclePlanAction newAction = newActionData.action;
			
				/**
				 * Feasibility checks
				 */
				// free capacity check
				if(newAction instanceof VGAVehiclePlanDropoff || onBoardCount < rideSharingVehicle.getCapacity()){

					// max pick up / drop off time check
					double duration = MathUtils.getTravelTimeProvider().getTravelTime(rideSharingVehicle,
							lastPosition, newAction.getPosition()) / 1000.0;		
					if((newAction instanceof VGAVehiclePlanPickup && newAction.getRequest().maxPickUpTime >= endTime + duration)
							|| (newAction instanceof VGAVehiclePlanDropoff && (newAction.getRequest().maxDropOffTime >= endTime + duration || !ignoreTime))){

						// completion check
						if(planPositionIndex == plan.length - 1){
							
							// compute necessary variables as if going deep
							double endTimeTemp = endTime + duration;
							VGARequest request = newAction.getRequest();
							double discomfort = endTimeTemp - request.getOriginTime() - request.minTravelTime;
							
							int totalDuration = (int) (endTimeTemp - startTime);
							double planCost = planCostComputation.calculatePlanCost(totalDiscomfort + discomfort,
									totalDuration);

							if(planCost < bestPlanCost){
								bestPlanCost = planCost;

								// save best plan
								
								// actions from previous steps
								for (int i = 0; i < plan.length - 1; i++) {
									bestPlan[i] = plan[i].action;
								}
								
								// current action
								bestPlan[bestPlan.length - 1] = newAction;
							}
						}
						// go deeper
						else{
							// we add new action to plan
							newActionData.durationFromPreviousAction = duration;
							endTime += duration;
							lastPosition = newAction.getPosition();
							if(newAction instanceof VGAVehiclePlanDropoff){
								VGARequest request = newAction.getRequest();
								double discomfort = endTime - request.getOriginTime() - request.minTravelTime;
								newActionData.discomfort = discomfort;
								totalDiscomfort += discomfort;
							}
							else{
								onBoardCount++;
								availableActions[actionIndex + 1].open = true;
							}
							plan[planPositionIndex] = newActionData;
							newActionData.used = true;
							
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
						actionIndex = lastActionData.actionIndex + 1;
						endTime -= lastActionData.durationFromPreviousAction;
						if(planPositionIndex > 0){
							lastPosition = plan[planPositionIndex - 1].action.getPosition();
						}
						else{
							lastPosition = rideSharingVehicle.getPosition();
						}
						if(lastActionData.action instanceof VGAVehiclePlanDropoff){
							totalDiscomfort -= lastActionData.discomfort;
						}
						else{
							onBoardCount--;
							availableActions[actionIndex].open = false;
						}
						lastActionData.used = false;
						
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
		
		// convert to Plan
		List<VGAVehiclePlanAction> bestPlanActions = new ArrayList<>(bestPlan.length);
		for (int i = 0; i < bestPlan.length; i++) {
			bestPlanActions.add(bestPlan[i]);
			
		}

        return new Plan((int) startTime, (int) endTime, (int) bestPlanCost, bestPlanActions, vehicle);
    }
	
	private class GroupData {
		private final Set<VGARequest> requests;

		private final List<VGAVehiclePlanAction> actions;

		private GroupData(Set<VGARequest> requests, List<VGAVehiclePlanAction> actions) {
			this.requests = requests;
			this.actions = actions;
		}
		
//		private GroupData(GroupData groupData) {
//			this.requests = new LinkedHashSet<>(groupData.requests);
//			this.actions = new ArrayList<>(groupData.actions);
//		}


	}
	
	private class PlanActionData{
		
		private final VGAVehiclePlanAction action;
		
		private final int actionIndex;
		
		private boolean used;
		
		private double durationFromPreviousAction;
		
		private double discomfort;
		
		private boolean open;

		public PlanActionData(VGAVehiclePlanAction action, int actionIndex, boolean open) {
			this.action = action;
			this.actionIndex = actionIndex;
			this.open = open;
			used = false;
			durationFromPreviousAction = 0;
			discomfort = 0;
//			open = action instanceof VGAVehiclePlanPickup;
		}
	}
	
//	private class ActionData{
//		private final VGAVehiclePlanAction action;
//		
//		private boolean used;
//		
//		private Pla
//	}

//private enum Iteration{
//	GO_DEEPER,
//	TRY_NEXT,
//	GO_BACK
//}
}
