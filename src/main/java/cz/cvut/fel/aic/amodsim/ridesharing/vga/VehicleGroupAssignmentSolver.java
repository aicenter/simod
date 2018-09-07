package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.VGAILPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import java.util.*;

public class VehicleGroupAssignmentSolver extends DARPSolver {

    private final int maxDelayTime;
    private final double maxDistance;
    private final double maxDistanceSquared;

    private final AmodsimConfig config;
    private final PositionUtil positionUtil;
    private final TimeProvider timeProvider;

    private static RideSharingOnDemandVehicle vehicle;

    @Inject
    public VehicleGroupAssignmentSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider,
                                        OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
                                        AmodsimConfig config, TimeProvider timeProvider) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
        this.timeProvider = timeProvider;
        maxDistance = (double) config.amodsim.ridesharing.maxWaitTime
                * config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime  * 1000;
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {

        MathUtils.setTravelTimeProvider(travelTimeProvider);
        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();

        List<VGARequest> rqs = new ArrayList<>();
        for (OnDemandRequest request : requests) {
            rqs.add(VGARequest.newInstance(request.getDemandAgent().getPosition(), request.getTargetLocation(), request.getDemandAgent()));
        }

        AgentPolisEntity vehicles[] = vehicleStorage.getEntitiesForIteration();
        RideSharingOnDemandVehicle vhs[] = new RideSharingOnDemandVehicle[vehicles.length];
        vehicle = vhs[0];
        int i = 0;
        for(AgentPolisEntity e : vehicles){
            vhs[i] = (RideSharingOnDemandVehicle) e;
        }

        Map<AgentPolisEntity, Set<VGAVehiclePlan>> feasiblePlans = new HashMap<>();

        for (RideSharingOnDemandVehicle vehicle : vhs) {
            feasiblePlans.put(vehicle, VGAGroupGenerator.generateGroupsForVehicle(vehicle, rqs, vehicles.length));
            System.out.println("Generated groups for vehicle id: " + vehicle.getId());
        }

        System.out.println("Generated groups for all vehicles.");

        //Using an ILP solver to optimally assign a group to each vehicle

        Set<VGAVehiclePlan> optimalPlans = VGAILPSolver.assignOptimallyFeasiblePlans(feasiblePlans, rqs);

        i = 0;
        for (VGAVehiclePlan plan : optimalPlans) {
            planMap.put(vhs[i], plan.toDriverPlan());
            System.out.println("Vehicle id: " + vehicles[i].getId());
            System.out.print(plan.toString());
            i++;
        }

        return planMap;
    }

    public static RideSharingOnDemandVehicle getVehicle() { return vehicle; }
}
