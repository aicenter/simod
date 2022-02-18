package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.PhysicalPFVehicleStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

public class PeopleFreightVehicle extends RideSharingOnDemandVehicle<PhysicalPFVehicle>
{
    public final int vehiclePassengerCapacity = 1;

    private static final int LENGTH = 4;

    private boolean passengerOnboard;

    private final int maxParcelsCapacity;

    private int currentParcelsWeight;



    public PeopleFreightVehicle(
            PhysicalTransportVehicleStorage vehicleStorage,
            TripsUtil tripsUtil,
            StationsDispatcher onDemandVehicleStationsCentral,
            PhysicalVehicleDriveFactory driveActivityFactory,
            VisioPositionUtil positionUtil,
            IdGenerator tripIdGenerator,
            EventProcessor eventProcessor,
            StandardTimeProvider timeProvider,
            IdGenerator rebalancingIdGenerator,
            SimodConfig config,
            IdGenerator idGenerator,
            AgentpolisConfig agentpolisConfig,
            String vehicleId,
            SimulationNode startPosition,
            int maxParcelsCapacity)
    {
        super(
                vehicleStorage,
                tripsUtil,
                onDemandVehicleStationsCentral,
                driveActivityFactory,
                positionUtil,
                tripIdGenerator,
                eventProcessor,
                timeProvider,
                rebalancingIdGenerator,
                config,
                idGenerator,
                agentpolisConfig,
                vehicleId,
                startPosition);

        initPhysicalVehicle(vehicleId, startPosition, agentpolisConfig, vehicleStorage);

        this.maxParcelsCapacity = maxParcelsCapacity;
        this.currentParcelsWeight = 0;
        this.passengerOnboard = false;
    }


    // create new PhysicalPFVehicle
    @Override
    public void initPhysicalVehicle(String vehicleId, SimulationNode startPosition, AgentpolisConfig agentpolisConfig, PhysicalTransportVehicleStorage vehicleStorage)
    {
        vehicle = new PhysicalPFVehicle(
                vehicleId + " - vehicle",
                DemandSimulationEntityType.VEHICLE,
                LENGTH,
                maxParcelsCapacity,
                EGraphType.HIGHWAY,
                startPosition,
                agentpolisConfig.maxVehicleSpeedInMeters,
                vehiclePassengerCapacity);
        vehicleStorage.addEntity(vehicle);
        vehicle.setDriver(this);
        state = OnDemandVehicleState.WAITING;
    }

    public int getMaxParcelsCapacity()
    {
        return maxParcelsCapacity;
    }

    public int getCurrentParcelsWeight()
    {
        return currentParcelsWeight;
    }

    public boolean isPassengerOnboard()
    {
        return passengerOnboard;
    }

    public void setPassengerOnboard(boolean passengerOnboard)
    {
        this.passengerOnboard = passengerOnboard;
    }

    public void setCurrentParcelsWeight(int curWeight)
    {
        this.currentParcelsWeight = curWeight;
    }




}
