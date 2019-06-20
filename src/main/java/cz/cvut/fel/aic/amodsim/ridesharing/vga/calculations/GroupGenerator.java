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
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.Common;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;



@Singleton
public class GroupGenerator<V extends IOptimalPlanVehicle> {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);

	private static final int GROUP_RECORDS_BATCH_SIZE = 10_000;


	
	private final OptimalVehiclePlanFinder optimalVehiclePlanFinder;
	
	private final int vehicleCapacity;
	
	private final boolean recordTime;
	
	private final int maxGroupSize;
	
	private final long groupGenerationTimeLimitInNanoseconds;
	
	private CsvWriter groupRecordWriter = null;
	
	
	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;
	
	private List<String[]> groupRecords;
	private final Boolean exportGroupData;

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
	public GroupGenerator(OptimalVehiclePlanFinder optimalVehiclePlanFinder, AmodsimConfig config) {
		this.optimalVehiclePlanFinder = optimalVehiclePlanFinder;
		vehicleCapacity = config.ridesharing.vehicleCapacity;
		recordTime = config.ridesharing.vga.logPlanComputationalTime;
		maxGroupSize = config.ridesharing.vga.maxGroupSize;
		groupGenerationTimeLimitInNanoseconds = config.ridesharing.vga.groupGenerationTimeLimit * 1000;
		exportGroupData = config.ridesharing.vga.exportGroupData;
		if(exportGroupData){
			try {
				groupRecordWriter =  new CsvWriter(
						Common.getFileWriter(config.statistics.groupDataFilePath));
			} catch (IOException ex) {
				Logger.getLogger(GroupGenerator.class.getName()).log(Level.SEVERE, null, ex);
			}
			groupRecords = new ArrayList(GROUP_RECORDS_BATCH_SIZE);
		}
	}

	public List<Plan> generateGroupsForVehicle(V vehicle, LinkedHashSet<PlanComputationRequest> requests, int startTime) {
		
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
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			Plan initialPlan;
			if(recordTime){
				groupCounts.increment(group.size() - 1);
				groupCountsPlanExists.increment(group.size() - 1);
				initialPlan = Benchmark.measureTime(() -> 
						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
				computationalTimes.increment(group.size() - 1, Benchmark.getDurationMsInt());
				computationalTimesPlanExists.increment(group.size() - 1, Benchmark.getDurationMsInt());
			}
			else{
				initialPlan 
					= optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
			}
			
			groupPlans.add(initialPlan);
	
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
			if(recordTime){
				groupCounts.increment(group.size() - 1);
				plan = Benchmark.measureTime(() -> 
						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
				int timeInMs = Benchmark.getDurationMsInt();
				if(plan != null){
					groupCountsPlanExists.increment(group.size() - 1);
					computationalTimesPlanExists.increment(group.size() - 1, timeInMs);
				}			
				computationalTimes.increment(group.size() - 1, timeInMs);
			}
			else{
				plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
			}
			
			if(exportGroupData){
				saveGroupData(vehicle, startTime, group, plan != null);
			}

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
					for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
						if(!currentGroups.contains(new GroupData(subset, null))){
							checkFeasibility = false;
							break;
						}
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
							plan = Benchmark.measureTime(() -> 
									optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
											vehicle, newGroupToCheck, startTime, false));
							int timeInMs = Benchmark.getDurationMsInt();
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
						
						if(exportGroupData){
							saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
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

//							if(groups.size() > 50){
//								return groups;
//							}
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

	private void saveGroupData(V vehicle, int startTime, LinkedHashSet<PlanComputationRequest> group, boolean feasible) {
		int size = group.size() * 6 + 4;
		String[] record = new String[size];
		record[0] = Boolean.toString(feasible);
		record[1] = Integer.toString(vehicle.getRequestsOnBoard().size());
		record[2] = Integer.toString(vehicle.getPosition().latE6);
		record[3] = Integer.toString(vehicle.getPosition().lonE6);
		
		int index = 4;
		for(PlanComputationRequest planComputationRequest: group){
			record[index++] = Integer.toString(planComputationRequest.getPickUpAction().getPosition().latE6);
			record[index++] = Integer.toString(planComputationRequest.getPickUpAction().getPosition().lonE6);
			record[index++] = Integer.toString(planComputationRequest.getPickUpAction().getMaxTime() - startTime);
			record[index++] = Integer.toString(planComputationRequest.getDropOffAction().getPosition().latE6);
			record[index++] = Integer.toString(planComputationRequest.getDropOffAction().getPosition().lonE6);
			record[index++] = Integer.toString(planComputationRequest.getDropOffAction().getMaxTime() - startTime);
		}
		groupRecords.add(record);
		if(groupRecords.size() == GROUP_RECORDS_BATCH_SIZE){
			exportGroupData();
			groupRecords = new ArrayList<>();
		}
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
		
	private void exportGroupData() {
		try {
			for (String[] groupRecord : groupRecords) {
				groupRecordWriter.writeLine(groupRecord);
			}
			groupRecordWriter.flush();
			groupRecords = new ArrayList<>(GROUP_RECORDS_BATCH_SIZE);
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}
}
