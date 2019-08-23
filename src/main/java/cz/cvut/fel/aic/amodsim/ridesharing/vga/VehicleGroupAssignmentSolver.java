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
package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.io.Common;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import java.io.IOException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.PlanActionCurrentPosition;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsVGA;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage.NearestType;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VehicleGroupAssignmentSolver.class);
	
	private static final int MILLION = 1000000;
	
	
	private final LinkedHashSet<PlanComputationRequest> activeRequests;

	private final AmodsimConfig config;
	
	private final GroupGenerator groupGenerator;
	
	private final GurobiSolver gurobiSolver;
	

	
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

	private final TimeProvider timeProvider;
	
	
	private List<VGAVehicle> vgaVehicles;
	
	private int startTime;
	
	private int planCount;
	
	private List<VehiclePlanList> feasiblePlans;
	
	private List<List<String>> logRecords;
	
	private int groupGenerationTime;
			
	private int solverTime;
	
	private int newRequestCount;
	
	private int insufficientCapacityCount;
	
	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;

	
	

	@Inject
	public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, 
			TimeProvider timeProvider, GroupGenerator vGAGroupGenerator, 
			DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
			GurobiSolver gurobiSolver, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.config = config;
		this.groupGenerator = vGAGroupGenerator;
		this.gurobiSolver = gurobiSolver;
		this.eventProcessor = eventProcessor;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.timeProvider = timeProvider;
		activeRequests = new LinkedHashSet<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
		MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, 
			List<PlanComputationRequest> waitingRequests) {
		
		// statistic vars
		newRequestCount = newRequests.size();
		if(config.ridesharing.vga.logPlanComputationalTime){
			groupCounts = new FlexArray();
			groupCountsPlanExists =new  FlexArray();
			computationalTimes = new FlexArray();
			computationalTimesPlanExists = new FlexArray();
		}
		
		logRecords = new ArrayList<>();
		
		//init VGA vehicles	
		if(vgaVehicles == null){
			initVehicles();
		}

		LOGGER.info("Total vehicle count: " + vgaVehicles.size());
		List<VGAVehicle> drivingVehicles = filterVehicles(vgaVehicles);

		// active requests update
		for (PlanComputationRequest newRequest : newRequests) {
			activeRequests.add(newRequest);
		}
		
		LOGGER.info("No. of active requests: {}", activeRequests.size());
		LOGGER.info("Number of vehicles used for planning: {}", drivingVehicles.size() + 1 * waitingRequests.size());


		/* Generating feasible plans for each vehicle */
		Benchmark benchmark = new Benchmark();
		benchmark.measureTime(() -> generateGroups(drivingVehicles, waitingRequests));	
		benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests));
		groupGenerationTime = benchmark.getDurationMsInt();
		
////		TO REMOVE TEST
//		LinkedHashSet<PlanComputationRequest> newRequestsHashSet = new LinkedHashSet<>(newRequests);
		
		
		/* Using an ILP solver to optimally assign a group to each vehicle */
		benchmark = new Benchmark();
		List<Plan<IOptimalPlanVehicle>> optimalPlans 
				= benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests));
		
		// ILP solver generation total time 
		solverTime = benchmark.getDurationMsInt();

		/* Filling the output with converted plans */
		
		// for virtual vehicles
		Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
		
		Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();
		for(Plan<IOptimalPlanVehicle> plan : optimalPlans) {
			DriverPlan driverPlan = toDriverPlan(plan);
			
			// normal vehicles (driving vehicles)
			if(plan.getVehicle() instanceof VGAVehicle){
				VGAVehicle vGAVehicle = (VGAVehicle) plan.getVehicle();
				planMap.put(vGAVehicle.getRidesharingVehicle(), driverPlan);
			}
			
			// virtual vehicles
			else{
				VirtualVehicle virtualVehicle = (VirtualVehicle) plan.getVehicle();

				OnDemandVehicleStation nearestStation = virtualVehicle.getStation();
				int indexFromEnd = usedVehiclesPerStation.containsKey(nearestStation) 
						? usedVehiclesPerStation.get(nearestStation) : 0;
				int index = nearestStation.getParkedVehiclesCount() - 1 - indexFromEnd;

				OnDemandVehicle onDemandVehicle = nearestStation.getVehicle(index);
				VGAVehicle vGAVehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(onDemandVehicle.getId());
				planMap.put(vGAVehicle.getRidesharingVehicle(), driverPlan);
				
				CollectionUtil.incrementMapValue(usedVehiclesPerStation, nearestStation, 1);
			}
		}

		VGAVehicle.resetMapping();
		
		if(config.ridesharing.vga.logPlanComputationalTime){
			logRecords();
		}
		
		// check if all driving vehicles have a plan
