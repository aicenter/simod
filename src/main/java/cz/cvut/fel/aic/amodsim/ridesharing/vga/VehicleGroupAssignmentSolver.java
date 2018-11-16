package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAILPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import java.util.*;

public class VehicleGroupAssignmentSolver extends DARPSolver implements EventHandler{
	
	private final Set<VGARequest> waitingRequests;
	
	private final Set<VGARequest> activeRequests;
	
//	private final Set<VGARequest> onboardRequests;

    private final AmodsimConfig config;
    private final PositionUtil positionUtil;
	
	private final VGAGroupGenerator vGAGroupGenerator;
	
	private final VGAILPSolver vGAILPSolver;
	
	private final VGARequest.VGARequestFactory vGARequestFactory;
	
    private final Map<Integer,VGARequest> requestsMapBydemandAgents;
	
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private VGAVehicle[] vgaVehicles;

    private static TimeProvider timeProvider;
	
	
	

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, AmodsimConfig config, 
			TimeProvider timeProvider, VGAGroupGenerator vGAGroupGenerator, 
			VGARequest.VGARequestFactory vGARequestFactory, TypedSimulation eventProcessor,
			VGAILPSolver vGAILPSolver) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
		this.vGAGroupGenerator = vGAGroupGenerator;
		this.vGAILPSolver = vGAILPSolver;
		this.vGARequestFactory = vGARequestFactory;
		this.eventProcessor = eventProcessor;
		waitingRequests = new HashSet<>();
		activeRequests = new HashSet<>();
//		onboardRequests = new HashSet<>();
		requestsMapBydemandAgents = new HashMap<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
        VehicleGroupAssignmentSolver.timeProvider = timeProvider;
        MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
		
		//init VGA vehicles	
		if(vgaVehicles == null){
			initVehicles();
		}

        System.out.println("Current sim time is: " + timeProvider.getCurrentSimTime() / 1000.0);
        System.out.println("No. of request new requests: " + requests.size());
        System.out.println();

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

		// here it will be possible to exclude some vehicles
		List<VGAVehicle> vehiclesForPlanning = Arrays.asList(vgaVehicles);

        // Converting requests and adding them to collections
        for (OnDemandRequest request : requests) {
			VGARequest newRequest = vGARequestFactory.create(request.getDemandAgent().getPosition(), 
					request.getTargetLocation(), request.getDemandAgent());
            waitingRequests.add(newRequest);
			activeRequests.add(newRequest);
			requestsMapBydemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
        }

        // Generating feasible plans for each vehicle
        Map<VGAVehicle, Set<VGAVehiclePlan>> feasiblePlans = new LinkedHashMap<>();
        for (VGAVehicle vehicle : vehiclesForPlanning) {
			List<VGARequest> requestsForVehicle = new ArrayList<>(waitingRequests);

			// we only try to serve onboard request that are already transported by this vehicle
			requestsForVehicle.addAll(vehicle.getRequestsOnBoard());

			feasiblePlans.put(vehicle, 
					vGAGroupGenerator.generateGroupsForVehicle(vehicle, requestsForVehicle, vehiclesForPlanning.size()));
        }
		// empty plan
		VGAVehicle nullVehicle = VGAVehicle.newInstance(null);
		feasiblePlans.put(nullVehicle, 
				VGAGroupGenerator.generateDroppingVehiclePlans(nullVehicle, activeRequests));
        
		System.out.println("Generated groups for all vehicles.");

        //Using an ILP solver to optimally assign a group to each vehicle
        Map<VGAVehicle, VGAVehiclePlan> optimalPlans 
				= vGAILPSolver.assignOptimallyFeasiblePlans(feasiblePlans, activeRequests);

        //Removing the unnecessary empty plans
        Set<VGAVehicle> toRemove = new LinkedHashSet<>();
        for (Map.Entry<VGAVehicle, VGAVehiclePlan> entry : optimalPlans.entrySet()) {
            if(entry.getValue().getActions().isEmpty()) {
                toRemove.add(entry.getKey());
            }
        }
        for(VGAVehicle v : toRemove) {
            optimalPlans.remove(v);
        }

        //Filling the output with converted plans
        for(Map.Entry<VGAVehicle, VGAVehiclePlan> entry : optimalPlans.entrySet()) {
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
			waitingRequests.remove(request);
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
		vgaVehicles = new VGAVehicle[vehicleStorage.size()];
		int i = 0;
		for(OnDemandVehicle onDemandVehicle: vehicleStorage){
			VGAVehicle newVGAVehicle = VGAVehicle.newInstance((RideSharingOnDemandVehicle) onDemandVehicle);
			vgaVehicles[i++] = newVGAVehicle;
			vgaVehiclesMapBydemandOnDemandVehicles.put(onDemandVehicle.getId(), newVGAVehicle);
		}
	}

}
