package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.LoggerFactory;



@Singleton
public class GroupGenerator<V extends IOptimalPlanVehicle> {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);
	
	private static final int GROUP_STATS_SIZE_BUFFER = 0;
	
	private static final int MILLION = 1000000;

	
	private final OptimalVehiclePlanFinder optimalVehiclePlanFinder;
	
	private final int vehicleCapacity;
	
	private final boolean recordTime;
	
	
	private int[] groupCounts;
	
	private int[] groupCountsPlanExists;
	
	private int[] computationalTimes;
	
	private int[] computationalTimesPlanExists;

	public int[] getGroupCounts() {
		return groupCounts;
	}

	public int[] getGroupCountsPlanExists() {
		return groupCountsPlanExists;
	}

	public int[] getComputationalTimes() {
		return computationalTimes;
	}

	public int[] getComputationalTimesPlanExists() {
		return computationalTimesPlanExists;
	}
	
	
	

	@Inject
    public GroupGenerator(OptimalVehiclePlanFinder optimalVehiclePlanFinder, AmodsimConfig config) {
		this.optimalVehiclePlanFinder = optimalVehiclePlanFinder;
		vehicleCapacity = config.ridesharing.vehicleCapacity;
		recordTime = config.ridesharing.vga.logPlanComputationalTime;
	}

    public List<Plan> generateGroupsForVehicle(V vehicle, LinkedHashSet<PlanComputationRequest> requests, int startTime) {
		
		// statistics
		if(recordTime){
			groupCounts = new int[vehicleCapacity + GROUP_STATS_SIZE_BUFFER];
			groupCountsPlanExists = new int[vehicleCapacity + GROUP_STATS_SIZE_BUFFER];
			computationalTimes = new int[vehicleCapacity + GROUP_STATS_SIZE_BUFFER];
			computationalTimesPlanExists = new int[vehicleCapacity + GROUP_STATS_SIZE_BUFFER];
		}
		
		// F_v^{k - 1} - groupes for request adding
		Set<GroupData> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
        List<PlanComputationRequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
        List<Plan> groupPlans = new ArrayList<>();

		Set<PlanComputationRequest> onBoardRequestLock = null;
		if(vehicle.getRequestsOnBoard().isEmpty()){
			
			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
			Plan emptyPlan = new Plan((int) startTime, vehicle);
			groupPlans.add(emptyPlan);
		}
		else{
			// BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
			LinkedHashSet<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
			onBoardRequestLock = group;
			
			long startTimeNano = 0;
			if(recordTime){
				groupCounts[group.size() - 1]++;
				groupCountsPlanExists[group.size() - 1]++;
				startTimeNano = System.nanoTime();
			}
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan initialPlan 
					= optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
			groupPlans.add(initialPlan);
			
			if(recordTime){
				long computationDurationNano = startTimeNano - System.nanoTime();
				computationalTimes[group.size() - 1] += computationDurationNano / MILLION;
				computationalTimesPlanExists[group.size() - 1] += computationDurationNano / MILLION;	
			}
	
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

			long startTimeNano = 0;
			if(recordTime){
				groupCounts[group.size() - 1]++;
				startTimeNano = System.nanoTime();
			}
			
			Plan plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
			if(plan != null) {
				feasibleRequests.add(request);
				currentGroups.add(new GroupData(group, onBoardRequestLock));
				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
				if(vehicle.getRequestsOnBoard().isEmpty()){
					groupPlans.add(plan);
				}			
			}
			
			if(recordTime){
				long computationDurationNano = startTimeNano - System.nanoTime();
				int timeInMs = (int) (computationDurationNano / MILLION);
				if(plan != null){
					groupCountsPlanExists[group.size() - 1]++;
					computationalTimesPlanExists[group.size() - 1] += timeInMs;
				}
						
				computationalTimes[group.size() - 1] += timeInMs;	
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

						long startTimeNano = 0;
						if(recordTime){
							groupCounts[newGroupToCheck.size() - 1]++;
							startTimeNano = System.nanoTime();
						}
						
						Plan plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, newGroupToCheck, startTime, false);
						if(plan != null) {
			
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
						
						if(recordTime){
							long computationDurationNano = startTimeNano - System.nanoTime();
							int timeInMs = (int) (computationDurationNano / MILLION);
							if(plan != null){
								groupCountsPlanExists[newGroupToCheck.size() - 1]++;
								computationalTimesPlanExists[newGroupToCheck.size() - 1] += timeInMs;
							}

							computationalTimes[newGroupToCheck.size() - 1] += timeInMs;	
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

			if(optimalVehiclePlanFinder.groupFeasible(group, startTime, vehicleCapacity)) {
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

						if(optimalVehiclePlanFinder.groupFeasible(newGroupToCheck, startTime, vehicleCapacity)) {
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
