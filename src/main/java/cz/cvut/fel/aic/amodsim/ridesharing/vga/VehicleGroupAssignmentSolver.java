package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
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
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
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
import cz.cvut.fel.aic.amodsim.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsVGA;

@Singleton
public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VehicleGroupAssignmentSolver.class);
	
	private static final int MILLION = 1000000;
	
	
	private final LinkedHashSet<DefaultPlanComputationRequest> waitingRequests;
	
	private final LinkedHashSet<DefaultPlanComputationRequest> activeRequests;

	private final AmodsimConfig config;
	
	private final GroupGenerator groupGenerator;
	
	private final GurobiSolver gurobiSolver;
	

	
	private final Map<Integer,DefaultPlanComputationRequest> requestsMapByDemandAgents;
	
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
	
	private List<VGAVehicle> vgaVehicles;

	private static TimeProvider timeProvider;
	
	private int requestCounter;
	
	int startTime;
	
	int planCount;
	
	List<VehiclePlanList> feasiblePlans;
	
	List<List<String>> logRecords;
	
	// statistic vars
	
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
		waitingRequests = new LinkedHashSet<>();
		activeRequests = new LinkedHashSet<>();
		requestsMapByDemandAgents = new HashMap<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
		VehicleGroupAssignmentSolver.timeProvider = timeProvider;
		MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
		requestCounter = 0;
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
		
		// statistic vars
		newRequestCount = requests.size();
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

		LOGGER.info("Current sim time is: " + timeProvider.getCurrentSimTime() / 1000.0);
		

		Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

		LOGGER.info("Total vehicle count: " + vgaVehicles.size());
		List<VGAVehicle> drivingVehicles = filterVehicles(vgaVehicles);

		// Converting requests and adding them to collections
		for (OnDemandRequest request : requests) {
			SimulationNode requestStartPosition = request.getDemandAgent().getPosition();
			DefaultPlanComputationRequest newRequest = requestFactory.create(requestCounter++, requestStartPosition, 
					request.getTargetLocation(), request.getDemandAgent());
			waitingRequests.add(newRequest);
			activeRequests.add(newRequest);
			requestsMapByDemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
		}
		
		LOGGER.info("No. of new requests: " + requests.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequests.size());
		LOGGER.info("No. of active requests: {}", activeRequests.size());
		LOGGER.info("Number of vehicles used for planning: {}", drivingVehicles.size() + 1 * waitingRequests.size());


		/* Generating feasible plans for each vehicle */
		Benchmark.measureTime(() -> generateGroups(drivingVehicles));	
		groupGenerationTime = Benchmark.getDurationMsInt();
		
		
		/* Using an ILP solver to optimally assign a group to each vehicle */		
		List<Plan<IOptimalPlanVehicle>> optimalPlans 
				= Benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests));
		
		// ILP solver generation total time 
		solverTime = Benchmark.getDurationMsInt();

		/* Filling the output with converted plans */
		
		// for virtual vehicles
		Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
		
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
				int index = usedVehiclesPerStation.containsKey(nearestStation) 
						? usedVehiclesPerStation.get(nearestStation) : 0;

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

	public static TimeProvider getTimeProvider() {
		return timeProvider;
	}

	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
		OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
		OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
		DefaultPlanComputationRequest request = requestsMapByDemandAgents.get(eventContent.getDemandId());
		VGAVehicle vehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(eventContent.getOnDemandVehicleId());
		if(eventType == OnDemandVehicleEvent.PICKUP){
			vehicle.addRequestOnBoard(request);
			if(!waitingRequests.remove(request)){
				try {
					throw new Exception("Request picked up twice");
				} catch (Exception ex) {
					Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
				}
			};
			request.setOnboard(true);
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

	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, 
			LinkedHashSet<DefaultPlanComputationRequest> waitingRequests) {
		List<Plan> feasibleGroupPlans = Benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, startTime));
		
		// log
		if(config.ridesharing.vga.logPlanComputationalTime){
			logPlansPerVehicle(vehicle, feasibleGroupPlans, Benchmark.durationNano);
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

	private void generateGroups(List<VGAVehicle> drivingVehicles) {
		feasiblePlans = new ArrayList<>(drivingVehicles.size());
		startTime = (int) Math.round(VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0);
		LOGGER.info("Generating groups for vehicles.");
		planCount = 0;
		
//		// global groups
//		LOGGER.info("Generating global groups");
//		Set<Set<PlanComputationRequest>> globalFeasibleGroups 
//				= vGAGroupGenerator.generateGlobalGroups(waitingRequests, startTime);
//		LOGGER.info("{} global groups generated", globalFeasibleGroups.size());
		
		// groups for driving vehicls
		for (VGAVehicle vehicle : ProgressBar.wrap(drivingVehicles, "Generating groups for driving vehicles")) {
			List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vehicle, waitingRequests);
			VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
			feasiblePlans.add(vehiclePlanList);
			planCount += feasibleGroupPlans.size();
		}
		
		// groups for vehicles in the station
		if(!onDemandvehicleStationStorage.isEmpty()){
			Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
			int insufficientCacityCount = 0;
			
			// dictionary - all vehicles from a station have the same feasible groups
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation = new HashMap<>();
			
			for(DefaultPlanComputationRequest request: ProgressBar.wrap(waitingRequests, "Generating groups for vehicles in station")){
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
					
					// all waiting request can be assigned to the waiting vehicle
					List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vGAVehicle, waitingRequests);
//						groupGenerator.generateGroupsForVehicle(vGAVehicle, waitingRequests, startTime);
					plansFromStation.put(nearestStation, feasibleGroupPlans);
				}

				CollectionUtil.incrementMapValue(usedVehiclesPerStation, nearestStation, 1);
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

}
