package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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

    public List<Plan> generateGroupsForVehicle(V vehicle, LinkedHashSet<PlanComputationRequest> requests, int startTime) {
		// F_v^{k - 1} - groupes for request adding
		Set<GroupData> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
        List<PlanComputationRequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
        List<Plan> groupPlans = new ArrayList<>();

		Set<PlanComputationRequest> onBoardRequestLock = null;
		if(vehicle.getRequestsOnBoard().isEmpty()){
			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
			groupPlans.add(new Plan((int) startTime, vehicle));
		}
		else{
			// BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
			Set<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
			onBoardRequestLock = group;
			
			// actions - only drop off actions are generated for on board vehicles
			List<VGAVehiclePlanAction> actions = new ArrayList<>();
			for(PlanComputationRequest request: group){
				actions.add(new VGAVehiclePlanDropoff(request));
			}
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, true);
			groupPlans.add(plan);
			
//			// onboard request composes the single base group
//			currentGroups.add(new GroupData(group, actions));
	
			for (PlanComputationRequest request : group) {
				Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
				singleRequestGroup.add(request);
				List<VGAVehiclePlanAction> singleAction = new ArrayList<>(1);
				singleAction.add(new VGAVehiclePlanDropoff(request));
				currentGroups.add(new GroupData(singleRequestGroup, singleAction, onBoardRequestLock));
			}
		}
		
		// groups of size 1
		for (PlanComputationRequest request : requests) {
			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
			group.add(request);
			
			// actions
			List<VGAVehiclePlanAction> actions = new ArrayList<>();
			actions.add(new VGAVehiclePlanPickup(request));
			actions.add(new VGAVehiclePlanDropoff(request));

			Plan plan;
			if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, false)) != null) {
				feasibleRequests.add(request);
				currentGroups.add(new GroupData(group, actions, onBoardRequestLock));
				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
				if(vehicle.getRequestsOnBoard().isEmpty()){
					groupPlans.add(plan);
				}			
			}
		}
		
		
		// generate other groups
		int currentGroupSize = 1;
        while(!currentGroups.isEmpty()) {

			// current groups for the next iteration
            Set<GroupData> newCurrentGroups = new LinkedHashSet<>();
			
			// set of groups that were already checked
			Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();

            for (GroupData groupData : currentGroups) {
                for (PlanComputationRequest request : feasibleRequests) {
                    if (groupData.requests.contains(request)){
						continue;
					}
					
					// G'
                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.requests);
                    newGroupToCheck.add(request);
					
                    if (currentCheckedGroups.contains(newGroupToCheck)){
						continue;
					}
                    currentCheckedGroups.add(newGroupToCheck);
					
					// check whether all n-1 subsets are in F_v^{k - 1}
					boolean checkFeasibility = true;
					for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
						if(!currentGroups.contains(new GroupData(subset, null))){
							checkFeasibility = false;
							break;
						}
					}
					
					if(checkFeasibility){

						// actions
						List<VGAVehiclePlanAction> actions = new ArrayList<>(groupData.actions);
						actions.add(new VGAVehiclePlanPickup(request));
						actions.add(new VGAVehiclePlanDropoff(request));

						Plan plan;
						if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, startTime, false)) != null) {
			
							if(groupData.onboardRequestLock == null || newGroupToCheck.containsAll(groupData.onboardRequestLock)){
								newCurrentGroups.add(new GroupData(newGroupToCheck, actions));
								groupPlans.add(plan);
							}
							else{
								newCurrentGroups.add(new GroupData(newGroupToCheck, actions, groupData.onboardRequestLock));
							}
	//                        if(groups.size() > 50){
	//                            return groups;
	//                        }
						}
					}
                }
            }

            currentGroups = newCurrentGroups;
			currentGroupSize++;
//			LOGGER.debug("{} groups of the size {} generated", currentGroups.size(), currentGroupSize);
//			if(currentGroupSize >= 2){
//				break;
//			}
        }

//		LOGGER.debug("Groups generated, total number of groups is {}", groups.size());
		
        return groupPlans;
    }

	
	private class GroupData {
		private final Set<PlanComputationRequest> requests;

		private final List<VGAVehiclePlanAction> actions;
		
		private final Set<PlanComputationRequest> onboardRequestLock;

		private GroupData(Set<PlanComputationRequest> requests, List<VGAVehiclePlanAction> actions) {
			this(requests, actions, null);
		}
		
		private GroupData(Set<PlanComputationRequest> requests, List<VGAVehiclePlanAction> actions,
				Set<PlanComputationRequest> onboardRequestLock) {
			this.requests = requests;
			this.actions = actions;
			this.onboardRequestLock = onboardRequestLock;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 31 * hash + Objects.hashCode(this.requests);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final GroupData other = (GroupData) obj;
			if (!Objects.equals(this.requests, other.requests)) {
				return false;
			}
			return true;
		}
		
		
	}
	
	private List<Set<PlanComputationRequest>> getAllNMinus1Subsets(LinkedHashSet<PlanComputationRequest> set){
		List<Set<PlanComputationRequest>> subsets = new ArrayList<>();
		
		for (PlanComputationRequest planComputationRequest : set) {
			Set<PlanComputationRequest> subset = new HashSet<>(set);
//			Set<PlanComputationRequest> subset = (Set<PlanComputationRequest>) set.clone();
			subset.remove(planComputationRequest);
			subsets.add(subset);
		}
		
		return subsets;
	}
		

}
