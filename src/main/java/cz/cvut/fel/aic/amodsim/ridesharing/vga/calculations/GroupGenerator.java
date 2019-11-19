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
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import com.google.inject.Inject;
import com.google.inject.Provider;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.LoggerFactory;



public class GroupGenerator<V extends IOptimalPlanVehicle> {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);
	


	private final OptimalVehiclePlanFinder optimalVehiclePlanFinder;
	
	private final int vehicleCapacity;
	
	private final boolean recordTime;
	
	private final int maxGroupSize;
	
	private final long groupGenerationTimeLimitInNanoseconds;
	
	private final Boolean exportGroupData;
	
	private final Provider<VehicleGroupAssignmentSolver> vgaSolverProvider;
	

	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;
	
	
	

	public FlexArray getGroupCounts() {
		return groupCounts;
	}

	public FlexArray getGroupCountsPlanExists() {
		return groupCountsPlanExists;
	}

	public FlexArray getComputationalTimes() {
		return computationalTimes;
	}

	public FlexArray getComputationalTimesPlanExists() {
		return computationalTimesPlanExists;
	}
	
	
	

	@Inject
	public GroupGenerator(OptimalVehiclePlanFinder optimalVehiclePlanFinder, AmodsimConfig config, 
			Provider<VehicleGroupAssignmentSolver> vgaSolverProvider) {
		this.optimalVehiclePlanFinder = optimalVehiclePlanFinder;
		this.vgaSolverProvider = vgaSolverProvider;
		vehicleCapacity = config.ridesharing.vehicleCapacity;
		recordTime = config.ridesharing.vga.logPlanComputationalTime;
		maxGroupSize = config.ridesharing.vga.maxGroupSize;
		groupGenerationTimeLimitInNanoseconds = config.ridesharing.vga.groupGenerationTimeLimit * 1000;
		exportGroupData = config.ridesharing.vga.exportGroupData;	
	}

	public List<Plan> generateGroupsForVehicle(V vehicle, Collection<PlanComputationRequest> requests, int startTime) {
		
		/* STATISTICS */
		long group_generation_start_time = System.nanoTime();
		boolean stop = false;
		
		// statistics
		if(recordTime){
			int size = vehicle.getRequestsOnBoard().size() + 1;
			groupCounts = new FlexArray(size);
			groupCountsPlanExists = new FlexArray(size);
			computationalTimes = new FlexArray(size);
			computationalTimesPlanExists = new FlexArray(size);
		}
		
		/* COLLECTION INIT */
		// F_v^{k - 1} - groupes for request adding
		Set<GroupData> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
		List<PlanComputationRequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
		List<Plan> groupPlans = new ArrayList<>();

		/* BASE PLAN GENERATION */
		Set<PlanComputationRequest> onBoardRequestLock = null;
		RideSharingOnDemandVehicle rVehicle = (RideSharingOnDemandVehicle) vehicle.getRealVehicle();
		if(vehicle.getRequestsOnBoard().isEmpty()){
			
			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
			Plan emptyPlan = new Plan((int) startTime, vehicle);
			groupPlans.add(emptyPlan);
		}
		if(rVehicle.getCurrentPlan().getLength() > 1){
			// BASE PLAN - for each driving vehicle, we add a base plan the previously assigned plan
			LinkedHashSet<PlanComputationRequest> onBoardRequests = vehicle.getRequestsOnBoard();
			onBoardRequestLock = onBoardRequests;
			DriverPlan currentPlan = rVehicle.getCurrentPlan();
			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
			for(PlanAction action: currentPlan.plan){
				if(action instanceof PlanRequestAction){
					group.add(((PlanRequestAction) action).getRequest());
				}
			}
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan initialPlan;
			if(recordTime){
				groupCounts.increment(group.size() - 1);
				groupCountsPlanExists.increment(group.size() - 1);
				Benchmark benchmark = new Benchmark();
				initialPlan = benchmark.measureTime(() -> 
						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
				computationalTimes.increment(group.size() - 1, benchmark.getDurationMsInt());
				computationalTimesPlanExists.increment(group.size() - 1, benchmark.getDurationMsInt());
			}
			else{
				initialPlan 
					= optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
			}
			
			groupPlans.add(initialPlan);
	
			/* we have to add onboard request to feasible requests set and to current groups of size 1 */
			for (PlanComputationRequest request : group) {
				feasibleRequests.add(request);
				Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
				singleRequestGroup.add(request);
				currentGroups.add(new GroupData(singleRequestGroup, onBoardRequestLock));
			}
		}
		
		// groups of size 1
		for (PlanComputationRequest request : requests) {
			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>(1);
			group.add(request);

			Plan plan;
			if(recordTime){
				groupCounts.increment(group.size() - 1);
				Benchmark benchmark = new Benchmark();
				plan = benchmark.measureTime(() -> 
						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
				int timeInMs = benchmark.getDurationMsInt();
				if(plan != null){
					groupCountsPlanExists.increment(group.size() - 1);
					computationalTimesPlanExists.increment(group.size() - 1, timeInMs);
				}			
				computationalTimes.increment(group.size() - 1, timeInMs);
			}
			else{
				plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
			}
			
//			if(exportGroupData){
//				vgaSolverProvider.get().saveGroupData(vehicle, startTime, group, plan != null);
//			}

			if(plan != null) {
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
		while(!currentGroups.isEmpty() && (maxGroupSize == 0 || currentGroupSize < maxGroupSize) && !stop) {

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
					Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
					for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
						subset.remove(planComputationRequest);
						if(!currentGroups.contains(new GroupData(subset, null))){
							checkFeasibility = false;
							break;
						}
						subset.add(planComputationRequest);
					}
					
					if(checkFeasibility){

						Plan plan ;
						
						if(groupGenerationTimeLimitInNanoseconds > 0){
							long currentDuration = System.nanoTime() - group_generation_start_time;
							if(currentDuration > groupGenerationTimeLimitInNanoseconds){
								stop = true;
								break;
							}
						}
						
						if(recordTime){
							groupCounts.increment(newGroupToCheck.size() - 1);
							Benchmark benchmark = new Benchmark();
							plan = benchmark.measureTime(() -> 
									optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
											vehicle, newGroupToCheck, startTime, false));
							int timeInMs = benchmark.getDurationMsInt();
							if(plan != null){
								groupCountsPlanExists.increment(newGroupToCheck.size() - 1);
								computationalTimesPlanExists.increment(newGroupToCheck.size() - 1, timeInMs);
							}
							computationalTimes.increment(newGroupToCheck.size() - 1, timeInMs);
						}
						else{
							plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
									vehicle, newGroupToCheck, startTime, false);
						}
						
						if(exportGroupData && currentGroupSize >= 3){
							vgaSolverProvider.get().saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
						}

						if(plan != null) {
			
							if(groupData.onboardRequestLock == null || newGroupToCheck.containsAll(groupData.onboardRequestLock)){
								newCurrentGroups.add(new GroupData(newGroupToCheck));
								groupPlans.add(plan);
							}
							else{
								newCurrentGroups.add(new GroupData(newGroupToCheck, groupData.onboardRequestLock));
							}
//							if(groups.size() > 50){
//								return groups;
//							}
						}
					}
				}
				if(stop){
					break;
				}
			}

			currentGroups = newCurrentGroups;
			currentGroupSize++;
		}
		
		return groupPlans;
	}

	

	
	private class GroupData {
		private final Set<PlanComputationRequest> requests;
		
		private final Set<PlanComputationRequest> onboardRequestLock;
		
		private int hash;

		private GroupData(Set<PlanComputationRequest> requests) {
			this(requests, null);
		}
		
		private GroupData(Set<PlanComputationRequest> requests, 
				Set<PlanComputationRequest> onboardRequestLock) {
			this.requests = requests;
			this.onboardRequestLock = onboardRequestLock;
			hash = 0;
		}

		@Override
		public int hashCode() {
			if(hash == 0){
				hash = this.requests.hashCode() % 1_200_000;
			}
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
