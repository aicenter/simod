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

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.Common;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author matal
 */
public class GroupGeneratorv2<V extends IOptimalPlanVehicle> {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);

	private static final int GROUP_RECORDS_BATCH_SIZE = 10_000;

    private final NN nn;
	
	private final OptimalVehiclePlanFinder optimalVehiclePlanFinder;
	
	private final int vehicleCapacity;
	
	private final boolean recordTime;
	
	private final int maxGroupSize;
	
	private final long groupGenerationTimeLimitInNanoseconds;
	public int true_count, false_count;
    public int nn_time[];
	private CsvWriter groupRecordWriter = null;
	
	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;
    
	private Map<V,FlexArray> groupCountPerVehicle;
	
	private Map<V,FlexArray> groupCountsPlanExistPerVehicle;
	
	private Map<V,FlexArray> computationalTimePerVehicle;
	
	private Map<V,FlexArray> computationalTimesPlanExistPerVehicle;	
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

    public Map<V, FlexArray> getGroupCountPerVehicle() {
        return groupCountPerVehicle;
    }

    public Map<V, FlexArray> getGroupCountsPlanExistPerVehicle() {
        return groupCountsPlanExistPerVehicle;
    }

    public Map<V, FlexArray> getComputationalTimePerVehicle() {
        return computationalTimePerVehicle;
    }

    public Map<V, FlexArray> getComputationalTimesPlanExistPerVehicle() {
        return computationalTimesPlanExistPerVehicle;
    }
	
	
	

	@Inject
	public GroupGeneratorv2(OptimalVehiclePlanFinder optimalVehiclePlanFinder, AmodsimConfig config) {
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
        nn = new MatrixMultiplyNN();
        nn_time = new int[5];
        true_count = 0;
        false_count = 0;
        for (int i = 0; i < this.nn_time.length; i++) {
            this.nn_time[i] = 0;
        }
	}
	public List<Plan> generateGroupsForVehicleClean(V vehicle, Iterable<PlanComputationRequest> requests, int startTime) {
		
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
                                feasibleRequests.add(request);
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
            // groups of size 1,2 and bigger than 7 are not supported in NN algorithm
            if(currentGroupSize == 1 || currentGroupSize > 6){
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);

                        // check whether all n-1 subsets are in F_v^{k - 1}
                        boolean checkFeasibility = true;
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                            if(!currentGroups.contains(new GroupData(subset))){
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

                                if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                    newCurrentGroups.add(new GroupData(newGroupToCheck));
                                    groupPlans.add(plan);
                                }
                                else{
                                    newCurrentGroups.add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock()));
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
            }
            else{
                Set<GroupData> groupsForNN = new LinkedHashSet<>();
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);
                        boolean checkFeasibility = true;
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                            if(!currentGroups.contains(new GroupData(subset))){
                                checkFeasibility = false;
                                break;
                            }
                        }

                        if(checkFeasibility){                   
                            GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock());
                            groupsForNN.add(gd);
                        }
                    }
                    if(stop){
                        break;
                    }
                }       
                // NN alghorithm
                for (GroupData newGroupToCheck : groupsForNN) {
                    
                    Plan plan ;

                    if(groupGenerationTimeLimitInNanoseconds > 0){
                        long currentDuration = System.nanoTime() - group_generation_start_time;
                        if(currentDuration > groupGenerationTimeLimitInNanoseconds){
                            stop = true;
                            break;
                        }
                    }

                    if(recordTime){
                        groupCounts.increment(newGroupToCheck.getRequests().size() - 1);
                        plan = Benchmark.measureTime(() -> 
                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                        vehicle, (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                        int timeInMs = Benchmark.getDurationMsInt();
                        if(plan != null){
                            groupCountsPlanExists.increment(newGroupToCheck.getRequests().size() - 1);
                            computationalTimesPlanExists.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                        }
                        computationalTimes.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                    }
                    else{
                        plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                vehicle,(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                    }

                    if(exportGroupData){
                        saveGroupData(vehicle, startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                    }

                    if(plan != null) {

                        if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                            newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests()));
                            groupPlans.add(plan);
                        }
                        else{
                            newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock()));
                        }
    //							if(groups.size() > 50){
    //								return groups;
    //							}
                    }
                    if(stop){
                        break;
                    }
                }
            }
			currentGroups = newCurrentGroups;
			currentGroupSize++;
		}
		return groupPlans;
	}
	public List<Plan> generateGroupsForVehicleNN(V vehicle, Iterable<PlanComputationRequest> requests, int startTime) {
		
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
            // groups of size 1,2 and bigger than 7 are not supported in NN algorithm
            if(currentGroupSize == 1 || currentGroupSize > 6){
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);

                        // check whether all n-1 subsets are in F_v^{k - 1}
                        boolean checkFeasibility = true;
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                            if(!currentGroups.contains(new GroupData(subset))){
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

                                if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                    newCurrentGroups.add(new GroupData(newGroupToCheck));
                                    groupPlans.add(plan);
                                }
                                else{
                                    newCurrentGroups.add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock()));
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
            }
            else{
                Set<GroupData> groupsForNN = new LinkedHashSet<>();
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);
                        boolean checkFeasibility = true;
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                            if(!currentGroups.contains(new GroupData(subset))){
                                checkFeasibility = false;
                                break;
                            }
                        }

                        if(checkFeasibility){                   
                            GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock());
                            groupsForNN.add(gd);
                        }
                    }
                    if(stop){
                        break;
                    }
                }
                if(!groupsForNN.isEmpty()){
                    nn.setProbability(groupsForNN, vehicle, currentGroupSize);
                }         
                // NN alghorithm
                for (GroupData newGroupToCheck : groupsForNN) {
                    if(newGroupToCheck.getFeasible() < 0.5){
                        //System.out.println("False: "+false_count++);
                        continue;
                    }/*else{
                        System.out.println("True: "+true_count++);
                    }*/
                    
                    Plan plan ;

                    if(groupGenerationTimeLimitInNanoseconds > 0){
                        long currentDuration = System.nanoTime() - group_generation_start_time;
                        if(currentDuration > groupGenerationTimeLimitInNanoseconds){
                            stop = true;
                            break;
                        }
                    }

                    if(recordTime){
                        groupCounts.increment(newGroupToCheck.getRequests().size() - 1);
                        plan = Benchmark.measureTime(() -> 
                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                        vehicle, (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                        int timeInMs = Benchmark.getDurationMsInt();
                        if(plan != null){
                            groupCountsPlanExists.increment(newGroupToCheck.getRequests().size() - 1);
                            computationalTimesPlanExists.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                        }
                        computationalTimes.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                    }
                    else{
                        plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                vehicle,(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                    }

                    if(exportGroupData){
                        saveGroupData(vehicle, startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                    }

                    if(plan != null) {

                        if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                            newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests()));
                            groupPlans.add(plan);
                        }
                        else{
                            newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock()));
                        }
    //							if(groups.size() > 50){
    //								return groups;
    //							}
                    }
                    if(stop){
                        break;
                    }
                }
            }
			currentGroups = newCurrentGroups;
			currentGroupSize++;
		}
		return groupPlans;
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
            if(currentGroupSize == 1){
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);

                        // check whether all n-1 subsets are in F_v^{k - 1}
                        boolean checkFeasibility = true;
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                            if(!currentGroups.contains(new GroupData(subset))){
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

                                if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                    newCurrentGroups.add(new GroupData(newGroupToCheck));
                                    groupPlans.add(plan);
                                }
                                else{
                                    newCurrentGroups.add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock()));
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
            }else{
                // set of groups for NN
                Set<GroupData> groupsForNN = new LinkedHashSet<>();
                for (GroupData groupData : currentGroups) {
                    for (PlanComputationRequest request : feasibleRequests) {
                        if (groupData.getRequests().contains(request)){
                            continue;
                        }

                        // G'
                        LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                        newGroupToCheck.add(request);

                        if (currentCheckedGroups.contains(newGroupToCheck)){
                            continue;
                        }
                        currentCheckedGroups.add(newGroupToCheck);
                        GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock());
                        groupsForNN.add(gd);
                    }
                    if(stop){
                        break;
                    }
                }
                if(!groupsForNN.isEmpty()){
                    nn.setProbability(groupsForNN, vehicle, currentGroupSize);
                }
                // NN alghorithm
                for (GroupData newGroupToCheck : groupsForNN) {
                    if(newGroupToCheck.getFeasible() < 0.5) continue;
                        boolean checkFeasibility = true;
                        LinkedHashSet<PlanComputationRequest> set = (LinkedHashSet) newGroupToCheck.getRequests();
                        for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(set)){
                            if(!currentGroups.contains(new GroupData(subset))){
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
                            groupCounts.increment(newGroupToCheck.getRequests().size() - 1);
                            plan = Benchmark.measureTime(() -> 
                                    optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                            vehicle, (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                            int timeInMs = Benchmark.getDurationMsInt();
                            if(plan != null){
                                groupCountsPlanExists.increment(newGroupToCheck.getRequests().size() - 1);
                                computationalTimesPlanExists.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                            }
                            computationalTimes.increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                        }
                        else{
                            plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                    vehicle,(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                        }

                        if(exportGroupData){
                            saveGroupData(vehicle, startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                        }

                        if(plan != null) {

                            if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                                newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests()));
                                groupPlans.add(plan);
                            }
                            else{
                                newCurrentGroups.add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock()));
                            }
        //							if(groups.size() > 50){
        //								return groups;
        //							}
                        }
                    }
                    if(stop){
                        break;
                    }
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
    public Map<V,List<Plan>> generateGroupsForVehicleClean(List<V> vehicles, Iterable<DefaultPlanComputationRequest> requests, int startTime) {
            //group generation time limit is not here

            // statistics
            if(recordTime){
                int size = 0;
                groupCountPerVehicle = new HashMap<>();
                groupCountsPlanExistPerVehicle = new HashMap<>();
                computationalTimePerVehicle = new HashMap<>();
                computationalTimesPlanExistPerVehicle = new HashMap<>();
                for (V vehicle : vehicles) {
                    size = vehicle.getRequestsOnBoard().size() + 1;
                    groupCountPerVehicle.put(vehicle, new FlexArray(size));
                    groupCountsPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimePerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimesPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                }
            }

            // F_v^{k - 1} - groupes for request adding
            Map<V,Set<GroupData>> currentGroups = new HashMap<>();
            // F_v^{1}
            Map<V,List<PlanComputationRequest>> feasibleRequests = new HashMap<>();
            for (V vehicle : vehicles) {
                currentGroups.put(vehicle,new LinkedHashSet<>());
                feasibleRequests.put(vehicle, new ArrayList<>());
            }
            
            

            // F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
            Map<V,List<Plan>> groupPlans = new HashMap<>();

            Map<V,Set<PlanComputationRequest>> onBoardRequestLock = new HashMap<>();
            for (V vehicle: vehicles) {
                if(vehicle.getRequestsOnBoard().isEmpty()){

                        // BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
                        Plan emptyPlan = new Plan((int) startTime, vehicle);
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(emptyPlan);
                        onBoardRequestLock.put(vehicle, null);
                }
                else{
                        // BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
                        LinkedHashSet<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
                        onBoardRequestLock.put(vehicle, group);

                        // currently, the time window has to be ignored, because the planner underestimates the cost
                        Plan initialPlan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                initialPlan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                        }
                        else{
                                initialPlan 
                                        = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
                        }
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(initialPlan);

                        for (PlanComputationRequest request : group) {
                                Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
                                singleRequestGroup.add(request);
                                currentGroups.get(vehicle).add(new GroupData(singleRequestGroup, group, vehicle));
                        }
                }           
            }


            // groups of size 1
            for (PlanComputationRequest request : requests) {
                    LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
                    group.add(request);
                    for (V vehicle : vehicles) {
                        Plan plan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                        groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                        computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                                }			
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                        }
                        else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
                        }

                        if(exportGroupData){
                                saveGroupData(vehicle, startTime, group, plan != null);
                        }

                        if(plan != null) {
                                feasibleRequests.get(vehicle).add(request);
                                currentGroups.get(vehicle).add(new GroupData(group, onBoardRequestLock.get(vehicle), vehicle));
                                //if the vehicle is empty, feasible requests are feasible plans and are used as base groups
                                if(vehicle.getRequestsOnBoard().isEmpty()){
                                        groupPlans.get(vehicle).add(plan);
                                }			
                        }
                    }
            }


            // generate other groups
            int currentGroupSize = 1;
            boolean end = false;
            while(!end){
                end = true;
                if((maxGroupSize == 0 || currentGroupSize < maxGroupSize)){
                    Map<V,Set<GroupData>> newCurrentGroups = new HashMap<>();
                    if(currentGroupSize == 1 || currentGroupSize > 6){
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;                                 

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);

                                    // check whether all n-1 subsets are in F_v^{k - 1}
                                    boolean checkFeasibility = true;
                                    for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                    }

                                    if(checkFeasibility){
                                        end = false;
                                        Plan plan ;

                                        if(recordTime){
                                            groupCountPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                            plan = Benchmark.measureTime(() -> 
                                                    optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                            vehicle, newGroupToCheck, startTime, false));
                                            int timeInMs = Benchmark.getDurationMsInt();
                                            if(plan != null){
                                                groupCountsPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                            }
                                            computationalTimePerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                        }
                                        else{
                                            plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                    vehicle, newGroupToCheck, startTime, false);
                                        }

                                        if(exportGroupData){
                                            saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
                                        }

                                        if(plan != null) {

                                            if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, vehicle));
                                                groupPlans.get(vehicle).add(plan);
                                            }
                                            else{
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle));
                                            }
                //							if(groups.size() > 50){
                //								return groups;
                //							}
                                        }
                                    }
                                }
                            }
                        }                       
                    }
                    else{
                        Set<GroupData> groupsForNN = new LinkedHashSet<>();
                        
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                                           
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);
                                    boolean checkFeasibility = true;
                                    for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                    }

                                    if(checkFeasibility){
                                        GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle);
                                        groupsForNN.add(gd);
                                    }
                                }
                            }              
                        }
                        if(!groupsForNN.isEmpty()){
                            end = false;
                        }   
                        for (GroupData newGroupToCheck : groupsForNN) {

                            Plan plan ;

                            if(recordTime){
                                groupCountPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                        optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                newGroupToCheck.getVehicle(), (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                    groupCountsPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                    computationalTimesPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                                }
                                computationalTimePerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                            }
                            else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                        newGroupToCheck.getVehicle(),(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                            }

                            if(exportGroupData){
                                saveGroupData((V)newGroupToCheck.getVehicle(), startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                            }

                            if(plan != null) {

                                if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getVehicle()));
                                    groupPlans.get(newGroupToCheck.getVehicle()).add(plan);
                                }
                                else{
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock(), newGroupToCheck.getVehicle()));
                                }
            //							if(groups.size() > 50){
            //								return groups;
            //							}
                            }
                        }
                    }
                    currentGroups = newCurrentGroups;
                    currentGroupSize++; 
                }
            }
        return groupPlans;
    }
    public Map<V,List<Plan>> generateGroupsForVehicleNN(List<V> vehicles, Collection<DefaultPlanComputationRequest> requests, int startTime) {
            //group generation time limit is not here

            // statistics
            if(recordTime){
                int size = 0;
                groupCountPerVehicle = new HashMap<>();
                groupCountsPlanExistPerVehicle = new HashMap<>();
                computationalTimePerVehicle = new HashMap<>();
                computationalTimesPlanExistPerVehicle = new HashMap<>();
                for (V vehicle : vehicles) {
                    size = vehicle.getRequestsOnBoard().size() + 1;
                    groupCountPerVehicle.put(vehicle, new FlexArray(size));
                    groupCountsPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimePerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimesPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                }
            }

            // F_v^{k - 1} - groupes for request adding
            Map<V,Set<GroupData>> currentGroups = new HashMap<>();
            // F_v^{1}
            Map<V,List<PlanComputationRequest>> feasibleRequests = new HashMap<>();
            for (V vehicle : vehicles) {
                currentGroups.put(vehicle,new LinkedHashSet<>());
                feasibleRequests.put(vehicle, new ArrayList<>());
            }
            
            

            // F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
            Map<V,List<Plan>> groupPlans = new HashMap<>();

            Map<V,Set<PlanComputationRequest>> onBoardRequestLock = new HashMap<>();
            for (V vehicle: vehicles) {
                if(vehicle.getRequestsOnBoard().isEmpty()){

                        // BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
                        Plan emptyPlan = new Plan((int) startTime, vehicle);
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(emptyPlan);
                        onBoardRequestLock.put(vehicle, null);
                }
                else{
                        // BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
                        LinkedHashSet<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
                        onBoardRequestLock.put(vehicle, group);

                        // currently, the time window has to be ignored, because the planner underestimates the cost
                        Plan initialPlan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                initialPlan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                        }
                        else{
                                initialPlan 
                                        = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
                        }
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(initialPlan);

                        for (PlanComputationRequest request : group) {
                                feasibleRequests.get(vehicle).add(request);
                                Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
                                singleRequestGroup.add(request);
                                currentGroups.get(vehicle).add(new GroupData(singleRequestGroup, group, vehicle));
                        }
                }           
            }


            // groups of size 1
            for (PlanComputationRequest request : requests) {
                    LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
                    group.add(request);
                    for (V vehicle : vehicles) {
                        Plan plan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                        groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                        computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                                }			
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                        }
                        else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
                        }

                        if(exportGroupData){
                                saveGroupData(vehicle, startTime, group, plan != null);
                        }

                        if(plan != null) {
                                feasibleRequests.get(vehicle).add(request);
                                currentGroups.get(vehicle).add(new GroupData(group, onBoardRequestLock.get(vehicle), vehicle));
                                //if the vehicle is empty, feasible requests are feasible plans and are used as base groups
                                if(vehicle.getRequestsOnBoard().isEmpty()){
                                        groupPlans.get(vehicle).add(plan);
                                }			
                        }
                    }
            }


            // generate other groups
            int currentGroupSize = 1;
            boolean end = false;
            while(!end){
                end = true;
                if((maxGroupSize == 0 || currentGroupSize < maxGroupSize)){
                    Map<V,Set<GroupData>> newCurrentGroups = new HashMap<>();
                    if(currentGroupSize == 1 || currentGroupSize > 6){
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;                                 

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);

                                    // check whether all n-1 subsets are in F_v^{k - 1}
                                    boolean checkFeasibility = true;
                                    //for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                    Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
                                    for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
					subset.remove(planComputationRequest);

                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                        subset.add(planComputationRequest);
                                    }

                                    if(checkFeasibility){
                                        end = false;
                                        Plan plan ;

                                        if(recordTime){
                                            groupCountPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                            plan = Benchmark.measureTime(() -> 
                                                    optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                            vehicle, newGroupToCheck, startTime, false));
                                            int timeInMs = Benchmark.getDurationMsInt();
                                            if(plan != null){
                                                groupCountsPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                            }
                                            computationalTimePerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                        }
                                        else{
                                            plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                    vehicle, newGroupToCheck, startTime, false);
                                        }

                                        if(exportGroupData){
                                            saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
                                        }

                                        if(plan != null) {

                                            if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, vehicle));
                                                groupPlans.get(vehicle).add(plan);
                                            }
                                            else{
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle));
                                            }
                //							if(groups.size() > 50){
                //								return groups;
                //							}
                                        }
                                    }
                                }
                            }
                        }                       
                    }
                    else{
                        final Set<GroupData> groupsForNN = new LinkedHashSet<>();
                        
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                                           
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);
                                    boolean checkFeasibility = true;
                                    //for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                    Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
                                    for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
					subset.remove(planComputationRequest);

                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                        subset.add(planComputationRequest);
                                    }

                                    if(checkFeasibility){                   
                                        GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle);
                                        groupsForNN.add(gd);
                                    }
                                }
                            }              
                        }
                        if(!groupsForNN.isEmpty()){
                            end = false;
                            final int h = currentGroupSize;
                            Benchmark.measureTime(() -> nn.setProbability(groupsForNN, h));
                            nn_time[currentGroupSize-2] += Benchmark.getDurationMsInt();
                        }   
                        for (GroupData newGroupToCheck : groupsForNN) {
                            if(newGroupToCheck.getFeasible() < 0.5){
                                false_count++;
                                continue;
                            }else{
                                true_count++;
                            }

                            Plan plan ;

                            if(recordTime){
                                groupCountPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                        optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                newGroupToCheck.getVehicle(), (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                    groupCountsPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                    computationalTimesPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                                }
                                computationalTimePerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                            }
                            else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                        newGroupToCheck.getVehicle(),(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                            }

                            if(exportGroupData){
                                saveGroupData((V)newGroupToCheck.getVehicle(), startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                            }

                            if(plan != null) {

                                if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getVehicle()));
                                    groupPlans.get(newGroupToCheck.getVehicle()).add(plan);
                                }
                                else{
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock(), newGroupToCheck.getVehicle()));
                                }
            //							if(groups.size() > 50){
            //								return groups;
            //							}
                            }
                        }
                    }
                    currentGroups = newCurrentGroups;
                    currentGroupSize++; 
                }
            }
        return groupPlans;
    }
    public Map<V,List<Plan>> generateGroupsForVehicleNNBefore(List<V> vehicles, Iterable<DefaultPlanComputationRequest> requests, int startTime) {
            //group generation time limit is not here

            // statistics
            if(recordTime){
                int size = 0;
                groupCountPerVehicle = new HashMap<>();
                groupCountsPlanExistPerVehicle = new HashMap<>();
                computationalTimePerVehicle = new HashMap<>();
                computationalTimesPlanExistPerVehicle = new HashMap<>();
                for (V vehicle : vehicles) {
                    size = vehicle.getRequestsOnBoard().size() + 1;
                    groupCountPerVehicle.put(vehicle, new FlexArray(size));
                    groupCountsPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimePerVehicle.put(vehicle, new FlexArray(size));
                    computationalTimesPlanExistPerVehicle.put(vehicle, new FlexArray(size));
                }
            }

            // F_v^{k - 1} - groupes for request adding
            Map<V,Set<GroupData>> currentGroups = new HashMap<>();
            // F_v^{1}
            Map<V,List<PlanComputationRequest>> feasibleRequests = new HashMap<>();
            for (V vehicle : vehicles) {
                currentGroups.put(vehicle,new LinkedHashSet<>());
                feasibleRequests.put(vehicle, new ArrayList<>());
            }
            
            

            // F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
            Map<V,List<Plan>> groupPlans = new HashMap<>();

            Map<V,Set<PlanComputationRequest>> onBoardRequestLock = new HashMap<>();
            for (V vehicle: vehicles) {
                if(vehicle.getRequestsOnBoard().isEmpty()){

                        // BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
                        Plan emptyPlan = new Plan((int) startTime, vehicle);
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(emptyPlan);
                        onBoardRequestLock.put(vehicle, null);
                }
                else{
                        // BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
                        LinkedHashSet<PlanComputationRequest> group = vehicle.getRequestsOnBoard();
                        onBoardRequestLock.put(vehicle, group);

                        // currently, the time window has to be ignored, because the planner underestimates the cost
                        Plan initialPlan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                initialPlan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, Benchmark.getDurationMsInt());
                        }
                        else{
                                initialPlan 
                                        = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
                        }
                        groupPlans.put(vehicle, new ArrayList<>());
                        groupPlans.get(vehicle).add(initialPlan);

                        for (PlanComputationRequest request : group) {
                                feasibleRequests.get(vehicle).add(request);
                                Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
                                singleRequestGroup.add(request);
                                currentGroups.get(vehicle).add(new GroupData(singleRequestGroup, group, vehicle));
                        }
                }           
            }


            // groups of size 1
            for (PlanComputationRequest request : requests) {
                    LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
                    group.add(request);
                    for (V vehicle : vehicles) {
                        Plan plan;
                        if(recordTime){
                                groupCountPerVehicle.get(vehicle).increment(group.size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                                optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                        groupCountsPlanExistPerVehicle.get(vehicle).increment(group.size() - 1);
                                        computationalTimesPlanExistPerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                                }			
                                computationalTimePerVehicle.get(vehicle).increment(group.size() - 1, timeInMs);
                        }
                        else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
                        }

                        if(exportGroupData){
                                saveGroupData(vehicle, startTime, group, plan != null);
                        }

                        if(plan != null) {
                                feasibleRequests.get(vehicle).add(request);
                                currentGroups.get(vehicle).add(new GroupData(group, onBoardRequestLock.get(vehicle), vehicle));
                                //if the vehicle is empty, feasible requests are feasible plans and are used as base groups
                                if(vehicle.getRequestsOnBoard().isEmpty()){
                                        groupPlans.get(vehicle).add(plan);
                                }			
                        }
                    }
            }


            // generate other groups
            int currentGroupSize = 1;
            boolean end = false;
            while(!end){
                end = true;
                if((maxGroupSize == 0 || currentGroupSize < maxGroupSize)){
                    Map<V,Set<GroupData>> newCurrentGroups = new HashMap<>();
                    if(currentGroupSize == 1 || currentGroupSize > 6){
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;                                 

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);

                                    // check whether all n-1 subsets are in F_v^{k - 1}
                                    boolean checkFeasibility = true;
                                    //for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                    Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
                                    for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
					subset.remove(planComputationRequest);

                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                        subset.add(planComputationRequest);
                                    }

                                    if(checkFeasibility){
                                        end = false;
                                        Plan plan ;

                                        if(recordTime){
                                            groupCountPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                            plan = Benchmark.measureTime(() -> 
                                                    optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                            vehicle, newGroupToCheck, startTime, false));
                                            int timeInMs = Benchmark.getDurationMsInt();
                                            if(plan != null){
                                                groupCountsPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1);
                                                computationalTimesPlanExistPerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                            }
                                            computationalTimePerVehicle.get(vehicle).increment(newGroupToCheck.size() - 1, timeInMs);
                                        }
                                        else{
                                            plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                    vehicle, newGroupToCheck, startTime, false);
                                        }

                                        if(exportGroupData){
                                            saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
                                        }

                                        if(plan != null) {

                                            if(groupData.getOnboardRequestLock() == null || newGroupToCheck.containsAll(groupData.getOnboardRequestLock())){
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, vehicle));
                                                groupPlans.get(vehicle).add(plan);
                                            }
                                            else{
                                                newCurrentGroups.get(vehicle).add(new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle));
                                            }
                //							if(groups.size() > 50){
                //								return groups;
                //							}
                                        }
                                    }
                                }
                            }
                        }                       
                    }
                    else{
                        final Set<GroupData> groupsForNN = new LinkedHashSet<>();
                        
                        for (V vehicle : vehicles) {
                            // current groups for the next iteration
                            newCurrentGroups.put(vehicle, new LinkedHashSet<>());
                            
                            if(currentGroups.get(vehicle).isEmpty()) continue;

                            // set of groups that were already checked
                            Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
                                           
                            for (GroupData groupData : currentGroups.get(vehicle)) {
                                for (PlanComputationRequest request : feasibleRequests.get(vehicle)) {
                                    if (groupData.getRequests().contains(request)){
                                        continue;
                                    }

                                    // G'
                                    LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.getRequests());
                                    newGroupToCheck.add(request);

                                    if (currentCheckedGroups.contains(newGroupToCheck)){
                                        continue;
                                    }
                                    currentCheckedGroups.add(newGroupToCheck);
                                    boolean checkFeasibility = true;
                                    //for(Set<PlanComputationRequest> subset: getAllNMinus1Subsets(newGroupToCheck)){
                                    Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
                                    for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
					subset.remove(planComputationRequest);

                                        if(!currentGroups.get(vehicle).contains(new GroupData(subset))){
                                            checkFeasibility = false;
                                            break;
                                        }
                                        subset.add(planComputationRequest);
                                    }

                                    if(checkFeasibility){                   
                                        GroupData gd = new GroupData(newGroupToCheck, groupData.getOnboardRequestLock(), vehicle);
                                        groupsForNN.add(gd);
                                    }
                                }
                            }              
                        }
                        if(!groupsForNN.isEmpty()){
                            end = false;
                            final int h = currentGroupSize;
                            Benchmark.measureTime(() -> nn.setProbability(groupsForNN, h));
                            nn_time[currentGroupSize-2] += Benchmark.getDurationMsInt();
                        }   
                        for (GroupData newGroupToCheck : groupsForNN) {
                            if(newGroupToCheck.getFeasible() < 0.5){
                                false_count++;
                                continue;
                            }else{
                                true_count++;
                            }

                            Plan plan ;

                            if(recordTime){
                                groupCountPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                plan = Benchmark.measureTime(() -> 
                                        optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                                newGroupToCheck.getVehicle(), (LinkedHashSet) newGroupToCheck.getRequests(), startTime, false));
                                int timeInMs = Benchmark.getDurationMsInt();
                                if(plan != null){
                                    groupCountsPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1);
                                    computationalTimesPlanExistPerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                                }
                                computationalTimePerVehicle.get(newGroupToCheck.getVehicle()).increment(newGroupToCheck.getRequests().size() - 1, timeInMs);
                            }
                            else{
                                plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(
                                        newGroupToCheck.getVehicle(),(LinkedHashSet) newGroupToCheck.getRequests(), startTime, false);
                            }

                            if(exportGroupData){
                                saveGroupData((V)newGroupToCheck.getVehicle(), startTime,(LinkedHashSet) newGroupToCheck.getRequests(), plan != null);
                            }

                            if(plan != null) {

                                if(newGroupToCheck.getOnboardRequestLock() == null || newGroupToCheck.getRequests().containsAll(newGroupToCheck.getOnboardRequestLock())){
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getVehicle()));
                                    groupPlans.get(newGroupToCheck.getVehicle()).add(plan);
                                }
                                else{
                                    newCurrentGroups.get(newGroupToCheck.getVehicle()).add(new GroupData(newGroupToCheck.getRequests(), newGroupToCheck.getOnboardRequestLock(), newGroupToCheck.getVehicle()));
                                }
            //							if(groups.size() > 50){
            //								return groups;
            //							}
                            }
                        }
                    }
                    currentGroups = newCurrentGroups;
                    currentGroupSize++; 
                }
            }
        return groupPlans;
    }

 /*   private void export_to_csv(Set<GroupData> groupsForNN) {
        String[][] data = new String[groupsForNN.size()][4+6*(3)]; //groupSize 3 fixed
        int k = 0;
        for (GroupData newGroupToCheck : (Set<GroupData>) groupsForNN) {
            int j = 0;
            data[k][j++] = "true";
            data[k][j++] = Integer.toString(newGroupToCheck.getVehicle().getRequestsOnBoard().size());
            data[k][j++] = Integer.toString(newGroupToCheck.getVehicle().getPosition().getLatE6());
            data[k][j++] = Integer.toString(newGroupToCheck.getVehicle().getPosition().getLonE6());                      
            for (PlanComputationRequest rq : newGroupToCheck.getRequests()) {
                data[k][j++] = Integer.toString(rq.getFrom().getLatE6());
                data[k][j++] = Integer.toString(rq.getFrom().getLonE6());
                data[k][j++] = Integer.toString(rq.getMaxPickupTime());
                data[k][j++] = Integer.toString(rq.getTo().getLatE6());
                data[k][j++] = Integer.toString(rq.getTo().getLonE6());
                data[k][j++] = Integer.toString(rq.getMaxDropoffTime());
            }
            k++;
        }
        try {
			CsvWriter writer = new CsvWriter(
			Common.getFileWriter("C:\\Users\\matal\\Desktop\\exp\\data.csv", false));
            String s[] = new String[22];
            int p = 0;
            s[p++] = "feasible"; s[p++] = "onboard_count"; s[p++] = "lat"; s[p++] = "lon";
            for (int i = 1; i < 4; i++) {
                s[p++] = "pickup_" + i + "_lat";
                s[p++] = "pickup_" + i + "_lon";
                s[p++] = "pickup_" + i + "_maxtime";
                s[p++] = "dropoff_" + i + "_lat";
                s[p++] = "dropoff_" + i + "_lon";
                s[p++] = "dropoff_" + i + "_maxtime";
            }
			writer.writeLine(s);
            for (int i = 0; i < groupsForNN.size(); i++) {
               writer.writeLine(data[i]); 
            }
			writer.close();
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
    }*/
}
