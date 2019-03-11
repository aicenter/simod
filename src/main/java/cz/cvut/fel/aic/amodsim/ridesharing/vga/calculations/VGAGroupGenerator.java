package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
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
	
	private final int vehicleCapacityForGlobalGroupGeneration;
	

	@Inject
    public VGAGroupGenerator(OptimalVehiclePlanFinder optimalVehiclePlanFinder, AmodsimConfig config) {
		this.optimalVehiclePlanFinder = optimalVehiclePlanFinder;
		vehicleCapacityForGlobalGroupGeneration = config.ridesharing.vehicleCapacity;
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
			LinkedHashSet<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
			onBoardRequestLock = group;
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
			groupPlans.add(plan);
	
			for (PlanComputationRequest request : group) {
				Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
				singleRequestGroup.add(request);
				currentGroups.add(new GroupData(singleRequestGroup, onBoardRequestLock));
			}
		}
		
		// groups of size 1
		for (PlanComputationRequest request : requests) {
			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
			group.add(request);

			Plan plan;
			if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, group, startTime, false)) != null) {
				feasibleRequests.add(request);
				currentGroups.add(new GroupData(group, onBoardRequestLock));
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

						Plan plan;
						if((plan = optimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, newGroupToCheck, startTime, false)) != null) {
			
							if(groupData.onboardRequestLock == null || newGroupToCheck.containsAll(groupData.onboardRequestLock)){
								newCurrentGroups.add(new GroupData(newGroupToCheck));
								groupPlans.add(plan);
							}
							else{
								newCurrentGroups.add(new GroupData(newGroupToCheck, groupData.onboardRequestLock));
							}
//	                        if(groups.size() > 50){
//	                            return groups;
//	                        }
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
	
	public Set<Set<PlanComputationRequest>> generateGlobalGroups(LinkedHashSet<PlanComputationRequest> requests, int startTime) {
		// F_v^{k - 1} - groupes for request adding
		Set<Set<PlanComputationRequest>> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
        List<PlanComputationRequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
        Set<Set<PlanComputationRequest>> feasibleGroups = new HashSet<>();
		
		// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
		feasibleGroups.add(new LinkedHashSet<>());
		
		// groups of size 1
		for (PlanComputationRequest request : requests) {
			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
			group.add(request);

			if(optimalVehiclePlanFinder.groupFeasible(group, startTime, vehicleCapacityForGlobalGroupGeneration)) {
				feasibleRequests.add(request);
				currentGroups.add(group);
				feasibleGroups.add(group);		
			}
		}
		
		
		// generate other groups
		int currentGroupSize = 1;
        while(!currentGroups.isEmpty()) {

			// current groups for the next iteration
            Set<Set<PlanComputationRequest>> newCurrentGroups = new LinkedHashSet<>();
			
			// set of groups that were already checked
			Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();

            for (Set<PlanComputationRequest> group : currentGroups) {
                for (PlanComputationRequest request : feasibleRequests) {
                    if (group.contains(request)){
						continue;
					}
					
					// G'
                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(group);
                    newGroupToCheck.add(request);
					
                    if (currentCheckedGroups.contains(newGroupToCheck)){
						continue;
					}
                    currentCheckedGroups.add(newGroupToCheck);
					
					// check whether all n-1 subsets are in F_v^{k - 1}
					boolean checkFeasibility = true;
					for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
						if(!currentGroups.contains(subset)){
							checkFeasibility = false;
							break;
						}
					}
					
					if(checkFeasibility){

						if(optimalVehiclePlanFinder.groupFeasible(newGroupToCheck, startTime, vehicleCapacityForGlobalGroupGeneration)) {
							newCurrentGroups.add(newGroupToCheck);
							feasibleGroups.add(newGroupToCheck);

//	                        if(groups.size() > 50){
//	                            return groups;
//	                        }
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
		
        return feasibleGroups;
    }

	
	private class GroupData {
		private final Set<PlanComputationRequest> requests;
		
		private final Set<PlanComputationRequest> onboardRequestLock;

		private GroupData(Set<PlanComputationRequest> requests) {
			this(requests, null);
		}
		
		private GroupData(Set<PlanComputationRequest> requests, 
				Set<PlanComputationRequest> onboardRequestLock) {
			this.requests = requests;
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
