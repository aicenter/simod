package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.DelayData;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.event.RebalancingEventContent;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

import java.util.List;


public class OnDemandPFVehicle extends OnDemandVehicle
{

    private static final int LENGTH = 4;


    private final int index;

    protected PhysicalPFVehicle vehicle;

    protected final TripsUtil tripsUtil;

    protected final StationsDispatcher onDemandVehicleStationsCentral;

    protected final PhysicalVehicleDriveFactory driveFactory;

    private final VisioPositionUtil positionUtil;

    protected final EventProcessor eventProcessor;

    protected final StandardTimeProvider timeProvider;

    private final IdGenerator rebalancingIdGenerator;

    private List<SimulationNode> demandNodes;


    protected final SimodConfig config;

    protected OnDemandVehicleState state;

    protected OnDemandVehicleStation departureStation;

    protected OnDemandVehicleStation targetStation;

    protected VehicleTrip currentTrip;

    protected VehicleTrip demandTrip;

    protected VehicleTrip tripToStation;

    private VehicleTrip completeTrip;

    protected int metersWithPassenger;

    protected int metersToStartLocation;

    protected int metersToStation;

    private int metersRebalancing;

    private SimulationNode targetNode;

    private DelayData delayData;

    private DemandData currentlyServedDemmand;

    private int currentRebalancingId;

    protected OnDemandVehicleStation parkedIn;






    @Inject
    public OnDemandPFVehicle(
            PhysicalTransportVehicleStorage vehicleStorage,
            TripsUtil tripsUtil,
            StationsDispatcher onDemandVehicleStationsCentral,
            PhysicalVehicleDriveFactory driveFactory,
            VisioPositionUtil positionUtil,
            EventProcessor eventProcessor,
            StandardTimeProvider timeProvider,
            IdGenerator rebalancingIdGenerator,
            SimodConfig config,
            IdGenerator idGenerator,
            AgentpolisConfig agentpolisConfig,
            @Assisted String vehicleId, @Assisted SimulationNode startPosition)
    {
        super(
                vehicleStorage,
                tripsUtil,
                onDemandVehicleStationsCentral,
                driveFactory,
                positionUtil,
                eventProcessor,
                timeProvider,
                rebalancingIdGenerator,
                config,
                idGenerator,
                agentpolisConfig,
                vehicleId,
                startPosition);

        this.tripsUtil = tripsUtil;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.driveFactory = driveFactory;
        this.positionUtil = positionUtil;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        this.rebalancingIdGenerator = rebalancingIdGenerator;
        this.config = config;

        index = idGenerator.getId();

        vehicle = new PhysicalPFVehicle(
                vehicleId + " - vehicle",
                DemandSimulationEntityType.VEHICLE,
                LENGTH,
                0,
                EGraphType.HIGHWAY,
                startPosition,
                agentpolisConfig.maxVehicleSpeedInMeters,
                config.ridesharing.vehicleCapacity);

        vehicleStorage.addEntity(vehicle);
        vehicle.setDriver(this);
        state = OnDemandVehicleState.WAITING;

        metersWithPassenger = 0;
        metersToStartLocation = 0;
        metersToStation = 0;
        metersRebalancing = 0;
    }



    protected void driveToTargetLocation()
    {
        state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        pickupDemand();

        departureStation.release(this);

        currentTrip = demandTrip;

//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//		driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, currentTrip);

    }

    protected void driveToNearestStation()
    {
        if (tripToStation == null)
        {
            waitInStation();
            return;
        }

        state = OnDemandVehicleState.DRIVING_TO_STATION;

        currentTrip = tripToStation;

//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//		driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, currentTrip);
    }

    protected void waitInStation()
    {
        targetStation.parkVehicle(this);
        park();
    }

    protected void park()
    {
        state = OnDemandVehicleState.WAITING;
        completeTrip = null;
    }

    @Override
    public VehicleTrip getCurrentTripPlan()
    {
        return completeTrip;
    }

