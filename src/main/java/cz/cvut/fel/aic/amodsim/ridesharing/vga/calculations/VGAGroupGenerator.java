package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.slf4j.LoggerFactory;



@Singleton
public class VGAGroupGenerator<V extends IOptimalPlanVehicle> {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VGAGroupGenerator.class);

	
	private final OptimalVehiclePlanFinder optimalVehiclePlanFinder;
	

	@Inject
    public VGAGroupGenerator(OptimalVehiclePlanFinder optimalVehiclePlanFinder) {
		this.optimalVehiclePlanFinder = optimalVehiclePlanFinder;
	}

    public List<Plan> generateGroupsForVehicle(V vehicle, LinkedHashSet<VGARequest> requests, double startTime) {
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
			Set<VGARequest> group = vehicle.getRequestsOnBoard();
			
			// actions - only drop off actions are generated for on board vehicles
			List<VGAVehiclePlanAction> actions = new ArrayList<>();
			for(VGARequest request: group){
				actions.add(new VGAVehiclePlanDropoff(request));
			}
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, true);
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
			if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, false)) != null) {
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
                    if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, false)) != null) {
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

    private VGAVehiclePlan getOptimalPlan(V vehicle, Set<VGARequest> group, boolean ignoreTime){
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
                    double currentCost = optimalVehiclePlanFinder.planCostComputation.calculatePlanCost(simplerPlan);
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
	

	
	private class GroupData {
		private final Set<VGARequest> requests;

		private final List<VGAVehiclePlanAction> actions;

		private GroupData(Set<VGARequest> requests, List<VGAVehiclePlanAction> actions) {
			this.requests = requests;
			this.actions = actions;
		}
	}
	
	

}
