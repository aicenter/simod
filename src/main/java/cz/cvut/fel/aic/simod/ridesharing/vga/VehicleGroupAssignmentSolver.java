/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.ridesharing.vga;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.CsvWriter;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.amodsim.SimodException;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.io.Common;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.GroupGenerator;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VGAVehicle;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VehiclePlanList;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.VirtualVehicle;
import cz.cvut.fel.aic.simod.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsVGA;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage.NearestType;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

@Singleton
public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VehicleGroupAssignmentSolver.class);
	
	private static final int MILLION = 1000000;
	
	private static final int GROUP_RECORDS_BATCH_SIZE = 10_000;
	
	
	private final LinkedHashSet<PlanComputationRequest> activeRequests;

	private final SimodConfig config;
	
	private final GurobiSolver gurobiSolver;
		
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

	private final TimeProvider timeProvider;
	
	private final Provider<GroupGenerator> groupGeneratorProvider;
	
	private final Boolean exportGroupData;
	
	private final boolean optimizeParkedVehicles;
	

	private List<VGAVehicle> vgaVehicles;
	
	private int startTime;
	
	private int planCount;
	
	private List<VehiclePlanList> feasiblePlans;
	
	private List<List<String>> logRecords;
	
	private int groupGenerationTime;
			
	private int solverTime;
	
	private int newRequestCount;
	
	private int insufficientCapacityCount;
	
	/**
	 * Counts for groups for which plan compuation was attempted. There is only one empty group, therefore, these 
	 * counts starts for group of size 1.
	 */
	private FlexArray groupCounts;
	
	/**
	 * Counts for groups for which plan exists. There is only one empty group, therefore, these 
	 * counts starts for group of size 1.
	 */
	private FlexArray groupCountsPlanExists;
	
	/**
	 * Computational times for groups for which plan computation was attempted. There is only one empty group, 
	 * therefore, these counts starts for group of size 1.
	 */
	private FlexArray computationalTimes;
	
	/**
	 * Computational times for groups for which plan exists. There is only one empty group, therefore, these counts 
	 * starts for group of size 1.
	 */
	private FlexArray computationalTimesPlanExists;
	
	private int[] usedVehiclesPerStation;
	
	private List<String[]> groupRecords;
	
	private CsvWriter groupRecordWriter = null;

	private ArrayList<ArrayList<PlanComputationRequest>> requestsPerVehicle;
	
	

	@Inject
	public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, SimodConfig config, TimeProvider timeProvider, 
			DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
			GurobiSolver gurobiSolver, Provider<GroupGenerator> groupGeneratorProvider,
			OnDemandvehicleStationStorage onDemandvehicleStationStorage) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.config = config;
		this.gurobiSolver = gurobiSolver;
		this.eventProcessor = eventProcessor;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.timeProvider = timeProvider;
		this.groupGeneratorProvider = groupGeneratorProvider;
		exportGroupData = config.ridesharing.vga.exportGroupData;
		optimizeParkedVehicles = config.ridesharing.vga.optimizeParkedVehicles;
		activeRequests = new LinkedHashSet<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
		MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
		
		if(exportGroupData){
			try {
				groupRecordWriter =  new CsvWriter(
						Common.getFileWriter(config.statistics.groupDataFilePath));
			} catch (IOException ex) {
				Logger.getLogger(GroupGenerator.class.getName()).log(Level.SEVERE, null, ex);
			}
			groupRecords = new ArrayList<>(GROUP_RECORDS_BATCH_SIZE);
		}
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
		
		// filter driving vehicles
		final List<VGAVehicle> drivingVehicles = optimizeParkedVehicles ? filterVehicles(vgaVehicles) : vgaVehicles;
		int usedVehiclesCount = optimizeParkedVehicles 
				? drivingVehicles.size() + 1 * waitingRequests.size() : drivingVehicles.size();

		// active requests update
		for (PlanComputationRequest newRequest : newRequests) {
			activeRequests.add(newRequest);
		}
		
		LOGGER.info("No. of active requests: {}", activeRequests.size());
		LOGGER.info("Number of vehicles used for planning: {}",  usedVehiclesCount);
		
		if(activeRequests.isEmpty()){
			return new LinkedHashMap<>();
		}


		/* Generating feasible plans for each vehicle */
		Benchmark benchmark = new Benchmark();
		benchmark.measureTime(() -> generateGroups(drivingVehicles, waitingRequests));	
		groupGenerationTime = benchmark.getDurationMsInt();
		