//		checkPlanMapComplete(planMap);
		
		return planMap;
	}

	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
		OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
		OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
		PlanComputationRequest request = ridesharingDispatcher.getRequest(eventContent.getDemandId());
		VGAVehicle vehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(eventContent.getOnDemandVehicleId());
		if(eventType == OnDemandVehicleEvent.PICKUP){
			vehicle.addRequestOnBoard(request);
		}
		else if(eventType == OnDemandVehicleEvent.DROP_OFF){
			vehicle.removeRequestOnBoard(request);
			activeRequests.remove(request);
		}
	}
	
	

	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		eventProcessor.addEventHandler(this, typesToHandle);
	}

	private void initVehicles() {
		vgaVehicles = new LinkedList<>();
		int i = 0;
		for(OnDemandVehicle onDemandVehicle: vehicleStorage){
			VGAVehicle newVGAVehicle = VGAVehicle.newInstance((RideSharingOnDemandVehicle) onDemandVehicle);
			vgaVehicles.add(newVGAVehicle);
			vgaVehiclesMapBydemandOnDemandVehicles.put(onDemandVehicle.getId(), newVGAVehicle);
		}
	}

	private List<VGAVehicle> filterVehicles(List<VGAVehicle> vehiclesForPlanning) {
		List<VGAVehicle> filteredVehiclesForPlanning = new LinkedList<>();
		for (VGAVehicle vGAVehicle : vehiclesForPlanning) {
			OnDemandVehicle vehicle = vGAVehicle.getRidesharingVehicle();
			OnDemandVehicleStation parkedIn = vehicle.getParkedIn();
			if(parkedIn == null && vehicle.getState() != OnDemandVehicleState.REBALANCING){
				filteredVehiclesForPlanning.add(vGAVehicle);
			}
		}
		return filteredVehiclesForPlanning;
	}

	private DriverPlan toDriverPlan(Plan<IOptimalPlanVehicle> plan) {
		List<PlanAction> tasks = new ArrayList<>(plan.getActions().size() + 1);
		tasks.add(new PlanActionCurrentPosition(plan.getVehicle().getPosition()));
		for(PlanRequestAction action: plan.getActions()){
			tasks.add(action);
		}
		DriverPlan driverPlan = new DriverPlan(tasks, plan.getEndTime() - plan.getStartTime(), plan.getCost());
		
		return driverPlan;
	}

	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, Collection<PlanComputationRequest> waitingRequests) {
		Benchmark benchmark = new Benchmark();
		List<Plan> feasibleGroupPlans = benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, startTime));
		
		// log
		if(config.ridesharing.vga.logPlanComputationalTime){
			logPlansPerVehicle(vehicle, feasibleGroupPlans, benchmark.durationNano);
		}
		
		return feasibleGroupPlans;
	}

	private void printGroupStats(List<VehiclePlanList> feasiblePlans) {
		Map<Integer,Integer> stats = new HashMap();
		for (VehiclePlanList feasiblePlan : feasiblePlans) {
			for (Plan feasibleGroupPlan : feasiblePlan.feasibleGroupPlans) {
				int size = Math.round(feasibleGroupPlan.getActions().size() / 2);
				CollectionUtil.incrementMapValue(stats, size, 1);
			}
		}
		
		for (Map.Entry<Integer, Integer> entry : stats.entrySet()) {
			Integer size = entry.getKey();
			Integer count = entry.getValue();
			LOGGER.info("{} groups of size {}", count, size);
		}
	}

	private synchronized void logPlansPerVehicle(VGAVehicle vehicle, List<Plan> feasibleGroupPlans, long totalTimeNano) {
		// group generator statistic addition
		groupCounts.addArrayInPlace(groupGenerator.getGroupCounts());
		groupCountsPlanExists.addArrayInPlace(groupGenerator.getGroupCountsPlanExists());
		computationalTimes.addArrayInPlace(groupGenerator.getComputationalTimes());
		computationalTimesPlanExists.addArrayInPlace(groupGenerator.getComputationalTimesPlanExists());	
		
		List<String> record = new ArrayList<>(5);
		record.add(Integer.toString(startTime));
		record.add(vehicle.getRidesharingVehicle().getId());
		record.add(Long.toString(Math.round(totalTimeNano / MILLION)));
		record.add(Integer.toString(vehicle.getRequestsOnBoard().size()));
		
		int maxActionCount = groupCountsPlanExists.size() * 2 + 1;
		int[] counts = new int[maxActionCount];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
			
		}
		for (Plan feasibleGroupPlan : feasibleGroupPlans) {
			counts[feasibleGroupPlan.getActions().size()]++;
			
			// global group stats
			int groupSize = feasibleGroupPlan.getActions().size() / 2;
		}
		for (int i = 0; i < counts.length; i++) {
			record.add(Integer.toString(counts[i]));
			
		}
		logRecords.add(record);
	}

	private void logRecords() {
		
		GroupSizeData[] groupSizeDataForAllGroupSizes = new GroupSizeData[groupCounts.size()];
		GroupSizeData[] groupSizeDataForAllGroupSizesPlanExists = new GroupSizeData[groupCounts.size()];
		for(int i = 0; i < groupCounts.size(); i++){
			groupSizeDataForAllGroupSizes[i] = new GroupSizeData(computationalTimes.get(i), groupCounts.get(i));
		}
		for(int i = 0; i < groupCountsPlanExists.size(); i++){
			groupSizeDataForAllGroupSizesPlanExists[i] = new GroupSizeData(
					computationalTimesPlanExists.get(i), groupCountsPlanExists.get(i));
		}
		
		// batch records
		RidesharingBatchStatsVGA batchStatsVGA = new RidesharingBatchStatsVGA(activeRequests.size(), 
				groupGenerationTime, solverTime, gurobiSolver.getGap(), groupSizeDataForAllGroupSizes, 
				groupSizeDataForAllGroupSizesPlanExists, newRequestCount);
		ridesharingStats.add(batchStatsVGA);
		
		// per vehicle records
		try {
			CsvWriter writer = new CsvWriter(
					Common.getFileWriter(config.ridesharing.vga.groupGeneratorLogFilepath, true));
			for (List<String> record : logRecords) {
				writer.writeLine(record.toArray(new String[0]));
			}
			writer.close();
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}

	private void checkPlanMapComplete(Map<RideSharingOnDemandVehicle, DriverPlan> planMap) {
		for(OnDemandVehicle onDemandVehicle: vehicleStorage){
			if(onDemandVehicle.getState() != OnDemandVehicleState.WAITING){
				if(!planMap.containsKey(onDemandVehicle)){
					try {
						throw new Exception("Driving vehicle is not replanned:" + onDemandVehicle);
					} catch (Exception ex) {
						Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}
	}

	private void generateGroups(List<VGAVehicle> drivingVehicles, List<PlanComputationRequest> waitingRequests) {
		feasiblePlans = new ArrayList<>(drivingVehicles.size());
		startTime = (int) Math.round(timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("Generating groups for vehicles.");
		planCount = 0;
		
//		// global groups
//		LOGGER.info("Generating global groups");
//		Set<Set<PlanComputationRequest>> globalFeasibleGroups 
//				= vGAGroupGenerator.generateGlobalGroups(waitingRequests, startTime);
//		LOGGER.info("{} global groups generated", globalFeasibleGroups.size());
		
		// groups for driving vehicls
		ProgressBar.wrap(drivingVehicles.stream().parallel(), "Generating groups for driving vehicles")
				.forEach(vehicle -> computeGroupForDrivingVehicle(vehicle, waitingRequests));
		
		// groups for vehicles in the station
		if(!onDemandvehicleStationStorage.isEmpty()){
			Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
			insufficientCapacityCount = 0;
			
			// dictionary - all vehicles from a station have the same feasible groups
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation = new HashMap<>();
			
			ProgressBar.wrap(waitingRequests.stream().parallel(), "Generating groups for vehicles in station")
				.forEach(request -> computeGroupForVehicleInStation(request, usedVehiclesPerStation, 
						plansFromStation, waitingRequests));

			
			// generating virtual vehicle plans
			for (Map.Entry<OnDemandVehicleStation, Integer> entry : usedVehiclesPerStation.entrySet()) {
				OnDemandVehicleStation station = entry.getKey();
				Integer usedVehiclesCount = entry.getValue();

				List<Plan> feasibleGroupPlansFromStation = plansFromStation.get(station);
				int capacity = feasibleGroupPlansFromStation.get(0).getVehicle().getCapacity();

				VirtualVehicle virtualVehicle = new VirtualVehicle(station, capacity, usedVehiclesCount);

				List<Plan> feasibleGroupPlans = new ArrayList<>(feasibleGroupPlansFromStation.size());
				for (Plan feasibleGroupPlan : feasibleGroupPlansFromStation) {
					feasibleGroupPlans.add(feasibleGroupPlan.duplicateForVehicle(virtualVehicle));
				}

				VehiclePlanList vehiclePlanList = new VehiclePlanList(virtualVehicle, feasibleGroupPlans);
				feasiblePlans.add(vehiclePlanList);
				planCount += feasibleGroupPlans.size();
			}
			
			if(insufficientCapacityCount > 0){
				LOGGER.info("{} request won't be served from station due to insufficient capacity",
						insufficientCapacityCount);
			}
		}
		
		LOGGER.info("{} groups generated", planCount);
		if(true){
			printGroupStats(feasiblePlans);
		}
	}
	
	private void computeGroupForDrivingVehicle(VGAVehicle vehicle, List<PlanComputationRequest> waitingRequests){
		List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vehicle, waitingRequests);
		VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
		updatePlans(vehiclePlanList, feasibleGroupPlans);
	
//			feasiblePlans.add(vehiclePlanList);
//			planCount += feasibleGroupPlans.size();
	}
	
	public void computeGroupForVehicleInStation(PlanComputationRequest request, 
			Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation,
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation, List<PlanComputationRequest> waitingRequests){
		OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(
						request.getFrom(), NearestType.TRAVELTIME_FROM);
				
		// add all feasible plans from station if not computed yet
		if(checkIfComputePlansFromStation(usedVehiclesPerStation, nearestStation, plansFromStation)){
			OnDemandVehicle onDemandVehicle = nearestStation.getVehicle(0);
			VGAVehicle vGAVehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(onDemandVehicle.getId());

			// all waiting request can be assigned to the waiting vehicle
			List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vGAVehicle, waitingRequests);
//						groupGenerator.generateGroupsForVehicle(vGAVehicle, waitingRequests, startTime);
			updateGroupPlans(plansFromStation, nearestStation, feasibleGroupPlans);
		}
	}
	
	private synchronized void updatePlans(VehiclePlanList vehiclePlanList, List<Plan> feasibleGroupPlans){
		feasiblePlans.add(vehiclePlanList);
		planCount += feasibleGroupPlans.size();
	}
	
	private synchronized boolean checkIfComputePlansFromStation(Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation, 
			OnDemandVehicleStation nearestStation, Map<OnDemandVehicleStation,List<Plan>> plansFromStation){
		
		// check if there is not a lack of vehicles in the station
		int index = usedVehiclesPerStation.containsKey(nearestStation) 
				? usedVehiclesPerStation.get(nearestStation) : 0;
		
		CollectionUtil.incrementMapValue(usedVehiclesPerStation, nearestStation, 1);
		
		if(index >= nearestStation.getParkedVehiclesCount()){
			insufficientCapacityCount++;
			return false;
		}
		
		// check if plan is not alreadz computed
		if(plansFromStation.containsKey(nearestStation)){
			return false;
		}
		
		plansFromStation.put(nearestStation, null);
		return true;
	}
	
	private synchronized void updateGroupPlans(Map<OnDemandVehicleStation,List<Plan>> plansFromStation, 
			OnDemandVehicleStation nearestStation, List<Plan> feasibleGroupPlans){
		plansFromStation.put(nearestStation, feasibleGroupPlans);
	}
}
