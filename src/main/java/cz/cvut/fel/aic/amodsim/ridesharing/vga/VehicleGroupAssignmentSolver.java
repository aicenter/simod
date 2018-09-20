package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.Demand;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAILPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import java.util.*;

public class VehicleGroupAssignmentSolver extends DARPSolver {

    private final int maxDelayTime;
    private final double maxDistance;
    private final double maxDistanceSquared;

    private final AmodsimConfig config;
    private final PositionUtil positionUtil;

    private List<VGARequest> allRequests = new ArrayList<>();

    private static TimeProvider timeProvider;

    private static VGAVehicle vehicle;
    private static Map<RideSharingOnDemandVehicle, VGAVehiclePlan> currentPlans = new LinkedHashMap<>();

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
                                        OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
                                        AmodsimConfig config, TimeProvider timeProvider) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
        VehicleGroupAssignmentSolver.timeProvider = timeProvider;
        maxDistance = (double) config.amodsim.ridesharing.maxWaitTime
                * config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime * 1000;

        MathUtils.setTravelTimeProvider(travelTimeProvider);
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {

        System.out.println("Current sim time is: " + timeProvider.getCurrentSimTime());
        System.out.println("No of request being added: " + requests.get(0).getDemandAgent().getSimpleId());
        System.out.println();

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new LinkedHashMap<>();

        //Filtering the vehicles, which are either on the same spot (in the station) or rebalancing

        AgentPolisEntity vehicles[] = vehicleStorage.getEntitiesForIteration();
        List<RideSharingOnDemandVehicle> rsvhs = new ArrayList<>();
        for (AgentPolisEntity e : vehicles) {
            boolean add = true;
            for (RideSharingOnDemandVehicle v : rsvhs) {
                if (e.getPosition() == v.getPosition()) {
                    add = false;
                    break;
                }
            }
            if (add && ((RideSharingOnDemandVehicle) e).getState() != OnDemandVehicleState.REBALANCING) {
                rsvhs.add((RideSharingOnDemandVehicle) e);
            }
        }

        //Converting vehicles

        int i = 0;
        VGAVehicle vhs[] = new VGAVehicle[rsvhs.size() + 1];
        for (RideSharingOnDemandVehicle v : rsvhs) {
            vhs[i++] = VGAVehicle.newInstance(v);
        }
        vehicle = vhs[0] ;

        //Converting requests

        List<VGARequest> rqs = new ArrayList<>();
        for (OnDemandRequest request : requests) {
            rqs.add(VGARequest.newInstance(request.getDemandAgent().getPosition(), request.getTargetLocation(), request.getDemandAgent()));
        }

        allRequests.addAll(rqs);

        //A dropoff or a pickup might have happened in the meantime

        for (VGAVehicle v : vhs) {
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
        for (VGAVehicle vehicle : vhs) {
             if (i != rsvhs.size()) {
                List<VGARequest> rqsForVehicle = new ArrayList<>(rqs);
                rqsForVehicle.addAll(vehicle.getPromisedRequests());
                rqsForVehicle.addAll(vehicle.getRequestsOnBoard());

                feasiblePlans.put(vehicle, VGAGroupGenerator.generateGroupsForVehicle(vehicle, rqsForVehicle, rsvhs.size()));
            } else {
                vhs[rsvhs.size()] = VGAVehicle.newInstance(null);
                feasiblePlans.put(vhs[rsvhs.size()], VGAGroupGenerator.generateDroppingVehiclePlans(vhs[rsvhs.size()], allRequests));
            }
            i++;
        }

        System.out.println("Generated groups for all vehicles.");

        //Using an ILP solver to optimally assign a group to each vehicle

        Map<VGAVehicle, VGAVehiclePlan> optimalPlans = VGAILPSolver.assignOptimallyFeasiblePlans(feasiblePlans, rqs);
        Set<VGAVehicle> toRemove = new LinkedHashSet<>();

        //Removing the unnecessary empty plans

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

        VGAVehicle.resetIds();
        VGARequest.resetIds();
        return planMap;
    }

    public static RideSharingOnDemandVehicle getVehicle() {
        return vehicle.getRidesharingVehicle();
    }

    public static TimeProvider getTimeProvider() {
        return timeProvider;
    }

}