    public Node getDemandTarget()
    {
        if (demandNodes != null)
        {
            return demandNodes.get(demandNodes.size() - 1);
        }
        return null;
    }

    public void startRebalancing(OnDemandVehicleStation targetStation)
    {
        eventProcessor.addEvent(OnDemandVehicleEvent.START_REBALANCING, null, null,
                new RebalancingEventContent(timeProvider.getCurrentSimTime(), currentRebalancingId,
                        getId(), getParkedIn(), targetStation));

        parkedIn.releaseVehicle(this);
        state = OnDemandVehicleState.REBALANCING;
        currentRebalancingId = rebalancingIdGenerator.getId();

        currentTrip = tripsUtil.createTrip(getPosition(), targetStation.getPosition(), vehicle);
        metersRebalancing += positionUtil.getTripLengthInMeters(currentTrip);

        completeTrip = new VehicleTrip(currentRebalancingId, currentTrip.getVehicle(), currentTrip.getLocations().clone());

        this.targetStation = targetStation;

        driveFactory.runActivity(this, vehicle, currentTrip);
    }

    @Override
    public PhysicalPFVehicle getVehicle()
    {
        return vehicle;
    }

    @Override
    public double getVelocity()
    {
        return (double) vehicle.getVelocity();
    }

    public int getCapacity()
    {
        return vehicle.getCapacity();
    }

//	@Override
//	public List<AgentPolisEntity> getTransportedEntities() {
//		return cargo;
//	}

    @Override
    public void setTargetNode(SimulationNode targetNode)
    {
        this.targetNode = targetNode;
    }

    @Override
    public SimulationNode getTargetNode()
    {
        return targetNode;
    }

    protected void leavingStationEvent()
    {
        eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null,
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
                        currentlyServedDemmand.demandAgent.getSimpleId(), getId()));
    }

    protected void pickupDemand()
    {
        currentlyServedDemmand.demandAgent.tripStarted(this);
        vehicle.pickUp(currentlyServedDemmand.demandAgent);
        eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null,
                new PickupEventContent(timeProvider.getCurrentSimTime(),
                        currentlyServedDemmand.demandAgent.getSimpleId(), getId(),
                        positionUtil.getTripLengthInMeters(demandTrip)));
    }

    protected void dropOffDemand()
    {
        currentlyServedDemmand.demandAgent.tripEnded();
        vehicle.dropOff(currentlyServedDemmand.demandAgent);
        eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null,
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
                        currentlyServedDemmand.demandAgent.getSimpleId(), getId()));
    }

    @Override
    protected void onActivityFinish(Activity activity)
    {
        super.onActivityFinish(activity);
        PhysicalVehicleDrive drive = (PhysicalVehicleDrive) activity;
        finishedDriving(drive.isStoped());
    }

    @Override
    public EntityType getType()
    {
        return DemandSimulationEntityType.ON_DEMAND_VEHICLE;
    }

    // I edited this method
    @Override
    public void startDriving(PhysicalTransportVehicle vehicle)
    {
        this.vehicle = (PhysicalPFVehicle) vehicle;
    }

    @Override
    public void setDelayData(DelayData delayData)
    {
        this.delayData = delayData;
    }

    @Override
    public DelayData getDelayData()
    {
        return delayData;
    }

    @Override
    public void endDriving()
    {

    }

    protected void finishRebalancing()
    {
        waitInStation();
        eventProcessor.addEvent(OnDemandVehicleEvent.FINISH_REBALANCING, null, null,
                new RebalancingEventContent(timeProvider.getCurrentSimTime(),
                        currentRebalancingId, getId(), null, parkedIn));
    }

    protected void finishDrivingToStation()
    {
        eventProcessor.addEvent(OnDemandVehicleEvent.REACH_NEAREST_STATION, null, null,
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
                        -1, getId()));
        waitInStation();
    }

}
