package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
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
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import java.io.IOException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VehicleGroupAssignmentSolver.class);
	
	
	private final LinkedHashSet<VGARequest> waitingRequests;
	
	private final LinkedHashSet<VGARequest> activeRequests;
	
//	private final Set<VGARequest> onboardRequests;

    private final AmodsimConfig config;
    private final VisioPositionUtil positionUtil;
	
	private final VGAGroupGenerator vGAGroupGenerator;
	
	private final GurobiSolver gurobiSolver;
	
	private final VGARequest.VGARequestFactory vGARequestFactory;
	
    private final Map<Integer,VGARequest> requestsMapBydemandAgents;
	
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

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, VisioPositionUtil positionUtil, AmodsimConfig config, 
			TimeProvider timeProvider, VGAGroupGenerator vGAGroupGenerator, 
			VGARequest.VGARequestFactory vGARequestFactory, TypedSimulation eventProcessor, GurobiSolver gurobiSolver, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
		this.vGAGroupGenerator = vGAGroupGenerator;
		this.gurobiSolver = gurobiSolver;
		this.vGARequestFactory = vGARequestFactory;
		this.eventProcessor = eventProcessor;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		waitingRequests = new LinkedHashSet<>();
		activeRequests = new LinkedHashSet<>();
		requestsMapBydemandAgents = new HashMap<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
        VehicleGroupAssignmentSolver.timeProvider = timeProvider;
        MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
		requestCounter = 0;
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
		
		 logRecords = new ArrayList<>();
		
		//init VGA vehicles	
		if(vgaVehicles == null){
			initVehicles();
		}

        LOGGER.info("Current sim time is: " + timeProvider.getCurrentSimTime() / 1000.0);
		

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

		LOGGER.info("Total vehicle count: " + vgaVehicles.size());
		List<VGAVehicle> drivingVehicles = excludeParkedVehicles(vgaVehicles);

        // Converting requests and adding them to collections
        for (OnDemandRequest request : requests) {
			SimulationNode requestStartPosition = request.getDemandAgent().getPosition();
			VGARequest newRequest = vGARequestFactory.create(requestCounter++, requestStartPosition, 
					request.getTargetLocation(), request.getDemandAgent());
            waitingRequests.add(newRequest);
			activeRequests.add(newRequest);
			requestsMapBydemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
        }
		
		LOGGER.info("No. of new requests: " + requests.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequests.size());
		LOGGER.info("No. of active requests: {}", activeRequests.size());
		LOGGER.info("Number of vehicles used for planning: {}", drivingVehicles.size() + 1 * waitingRequests.size());


        // Generating feasible plans for each vehicle
		feasiblePlans = new ArrayList<>(drivingVehicles.size());
		startTime = (int) Math.round(VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0);
		LOGGER.info("Generating groups for vehicles.");
		planCount = 0;
//		
//		// global groups
//		LOGGER.info("Generating global groups");
//		Set<Set<PlanComputationRequest>> globalFeasibleGroups 
//				= vGAGroupGenerator.generateGlobalGroups(waitingRequests, startTime);
//		LOGGER.info("{} global groups generated", globalFeasibleGroups.size());
		
		// groups for driving vehicls
        for (VGAVehicle vehicle : ProgressBar.wrap(drivingVehicles, "Generating groups for driving vehicles")) {
			computeGroupsForVehicle(vehicle, waitingRequests);
        }
		
		// groups for vehicles in the station
		if(!onDemandvehicleStationStorage.isEmpty()){
			Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
			int insufficientCacityCount = 0;
			
			// dictionary - all vehicles from a station have the same feasible groups
			Map<OnDemandVehicleStation,List<Plan>> plansFromStation = new HashMap<>();
			
			for(VGARequest request: ProgressBar.wrap(waitingRequests, "Generating groups for vehicles in station")){
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
					List<Plan> feasibleGroupPlans = 
						vGAGroupGenerator.generateGroupsForVehicle(vGAVehicle, waitingRequests, startTime);
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

        //Using an ILP solver to optimally assign a group to each vehicle
        List<Plan<IOptimalPlanVehicle>> optimalPlans 
				= gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests);

		
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
		
//		logRecords();
		
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
		VGARequest request = requestsMapBydemandAgents.get(eventContent.getDemandId());
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

	private List<VGAVehicle> excludeParkedVehicles(List<VGAVehicle> vehiclesForPlanning) {
		List<VGAVehicle> filteredVehiclesForPlanning = new LinkedList<>();
		for (VGAVehicle vGAVehicle : vehiclesForPlanning) {
			OnDemandVehicle vehicle = vGAVehicle.getRidesharingVehicle();
			OnDemandVehicleStation parkedIn = vehicle.getParkedIn();
			if(parkedIn == null){
				filteredVehiclesForPlanning.add(vGAVehicle);
			}
		}
		return filteredVehiclesForPlanning;
	}

	private DriverPlan toDriverPlan(Plan<IOptimalPlanVehicle> plan) {
		List<DriverPlanTask> tasks = new ArrayList<>(plan.getActions().size() + 1);
		tasks.add(new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, 
				plan.getVehicle().getPosition()));
		for(VGAVehiclePlanAction action: plan.getActions()){
			DriverPlanTaskType taskType;
			if(action instanceof VGAVehiclePlanPickup){
				taskType = DriverPlanTaskType.PICKUP;
			}
			else{
				taskType = DriverPlanTaskType.DROPOFF;
			}
			DriverPlanTask task = new DriverPlanTask(
					taskType, ((VGARequest) action.getRequest()).getDemandAgent(), action.getPosition());
			tasks.add(task);
		}
		DriverPlan driverPlan = new DriverPlan(tasks, plan.getEndTime() - plan.getStartTime());
		
		return driverPlan;
	}

	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, LinkedHashSet<VGARequest> waitingRequests) {
		long startTimeNano = System.nanoTime();
		List<Plan> feasibleGroupPlans = 
					vGAGroupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, startTime);
		long totalTimeNano = System.nanoTime() - startTimeNano;

		VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
		feasiblePlans.add(vehiclePlanList);
		planCount += feasibleGroupPlans.size();
		
		// log
//		logPlansPerVehicle(vehicle, feasibleGroupPlans, totalTimeNano);
		
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
		List<String> record = new ArrayList<>(5);
		record.add(Integer.toString(startTime));
		record.add(vehicle.getRidesharingVehicle().getId());
		record.add(Long.toString(Math.round(totalTimeNano / 1000000)));
		record.add(Integer.toString(vehicle.getRequestsOnBoard().size()));
		
		int actionCount = 13;
		int[] counts = new int[actionCount];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
			
		}
		for (Plan feasibleGroupPlan : feasibleGroupPlans) {
			counts[feasibleGroupPlan.getActions().size()]++;
		}
		for (int i = 0; i < counts.length; i++) {
			record.add(Integer.toString(counts[i]));
			
		}
		logRecords.add(record);
	}

	private void logRecords() {
		try {
            CsvWriter writer = new CsvWriter(
                    Common.getFileWriter(config.amodsim.ridesharing.vga.groupGeneratorLogFilepath, true));
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

}
