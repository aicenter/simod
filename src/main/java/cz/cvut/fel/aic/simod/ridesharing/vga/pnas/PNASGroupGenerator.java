package cz.cvut.fel.aic.simod.ridesharing.vga.pnas;

///*
// * Copyright (C) 2021 Czech Technical University in Prague.
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
// * MA 02110-1301  USA
// */
//package cz.cvut.fel.aic.amodsim.ridesharing.vga.pnas;
//
//import com.google.inject.Provider;
//import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
//import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
//import cz.cvut.fel.aic.amodsim.config.SimodConfig;
//import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
//import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
//import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
//import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
//import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.InsertionHeuristicSingleVehicleDARPSolver;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.SingleVehicleDARPSolver;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Set;
//
///**
// *
// * @author Fido
// * @param <V>
// */
//public class PNASGroupGenerator<V extends IOptimalPlanVehicle> extends GroupGenerator<V>{
//	
//		
//	private final SingleVehicleDARPSolver heuristicVehiclePlanFinder;
//	
//	public PNASGroupGenerator(
//			SingleVehicleDARPSolver optimalVehiclePlanFinder, 
//			SimodConfig config, 
//			Provider<VehicleGroupAssignmentSolver> vgaSolverProvider, 
//			InsertionHeuristicSingleVehicleDARPSolver heuristicSingleVehicleDARPSolver) {
//		super(optimalVehiclePlanFinder, config, vgaSolverProvider);
//		this.heuristicVehiclePlanFinder = heuristicSingleVehicleDARPSolver;
//	}
//	
//	private class GroupPlan{
//		private final Plan plan;
//		
//		private final GroupData group;
//
//		public GroupPlan(Plan plan, GroupData group) {
//			this.plan = plan;
//			this.group = group;
//		}
//	}
//
//	@Override
//	public List<Plan> generateGroupsForVehicle(V vehicle, Collection<PlanComputationRequest> requests, int startTime) {
//		
//		/* STATISTICS */
//		long group_generation_start_time = System.nanoTime();
//		boolean stop = false;
//		
//		// statistics
//		if(recordTime){
//			int size = vehicle.getRequestsOnBoard().size() + 1;
//			groupCounts = new FlexArray(size);
//			groupCountsPlanExists = new FlexArray(size);
//			computationalTimes = new FlexArray(size);
//			computationalTimesPlanExists = new FlexArray(size);
//		}
//		
//		/* COLLECTION INIT */
//		// F_v^{k - 1} - groupes for request adding
//		Set<GroupData> currentGroups = new LinkedHashSet<>();
//		
//		// F_v^{1}
//		List<PlanComputationRequest> feasibleRequests = new ArrayList<>();
//		
//		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
//		List<GroupPlan> groupPlans = new ArrayList<>();
//
//		/* BASE PLAN GENERATION */
//		Set<PlanComputationRequest> onBoardRequestLock = null;
//		RideSharingOnDemandVehicle rVehicle = (RideSharingOnDemandVehicle) vehicle.getRealVehicle();
//		if(vehicle.getRequestsOnBoard().isEmpty()){
//			
//			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
//			Plan emptyPlan = new Plan((int) startTime, vehicle);
//			GroupData emptyGroup = new GroupData(new HashSet<>());
//			groupPlans.add(new GroupPlan(emptyPlan, emptyGroup));
//		}
//		if(rVehicle.getCurrentPlan().getLength() > 1){
//			// BASE PLAN - for each driving vehicle, we add a base plan the previously assigned plan
//			LinkedHashSet<PlanComputationRequest> onBoardRequests = vehicle.getRequestsOnBoard();
//			onBoardRequestLock = onBoardRequests;
//			DriverPlan currentPlan = rVehicle.getCurrentPlan();
//			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
//			for(PlanAction action: currentPlan.plan){
//				if(action instanceof PlanRequestAction){
//					group.add(((PlanRequestAction) action).getRequest());
//				}
//			}
//			
//			// currently, the time window has to be ignored, because the planner underestimates the cost
//			Plan initialPlan;
//			if(recordTime){
//				groupCounts.increment(group.size() - 1);
//				groupCountsPlanExists.increment(group.size() - 1);
//				Benchmark benchmark = new Benchmark();
//				initialPlan = benchmark.measureTime(() -> 
//						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true));
//				computationalTimes.increment(group.size() - 1, benchmark.getDurationMsInt());
//				computationalTimesPlanExists.increment(group.size() - 1, benchmark.getDurationMsInt());
//			}
//			else{
//				initialPlan 
//					= optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, true);
//			}
//			
//			groupPlans.add(new GroupPlan(initialPlan, new GroupData(group)));
//	
//			/* we have to add onboard request to feasible requests set and to current groups of size 1 */
//			for (PlanComputationRequest request : group) {
//				feasibleRequests.add(request);
//				Set<PlanComputationRequest> singleRequestGroup = new HashSet<>(1);
//				singleRequestGroup.add(request);
//				currentGroups.add(new GroupGenerator.GroupData(singleRequestGroup, onBoardRequestLock));
//			}
//		}
//		
//		// groups of size 1
//		for (PlanComputationRequest request : requests) {
//			LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>(1);
//			group.add(request);
//
//			Plan plan;
//			if(recordTime){
//				groupCounts.increment(group.size() - 1);
//				Benchmark benchmark = new Benchmark();
//				plan = benchmark.measureTime(() -> 
//						optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false));
//				int timeInMs = benchmark.getDurationMsInt();
//				if(plan != null){
//					groupCountsPlanExists.increment(group.size() - 1);
//					computationalTimesPlanExists.increment(group.size() - 1, timeInMs);
//				}			
//				computationalTimes.increment(group.size() - 1, timeInMs);
//			}
//			else{
//				plan = optimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, startTime, false);
//			}
//			
////			if(exportGroupData){
////				vgaSolverProvider.get().saveGroupData(vehicle, startTime, group, plan != null);
////			}
//
//			if(plan != null) {
//				feasibleRequests.add(request);
//				currentGroups.add(new GroupGenerator.GroupData(group, onBoardRequestLock));
//				
//				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
//				if(vehicle.getRequestsOnBoard().isEmpty()){
//					groupPlans.add(new GroupPlan(plan, new GroupData(group)));
//				}			
//			}
//		}
//		
//		
//		// generate other groups
//		int currentGroupSize = 1;
//		while(!currentGroups.isEmpty() && (maxGroupSize == 0 || currentGroupSize < maxGroupSize) && !stop) {
//
//			// current groups for the next iteration
//			Set<GroupGenerator.GroupData> newCurrentGroups = new LinkedHashSet<>();
//			
//			// set of groups that were already checked
//			Set<Set<PlanComputationRequest>> currentCheckedGroups = new LinkedHashSet<>();
//
//			for (GroupGenerator.GroupData groupData : currentGroups) {
//				for (PlanComputationRequest request : feasibleRequests) {
//					if (groupData.requests.contains(request)){
//						continue;
//					}
//					
//					// G'
//					LinkedHashSet<PlanComputationRequest> newGroupToCheck = new LinkedHashSet<>(groupData.requests);
//					newGroupToCheck.add(request);
//					
//					if (currentCheckedGroups.contains(newGroupToCheck)){
//						continue;
//					}
//					currentCheckedGroups.add(newGroupToCheck);
//					
//					// check whether all n-1 subsets are in F_v^{k - 1}
//					boolean checkFeasibility = true;
//					Set<PlanComputationRequest> subset = new HashSet<>(newGroupToCheck);
//					for (PlanComputationRequest planComputationRequest : newGroupToCheck) {
//						subset.remove(planComputationRequest);
//						if(!currentGroups.contains(new GroupGenerator.GroupData(subset, null))){
//							checkFeasibility = false;
//							break;
//						}
//						subset.add(planComputationRequest);
//					}
//					
//					if(checkFeasibility){
//
//						Plan plan ;
//						
//						if(groupGenerationTimeLimitInNanoseconds > 0){
//							long currentDuration = System.nanoTime() - group_generation_start_time;
//							if(currentDuration > groupGenerationTimeLimitInNanoseconds){
//								stop = true;
//								break;
//							}
//						}
//						
//						SingleVehicleDARPSolver solver;
//						if(maxGroupSize > 0 && currentGroupSize >= maxGroupSize){
//							solver = heuristicVehiclePlanFinder;
//						}
//						else{
//							solver = optimalVehiclePlanFinder;
//						}
//						
//						if(recordTime){
//							groupCounts.increment(newGroupToCheck.size() - 1);
//							Benchmark benchmark = new Benchmark();
//							plan = benchmark.measureTime(() -> 
//									solver.computeOptimalVehiclePlanForGroup(
//											vehicle, newGroupToCheck, startTime, false));
//							int timeInMs = benchmark.getDurationMsInt();
//							if(plan != null){
//								groupCountsPlanExists.increment(newGroupToCheck.size() - 1);
//								computationalTimesPlanExists.increment(newGroupToCheck.size() - 1, timeInMs);
//							}
//							computationalTimes.increment(newGroupToCheck.size() - 1, timeInMs);
//						}
//						else{
//							plan = so.computeOptimalVehiclePlanForGroup(
//									vehicle, newGroupToCheck, startTime, false);
//						}
//						
//						if(exportGroupData && currentGroupSize >= 3){
//							vgaSolverProvider.get().saveGroupData(vehicle, startTime, newGroupToCheck, plan != null);
//						}
//
//						if(plan != null) {
//			
//							if(groupData.onboardRequestLock == null || newGroupToCheck.containsAll(groupData.onboardRequestLock)){
//								newCurrentGroups.add(new GroupGenerator.GroupData(newGroupToCheck));
//								groupPlans.add(plan);
//							}
//							else{
//								newCurrentGroups.add(new GroupGenerator.GroupData(newGroupToCheck, groupData.onboardRequestLock));
//							}
//						}
//					}
//				}
//				if(stop){
//					break;
//				}
//			}
//
//			currentGroups = newCurrentGroups;
//			currentGroupSize++;
//		}
//		
//		return groupPlans;
//	}
//	
//	
//}
