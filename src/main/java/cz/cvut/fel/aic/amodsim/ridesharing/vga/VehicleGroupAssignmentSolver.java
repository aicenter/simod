package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAILPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import java.util.*;

public class VehicleGroupAssignmentSolver extends DARPSolver {

    private final int maxDelayTime;
    private final double maxDistance;
    private final double maxDistanceSquared;

    private final AmodsimConfig config;
    private final PositionUtil positionUtil;

    private final Set<VGARequest> allRequests = new LinkedHashSet<>();
	
	private final VGAGroupGenerator vGAGroupGenerator;
	
	private final VGARequest.VGARequestFactory vGARequestFactory;

    private static TimeProvider timeProvider;

    private static VGAVehicle vehicle;
    private static Map<RideSharingOnDemandVehicle, VGAVehiclePlan> currentPlans = new LinkedHashMap<>();

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, AmodsimConfig config, 
			TimeProvider timeProvider, VGAGroupGenerator vGAGroupGenerator, VGARequest.VGARequestFactory vGARequestFactory) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
		this.vGAGroupGenerator = vGAGroupGenerator;
		this.vGARequestFactory = vGARequestFactory;
        VehicleGroupAssignmentSolver.timeProvider = timeProvider;
        maxDistance = (double) config.amodsim.ridesharing.maxWaitTime
                * config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime * 1000;

        MathUtils.setTravelTimeProvider(travelTimeProvider);
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {

        System.out.println("Current sim time is: " + timeProvider.getCurrentSimTime() / 1000.0);
        System.out.println("No. of request being added: " + requests.get(0).getDemandAgent().getSimpleId());
        System.out.println();

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

        //Filtering the vehicles, which are either on the same spot (in the station) or rebalancing
        AgentPolisEntity vehicles[] = vehicleStorage.getEntitiesForIteration();
        List<RideSharingOnDemandVehicle> rsodVehicles = new ArrayList<>();
        for (AgentPolisEntity e : vehicles) {
            boolean add = true;
            for (RideSharingOnDemandVehicle v : rsodVehicles) {
                if (e.getPosition() == v.getPosition()) {
                    add = false;
                    break;
                }
            }
            if (add && ((RideSharingOnDemandVehicle) e).getState() != OnDemandVehicleState.REBALANCING) {
                rsodVehicles.add((RideSharingOnDemandVehicle) e);
            }
        }

        //Converting vehicles
        int i = 0;
        VGAVehicle vgaVehicles[] = new VGAVehicle[rsodVehicles.size() + 1];
        for (RideSharingOnDemandVehicle v : rsodVehicles) {
            vgaVehicles[i++] = VGAVehicle.newInstance(v);
        }
        vehicle = vgaVehicles[0] ;

        //Converting requests
        List<VGARequest> vgaRequests = new ArrayList<>();
        for (OnDemandRequest request : requests) {
            vgaRequests.add(vGARequestFactory.create(request.getDemandAgent().getPosition(), 
					request.getTargetLocation(), request.getDemandAgent()));
        }

        allRequests.addAll(vgaRequests);

        //A drop-off or a pickup might have happened in the meantime
        for (VGAVehicle v : vgaVehicles) {
            if(v == null) continue;

            VGAVehiclePlan plan = currentPlans.get(v.getRidesharingVehicle());
            if(plan == null) continue;

            for(VGAVehiclePlanAction action : plan.getActions()) {
                if(action instanceof VGAVehiclePlanPickup) {
                    v.addPromisedRequest(action.getRequest());
                }
            }

            List transporting = v.getRidesharingVehicle().getVehicle().getTransportedEntities();
            for (Object entity : transporting) {
                VGARequest request = VGARequest.getRequestByDemandAgentSimpleId(((DemandAgent) entity));
                v.removePromisedRequest(request);
                v.addRequestOnBoard(request);
            }

            for(VGARequest request : v.getRequestsOnBoard()) {
                if(!transporting.contains(request.getDemandAgent())) {
                    v.removeRequestOnBoard(request);
                    allRequests.remove(request);
                }
            }
        }

        //Generating feasible plans for each vehicle
        Map<VGAVehicle, Set<VGAVehiclePlan>> feasiblePlans = new LinkedHashMap<>();

        i = 0;
        for (VGAVehicle vehicle : vgaVehicles) {
             if (i != rsodVehicles.size()) {
                List<VGARequest> rqsForVehicle = new ArrayList<>(vgaRequests);
                rqsForVehicle.addAll(vehicle.getPromisedRequests());
                rqsForVehicle.addAll(vehicle.getRequestsOnBoard());

                feasiblePlans.put(vehicle, vGAGroupGenerator.generateGroupsForVehicle(vehicle, rqsForVehicle, rsodVehicles.size()));
            } else {
                vgaVehicles[rsodVehicles.size()] = VGAVehicle.newInstance(null);
                feasiblePlans.put(vgaVehicles[rsodVehicles.size()], VGAGroupGenerator.generateDroppingVehiclePlans(vgaVehicles[rsodVehicles.size()], allRequests));
            }
            i++;
        }

        System.out.println("Generated groups for all vehicles.");

        //Using an ILP solver to optimally assign a group to each vehicle

        Map<VGAVehicle, VGAVehiclePlan> optimalPlans = VGAILPSolver.assignOptimallyFeasiblePlans(feasiblePlans, vgaRequests);

        //Removing the unnecessary empty plans

        Set<VGAVehicle> toRemove = new LinkedHashSet<>();
        for (Map.Entry<VGAVehicle, VGAVehiclePlan> entry : optimalPlans.entrySet()) {
            if(entry.getValue().getActions().size() == 0) {
                toRemove.add(entry.getKey());
            }
        }

        for(VGAVehicle v : toRemove) {
            optimalPlans.remove(v);
        }

        //Filling the output with converted plans

        for(Map.Entry<VGAVehicle, VGAVehiclePlan> entry : optimalPlans.entrySet()) {
            if(entry.getKey().getRidesharingVehicle() != null) {
                currentPlans.put(entry.getKey().getRidesharingVehicle(), entry.getValue());
                planMap.put(entry.getKey().getRidesharingVehicle(), entry.getValue().toDriverPlan());
            }
        }

        VGAVehicle.resetMapping();
        return planMap;
    }

    public static RideSharingOnDemandVehicle getVehicle() {
        return vehicle.getRidesharingVehicle();
    }

    public static TimeProvider getTimeProvider() {
        return timeProvider;
    }

}
