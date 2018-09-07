package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;

import java.util.Set;

public class VGARideSharingOnDemandVehicle extends RideSharingOnDemandVehicle {

    private Set<VGAVehiclePlan> plans;

    public VGARideSharingOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, PhysicalVehicleDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor, StandardTimeProvider timeProvider, boolean precomputedPaths, IdGenerator rebalancingIdGenerator, AmodsimConfig config, String vehicleId, SimulationNode startPosition) {
        super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, precomputedPaths, rebalancingIdGenerator, config, vehicleId, startPosition);
    }

    public Set<VGAVehiclePlan> getPlans() {
        return plans;
    }

    public void setPlans(Set<VGAVehiclePlan> plans) {
        this.plans = plans;
    }

}
