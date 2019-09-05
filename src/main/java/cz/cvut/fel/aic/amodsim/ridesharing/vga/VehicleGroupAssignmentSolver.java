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
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGeneratorv2;
import cz.cvut.fel.aic.amodsim.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsVGA;

@Singleton
public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VehicleGroupAssignmentSolver.class);
	
	private static final int MILLION = 1000000;
	
	
	private final LinkedHashSet<PlanComputationRequest> activeRequests;

	private final AmodsimConfig config;
	
	private final GroupGeneratorv2 groupGenerator;
	
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
	
	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;

	

	@Inject
	public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, 
			TimeProvider timeProvider, GroupGeneratorv2 vGAGroupGenerator, 
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
        groupGenerator.false_count = 0;
        groupGenerator.true_count = 0;
        for (int i = 0; i < groupGenerator.nn_time.length; i++) {
            groupGenerator.nn_time[i] = 0;
        }
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
		Benchmark.measureTime(() -> generateGroups(drivingVehicles, waitingRequests));	
		groupGenerationTime = Benchmark.getDurationMsInt();
		
		
		/* Using an ILP solver to optimally assign a group to each vehicle */
                //feasiblePlans.sort(new MySort());
		List<Plan<IOptimalPlanVehicle>> optimalPlans 
				= Benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests));
		
		// ILP solver generation total time 
		solverTime = Benchmark.getDurationMsInt();

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
		checkPlanMapComplete(planMap);
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

	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, Iterable<PlanComputationRequest> waitingRequests) {
		List<Plan> feasibleGroupPlans = Benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicleNN(vehicle, waitingRequests, startTime));
		
		// log
		if(config.ridesharing.vga.logPlanComputationalTime){
			logPlansPerVehicle(vehicle, feasibleGroupPlans, Benchmark.durationNano);
		}
		
		return feasibleGroupPlans;
	}
	private LinkedHashMap<VGAVehicle,List<Plan>> computeGroupsForVehicles(List<VGAVehicle> vehicles, 
			Collection<PlanComputationRequest> waitingRequests) {
		LinkedHashMap<VGAVehicle,List<Plan>> feasibleGroupPlans = Benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicleClean(vehicles, waitingRequests, startTime));
		
		// log
        /*if(config.ridesharing.vga.logPlanComputationalTime){
            for (Map.Entry<VGAVehicle,List<Plan>> entry : feasibleGroupPlans.entrySet()) {
                logPlansPerVehicle(entry.getKey(), entry.getValue(), Benchmark.durationNano);
            }
        }*/
                if(config.ridesharing.vga.logPlanComputationalTime && !vehicles.isEmpty()){
                    logPlansPerVehicles(vehicles, feasibleGroupPlans, Benchmark.durationNano/vehicles.size());
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
	private void logPlansPerVehicles(List<VGAVehicle> vehicles, Map<VGAVehicle,List<Plan>> feasibleGroupPlans, long totalTimeNano) {
                Map<VGAVehicle, FlexArray> groupCount = groupGenerator.getGroupCountPerVehicle();
                Map<VGAVehicle, FlexArray> groupCountsPlanExist = groupGenerator.getGroupCountsPlanExistPerVehicle();
                Map<VGAVehicle, FlexArray> computationalTime = groupGenerator.getComputationalTimePerVehicle();
                Map<VGAVehicle, FlexArray> computationalTimesPlanExist = groupGenerator.getComputationalTimesPlanExistPerVehicle();
                for (VGAVehicle vehicle : vehicles){
                    // group generator statistic addition
                    groupCounts.addArrayInPlace(groupCount.get(vehicle));
                    groupCountsPlanExists.addArrayInPlace(groupCountsPlanExist.get(vehicle));
                    computationalTimes.addArrayInPlace(computationalTime.get(vehicle));
                    computationalTimesPlanExists.addArrayInPlace(computationalTimesPlanExist.get(vehicle));	

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
                    for (Plan feasibleGroupPlan : feasibleGroupPlans.get(vehicle)) {
                            counts[feasibleGroupPlan.getActions().size()]++;

                            // global group stats
                            int groupSize = feasibleGroupPlan.getActions().size() / 2;
                    }
                    for (int i = 0; i < counts.length; i++) {
                            record.add(Integer.toString(counts[i]));

                    }
                    logRecords.add(record);
                }
	}
	private void logPlansPerVehicle(VGAVehicle vehicle, List<Plan> feasibleGroupPlans, long totalTimeNano) {
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
        // log number of feasible and infeasible groups and computation time of NN
		try {
            CsvWriter writer;
            if(ridesharingStats.size() == 1){
			writer = new CsvWriter(
					Common.getFileWriter(config.amodsimExperimentDir + "/NN_logs.csv", false));                
            }
            else{
			writer = new CsvWriter(
					Common.getFileWriter(config.amodsimExperimentDir + "/NN_logs.csv", true));                
            }
            int nn_total = groupGenerator.nn_time[0] + groupGenerator.nn_time[1] +
                    groupGenerator.nn_time[2] + groupGenerator.nn_time[3] +
                    groupGenerator.nn_time[4];
            int group_total = 0;
            List<String> writerLine = new ArrayList<>();
            writerLine.add(Integer.toString(groupGenerator.false_count));
            writerLine.add(Integer.toString(groupGenerator.true_count));
            for(int i = 0; i < 8; i++){
                if(groupCounts.size() <= i){
                    writerLine.add("0");
                }else{
                    group_total += computationalTimes.get(i);
                    writerLine.add(Integer.toString(computationalTimes.get(i)));
                }
            }
            for (int i = 0; i < 5; i++) {
                writerLine.add(Integer.toString(groupGenerator.nn_time[i]));
            }
            writerLine.add(Integer.toString(nn_total));
            writerLine.add(Integer.toString(group_total));
            writerLine.add(Integer.toString(groupGenerationTime - nn_total - group_total));
            writerLine.add(Integer.toString(groupGenerationTime));
            
            /*writer.writeLine(new String[] {Integer.toString(groupGenerator.false_count),
                Integer.toString(groupGenerator.true_count),
                Integer.toString(nn_total),
                Integer.toString(groupGenerationTime - nn_total),
            Integer.toString(groupGenerator.nn_time[0]),
            Integer.toString(groupGenerator.nn_time[1]),
            Integer.toString(groupGenerator.nn_time[2]),
            Integer.toString(groupGenerator.nn_time[3]),
            Integer.toString(groupGenerator.nn_time[4])});*/
            writer.writeLine(writerLine.toArray(new String[writerLine.size()]));
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
		/*for (VGAVehicle vehicle : ProgressBar.wrap(drivingVehicles, "Generating groups for driving vehicles")) {
			List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vehicle, waitingRequests);
			VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
			feasiblePlans.add(vehiclePlanList);
			planCount += feasibleGroupPlans.size();
		}*/
		LinkedHashMap<VGAVehicle,List<Plan>> groupPlans = computeGroupsForVehicles(drivingVehicles, waitingRequests);
                for (Map.Entry<VGAVehicle,List<Plan>> entry : groupPlans.entrySet()) {
                    feasiblePlans.add(new VehiclePlanList(entry.getKey(), entry.getValue()));
                    planCount += entry.getValue().size();
                }			
            
					
		// groups for vehicles in the station
		if(!onDemandvehicleStationStorage.isEmpty()){
			Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
			int insufficientCacityCount = 0;
			
			// dictionary - all vehicles from a station have the same feasible groups
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation = new HashMap<>();
			List<VGAVehicle> vehicles = new ArrayList<>();
                        List<OnDemandVehicleStation> stations = new ArrayList<>();
			for(PlanComputationRequest request: waitingRequests){
				OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(request.getFrom());
				
				// check if there is not a lack of vehicles in the station
				int index = usedVehiclesPerStation.containsKey(nearestStation) 
						? usedVehiclesPerStation.get(nearestStation) : 0;
				if(index >= nearestStation.getParkedVehiclesCount()){
					insufficientCacityCount++;
					continue;
				}

				// add all feasible plans from station if not computed yet
				if(!plansFromStation.containsKey(nearestStation)){
					OnDemandVehicle onDemandVehicle = nearestStation.getVehicle(0);
					VGAVehicle vGAVehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(onDemandVehicle.getId());
					vehicles.add(vGAVehicle);
                                        stations.add(nearestStation);
					// all waiting request can be assigned to the waiting vehicle
					//List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vGAVehicle, waitingRequests);
//						groupGenerator.generateGroupsForVehicle(vGAVehicle, waitingRequests, startTime);
					plansFromStation.put(nearestStation, null);
				}

				CollectionUtil.incrementMapValue(usedVehiclesPerStation, nearestStation, 1);
			} 
                       LinkedHashMap<VGAVehicle,List<Plan>> carPlans = computeGroupsForVehicles(vehicles, waitingRequests);
                        for (int i = 0; i < stations.size(); i++) {                     
                            plansFromStation.replace(stations.get(i), carPlans.get(vehicles.get(i)));                        
                        }
			
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
			
			if(insufficientCacityCount > 0){
				LOGGER.info("{} request won't be served from station due to insufficient capacity",
						insufficientCacityCount);
			}
		}
		
		LOGGER.info("{} groups generated", planCount);
		if(true){
			printGroupStats(feasiblePlans);
		}
	}
private class MySort implements Comparator<VehiclePlanList> 
{ 
    // Used for sorting in ascending order of 
    // roll number 
    @Override
    public int compare(VehiclePlanList a, VehiclePlanList b) 
    {
        int a_l = 0;
        int b_l = 0;
        for (int i = 1; i > 0; i++) {
            if(a.optimalPlanVehicle.getId().charAt(a.optimalPlanVehicle.getId().length()-i) != ' '){
                a_l++;
            }else{
                break;
            }
        }
        for (int i = 1; i > 0; i++) {
            if(b.optimalPlanVehicle.getId().charAt(b.optimalPlanVehicle.getId().length()-i) != ' '){
                b_l++;
            }else{
                break;
            }
        }
        if(a_l > b_l){
            return 1;
        }else if(a_l < b_l){
            return -1;
        }else{
            int lenght = a_l;
            for (int i = lenght; i > 0; i--) {
                if(a.optimalPlanVehicle.getId().charAt(a.optimalPlanVehicle.getId().length()-i) >
                        b.optimalPlanVehicle.getId().charAt(b.optimalPlanVehicle.getId().length()-i)){
                    return 1;
                }else if(a.optimalPlanVehicle.getId().charAt(a.optimalPlanVehicle.getId().length()-i) <
                        b.optimalPlanVehicle.getId().charAt(b.optimalPlanVehicle.getId().length()-i)){
                    return -1;
                }
            }
            
            
        }

        return 0;
    } 
}
}