////		TO REMOVE TEST
//		LinkedHashSet<PlanComputationRequest> newRequestsHashSet = new LinkedHashSet<>(newRequests);
		
		
		/* Using an ILP solver to optimally assign a group to each vehicle */
		benchmark = new Benchmark();
		List<Plan<IOptimalPlanVehicle>> optimalPlans 
				= benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(
						feasiblePlans, activeRequests, usedVehiclesPerStation));
		
		// ILP solver generation total time 
		solverTime = benchmark.getDurationMsInt();

		/* Filling the output with converted plans */
		
		// for virtual vehicles
		Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
		
		Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();
		for(Plan<IOptimalPlanVehicle> plan : optimalPlans) {
			DriverPlan driverPlan = plan.toDriverPlan();
			
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

				RideSharingOnDemandVehicle onDemandVehicle = 
						(RideSharingOnDemandVehicle) nearestStation.getVehicle(index);
				planMap.put(onDemandVehicle, driverPlan);
				
				CollectionUtil.incrementMapValue(usedVehiclesPerStation, nearestStation, 1);
			}
		}
		
		if(config.ridesharing.vga.logPlanComputationalTime){
			logRecords();
		}
		
		// check if all driving vehicles have a plan
//		checkPlanMapComplete(planMap, optimalPlans);
		
		return planMap;
	}

	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
		if(event.getType() instanceof OnDemandVehicleEvent){
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
		else if(event.isType(DemandEvent.LEFT)){
			PlanComputationRequest request = (PlanComputationRequest) event.getContent();
			activeRequests.remove(request);
		}
	}
	
	public synchronized void saveGroupData(IOptimalPlanVehicle vehicle, int startTime, LinkedHashSet<PlanComputationRequest> group, 
			boolean feasible) {
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
		}
	}
	
	private synchronized void exportGroupData() {
		try {
			for (String[] groupRecord : groupRecords) {
				groupRecordWriter.writeLine(groupRecord);
			}
			groupRecordWriter.flush();
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
		finally{
			groupRecords = new ArrayList<>(GROUP_RECORDS_BATCH_SIZE);
		}
	}

	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		typesToHandle.add(DemandEvent.LEFT);
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

	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, Collection<PlanComputationRequest> waitingRequests) {
		Benchmark benchmark = new Benchmark();
		GroupGenerator groupGenerator = groupGeneratorProvider.get();
		List<Plan> feasibleGroupPlans = benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, startTime));
		
		// log
		if(config.ridesharing.vga.logPlanComputationalTime){
			logPlansPerVehicle(groupGenerator, vehicle, feasibleGroupPlans, benchmark.durationNano);
		}
		
		return feasibleGroupPlans;
	}

	private void printGroupStats(List<VehiclePlanList> feasiblePlans) {
		Map<Integer,Integer> stats = new LinkedHashMap();
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

	private synchronized void logPlansPerVehicle(GroupGenerator groupGenerator, VGAVehicle vehicle, 
			List<Plan> feasibleGroupPlans, long totalTimeNano) {
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
		
		int maxActionCount = (groupCountsPlanExists.size() + 1) * 2;
		int[] counts = new int[maxActionCount + 1]; // counts of plans with a specific number of actions
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
		GroupSizeData[] groupSizeDataForAllGroupSizesPlanExists = new GroupSizeData[groupCountsPlanExists.size()];
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

	private void checkPlanMapComplete(Map<RideSharingOnDemandVehicle, DriverPlan> planMap, 
			List<Plan<IOptimalPlanVehicle>> optimalPlans) {
		for(OnDemandVehicle onDemandVehicle: vehicleStorage){
			if(onDemandVehicle.getState() != OnDemandVehicleState.WAITING 
			&& onDemandVehicle.getState() != OnDemandVehicleState.REBALANCING){
				if(!planMap.containsKey(onDemandVehicle)){
					try {
						throw new SimodException("Driving vehicle is not replanned:" + onDemandVehicle);
					} catch (Exception ex) {
						Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
						VGAVehicle vGAVehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(onDemandVehicle.getId());
						List<Plan> feasiblePlansForVehicle = null;
						for(VehiclePlanList vpl: feasiblePlans){
							if(vpl.optimalPlanVehicle == vGAVehicle){
								feasiblePlansForVehicle = vpl.feasibleGroupPlans;
								break;
							}
						}
						if(feasiblePlansForVehicle != null){
							LOGGER.debug("Vehicle has {} feasible plans", feasiblePlansForVehicle.size());
							
							Plan optimalPlan = null;
							for(Plan p: optimalPlans){
								if(p.getVehicle() == vGAVehicle){
									optimalPlan = p;
								}
							}
							if(optimalPlan != null){
								LOGGER.debug("The optimal plan is: ", optimalPlan);
							}
							else{
								LOGGER.debug("There is no optimal plan for vehicle!");
							}
						}
						else{
							LOGGER.debug("Vehicle has no feasible plans!");
						}
						boolean inOptimalPlans = false;
								
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
		
		if(config.ridesharing.vga.maxVehiclesPerRequest > 0){
			generatePossibleVehicleRequestCombinations(waitingRequests, drivingVehicles);
		}
		
//		// global groups
//		LOGGER.info("Generating global groups");
//		Set<Set<PlanComputationRequest>> globalFeasibleGroups 
//				= vGAGroupGenerator.generateGlobalGroups(waitingRequests, startTime);
//		LOGGER.info("{} global groups generated", globalFeasibleGroups.size());
		
		// groups for driving vehicls
		ProgressBar.wrap(drivingVehicles.stream().parallel(), "Generating groups for driving vehicles")
				.forEach(vehicle -> computeGroupForDrivingVehicle(vehicle, waitingRequests));
		
		// groups for vehicles in the station
		if(!onDemandvehicleStationStorage.isEmpty()){ // or config.stations.on
			usedVehiclesPerStation = new int[onDemandvehicleStationStorage.size()];
			insufficientCapacityCount = 0;
			
			// dictionary - all vehicles from a station have the same feasible groups
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation = new HashMap<>();
			
			ProgressBar.wrap(waitingRequests.stream().parallel(), "Generating groups for vehicles in station")
				.forEach(request -> computeGroupForVehicleInStation(request, usedVehiclesPerStation, 
						plansFromStation, waitingRequests));

			if(optimizeParkedVehicles){
				// generating virtual vehicle plans
				for (OnDemandVehicleStation station: onDemandvehicleStationStorage) {
					Integer usedVehiclesCount = usedVehiclesPerStation[station.getIndex()];
					if(usedVehiclesCount > 0){
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
				}
			}
			if(insufficientCapacityCount > 0){
				LOGGER.info("{} requests won't be served from station due to insufficient capacity",
						insufficientCapacityCount);
			}
		}
		
		LOGGER.info("{} groups generated", planCount);
                printGroupStats(feasiblePlans);
		
		if(exportGroupData){
			exportGroupData();
		}
	}
	
	private void computeGroupForDrivingVehicle(VGAVehicle vehicle, List<PlanComputationRequest> waitingRequests){
		
		if(config.ridesharing.vga.maxVehiclesPerRequest > 0){
			waitingRequests = requestsPerVehicle.get(vehicle.getRidesharingVehicle().getIndex());
		}

		List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vehicle, waitingRequests);
		VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
		updatePlans(vehiclePlanList, feasibleGroupPlans);
	}
	
	public void computeGroupForVehicleInStation(PlanComputationRequest request, 
			int[] usedVehiclesPerStation,
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation, List<PlanComputationRequest> waitingRequests){
		OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(
						request.getFrom(), NearestType.TRAVELTIME_FROM);
				
		// add all feasible plans from station if not computed yet
		if(checkIfComputePlansFromStation(usedVehiclesPerStation, nearestStation, plansFromStation) && optimizeParkedVehicles){
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
	
	private synchronized boolean checkIfComputePlansFromStation(int[] usedVehiclesPerStation, 
			OnDemandVehicleStation nearestStation, Map<OnDemandVehicleStation,List<Plan>> plansFromStation){
		
		
		int index = usedVehiclesPerStation[nearestStation.getIndex()];
		
		// check if there is not a lack of vehicles in the station
		if(index == nearestStation.getParkedVehiclesCount()){
			insufficientCapacityCount++;
			return false;
		}
		
		usedVehiclesPerStation[nearestStation.getIndex()]++;
		
		// check if plan is not already computed
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

	private void generatePossibleVehicleRequestCombinations(
			List<PlanComputationRequest> waitingRequests, 
			List<VGAVehicle> drivingVehicles) {
		
		requestsPerVehicle = new ArrayList<>();
		for (int i = 0; i < vgaVehicles.size(); i++) {
			requestsPerVehicle.add(new ArrayList<>());
		}
		String message = String.format("Searching for %s nearest vehicles for each request", 
						config.ridesharing.vga.maxVehiclesPerRequest);
		for(PlanComputationRequest request: ProgressBar.wrap(waitingRequests, message)){
			drivingVehicles.sort((VGAVehicle vehicle1, VGAVehicle vehicle2) -> {
				long dist1 = travelTimeProvider.getTravelTime(
						vehicle1.getRealVehicle(), request.getPickUpAction().getPosition());
				
				long dist2 = travelTimeProvider.getTravelTime(
						vehicle2.getRealVehicle(), request.getPickUpAction().getPosition());
				
				if(dist1 < dist2){
					return -1;
				};
				if(dist1 > dist2){
					return 1;
				}
				return 0;
			});
			long oldDist = Long.MAX_VALUE;
			for (int i = 0; i < config.ridesharing.vga.maxVehiclesPerRequest && i < drivingVehicles.size(); i++) {
				VGAVehicle vGAVehicle = drivingVehicles.get(i);
				requestsPerVehicle.get(vGAVehicle.getRidesharingVehicle().getIndex()).add(request);
//				long dist = travelTimeProvider.getTravelTime(
//						vGAVehicle.getRealVehicle(), request.getPickUpAction().getPosition());
//				if(dist != oldDist){
//					oldDist = dist;
//					i++;
//				}
			}
		}
	}
}
