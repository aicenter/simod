package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAILPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

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
    private final PositionUtil positionUtil;
	
	private final VGAGroupGenerator vGAGroupGenerator;
	
	private final VGAILPSolver vGAILPSolver;
	
	private final GurobiSolver gurobiSolver;
	
	private final VGARequest.VGARequestFactory vGARequestFactory;
	
    private final Map<Integer,VGARequest> requestsMapBydemandAgents;
	
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private List<VGAVehicle> vgaVehicles;

    private static TimeProvider timeProvider;
	
	private int requestCounter;
	

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, AmodsimConfig config, 
			TimeProvider timeProvider, VGAGroupGenerator vGAGroupGenerator, 
			VGARequest.VGARequestFactory vGARequestFactory, TypedSimulation eventProcessor,
			VGAILPSolver vGAILPSolver, GurobiSolver gurobiSolver) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
		this.vGAGroupGenerator = vGAGroupGenerator;
		this.vGAILPSolver = vGAILPSolver;
		this.gurobiSolver = gurobiSolver;
		this.vGARequestFactory = vGARequestFactory;
		this.eventProcessor = eventProcessor;
		waitingRequests = new LinkedHashSet<>();
		activeRequests = new LinkedHashSet<>();
//		onboardRequests = new HashSet<>();
		requestsMapBydemandAgents = new HashMap<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
        VehicleGroupAssignmentSolver.timeProvider = timeProvider;
        MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
		requestCounter = 0;
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
		
		//init VGA vehicles	
		if(vgaVehicles == null){
			initVehicles();
		}

        LOGGER.info("Current sim time is: " + timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("No. of new requests: " + requests.size());

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

		LOGGER.info("Total vehicle count: " + vgaVehicles.size());
		List<VGAVehicle> vehiclesForPlanning = excludeUnnecesaryParkedVehicles(vgaVehicles, requests.size());
		LOGGER.info("Number of vehicles used for planning: " + vehiclesForPlanning.size());

        // Converting requests and adding them to collections
        for (OnDemandRequest request : requests) {
			VGARequest newRequest = vGARequestFactory.create(requestCounter++, request.getDemandAgent().getPosition(), 
					request.getTargetLocation(), request.getDemandAgent());
            waitingRequests.add(newRequest);
			activeRequests.add(newRequest);
			requestsMapBydemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
        }

        // Generating feasible plans for each vehicle
        List<VehiclePlanList> feasiblePlans = new ArrayList<>(vehiclesForPlanning.size());
		double startTime = VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0;
		LOGGER.info("Generating groups for vehicles.");
		int planCount = 0;
        for (VGAVehicle vehicle : ProgressBar.wrap(vehiclesForPlanning, "Generating groups for vehicles")) {
			List<Plan> feasibleGroupPlans = 
					vGAGroupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, startTime);

			VehiclePlanList vehiclePlanList = new VehiclePlanList(vehicle, feasibleGroupPlans);
			feasiblePlans.add(vehiclePlanList);
			planCount += feasibleGroupPlans.size();
        }
        
		LOGGER.info("{} groups generaated", planCount);

        //Using an ILP solver to optimally assign a group to each vehicle
        Map<VGAVehicle,Plan> optimalPlans 
				= gurobiSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests);

        //Filling the output with converted plans
        for(Map.Entry<VGAVehicle,Plan> entry : optimalPlans.entrySet()) {
            if(entry.getKey().getRidesharingVehicle() != null) {
                planMap.put(entry.getKey().getRidesharingVehicle(), entry.getValue().toDriverPlan());
            }
        }

        VGAVehicle.resetMapping();
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

	private List<VGAVehicle> excludeUnnecesaryParkedVehicles(List<VGAVehicle> vehiclesForPlanning, int size) {
		Map<OnDemandVehicleStation,Integer> includedParkedVehiclesPerStation = new HashMap<>();
		List<VGAVehicle> filteredVehiclesForPlanning = new LinkedList<>();
		for (VGAVehicle vGAVehicle : vehiclesForPlanning) {
			OnDemandVehicle vehicle = vGAVehicle.getRidesharingVehicle();
			OnDemandVehicleStation parkedIn = vehicle.getParkedIn();
			if(parkedIn != null){
				if(includedParkedVehiclesPerStation.containsKey(parkedIn) 
						&& includedParkedVehiclesPerStation.get(parkedIn) >= size){
					continue;
				}
				else{
					CollectionUtil.incrementMapValue(includedParkedVehiclesPerStation, parkedIn, 1);
				}
			}
			filteredVehiclesForPlanning.add(vGAVehicle);
		}
		return filteredVehiclesForPlanning;
	}

}
