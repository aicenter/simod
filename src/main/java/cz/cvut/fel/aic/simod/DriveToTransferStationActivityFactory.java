package cz.cvut.fel.aic.simod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.ActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.Drive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.VehicleMoveActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.Driver;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;

/**
 * @author fido
 */
@Singleton
public class DriveToTransferStationActivityFactory extends ActivityFactory implements PhysicalVehicleDriveFactory {

    private final TransportNetworks transportNetworks;

    private final VehicleMoveActivityFactory moveActivityFactory;

    private final TypedSimulation eventProcessor;

    private final StandardTimeProvider timeProvider;

    private final TripsUtil tripsUtil;


    @Inject
    public DriveToTransferStationActivityFactory(TransportNetworks transportNetworks, VehicleMoveActivityFactory moveActivityFactory,
                                TypedSimulation eventProcessor, StandardTimeProvider timeProvider,TripsUtil tripsUtil) {
        this.transportNetworks = transportNetworks;
        this.moveActivityFactory = moveActivityFactory;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        this.tripsUtil = tripsUtil;
    }


    @Override
    public <A extends Agent & Driver> void runActivity(A agent, PhysicalVehicle vehicle, Trip<SimulationNode> trip) {
        create(agent, vehicle, trip).run();
    }


    public <A extends Agent & Driver> DriveToTransferStation<A> create(A agent, PhysicalVehicle vehicle, Trip<SimulationNode> trip) {
        return new DriveToTransferStation<>(activityInitializer, transportNetworks, moveActivityFactory, eventProcessor,
                timeProvider, agent, vehicle, trip);
    }

    @Override
    public <A extends Agent & Driver> DriveToTransferStation<A> create(A agent, PhysicalVehicle vehicle, SimulationNode target) {
        Trip<SimulationNode> trip = tripsUtil.createTrip(agent.getPosition(), target);

        return new DriveToTransferStation<>(activityInitializer, transportNetworks, moveActivityFactory, eventProcessor, timeProvider,
                agent, vehicle, trip);
    }

}
