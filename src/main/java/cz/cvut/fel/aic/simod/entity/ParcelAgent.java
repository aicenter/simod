package cz.cvut.fel.aic.simod.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.ParcelData;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.entity.transportable.TrunkTransportableEntity;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.simod.statistics.StatisticEvent;
import cz.cvut.fel.aic.simod.storage.ParcelStorage;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ParcelAgent extends Agent implements EventHandler, TrunkTransportableEntity, SimulationAgent {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ParcelAgent.class);

    private final int simpleId;

    private final TimeTrip<SimulationNode> trip;

    private final StationsDispatcher onDemandVehicleStationsCentral;

    private final EventProcessor eventProcessor;

    private final StandardTimeProvider timeProvider;

    private final TripsUtil tripsUtil;

    /**
     * Request announcement time in milliseconds
     */
    private final long demandTime;

    private DemandAgentState state;

    private OnDemandVehicle onDemandVehicle;

    private TransportEntity transportingEntity;

    private SimulationNode lastFromPosition;

    private long realPickupTime = 0;

    private long minDemandServiceDuration;

    private boolean dropped;

    private final ParcelStorage parcelStorage;

    @Override
    public int getSimpleId() {
        return simpleId;
    }

    public long getRealPickupTime() {
        return realPickupTime;
    }

    @Override
    public long getDemandTime() {
        return demandTime;
    }

    public DemandAgentState getState() {
        return state;
    }

    public OnDemandVehicle getVehicle() {
        return onDemandVehicle;
    }

    public OnDemandVehicle getOnDemandVehicle() {
        return onDemandVehicle;
    }

    public long getMinDemandServiceDuration() {
        return minDemandServiceDuration;
    }

    @Override
    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    @Override
    public boolean isDropped() {
        return dropped;
    }

    @Inject
    public ParcelAgent(StationsDispatcher onDemandVehicleStationsCentral, EventProcessor eventProcessor,
                       StandardTimeProvider timeProvider, TripsUtil tripsUtil, @Assisted String agentId,
                       @Assisted int id, @Assisted TimeTrip<SimulationNode> trip, ParcelStorage parcelStorage) {
        super(agentId, trip.getLocations()[0]);
        this.simpleId = id;
        this.trip = trip;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.eventProcessor = eventProcessor;
        this.parcelStorage = parcelStorage;
        this.timeProvider = timeProvider;
        this.tripsUtil = tripsUtil;
        state = DemandAgentState.WAITING;
        dropped = false;
        demandTime = timeProvider.getCurrentSimTime();
        computeMinServiceDuration();
    }


    @Override
    public void born() {
        parcelStorage.addEntity(this);
        eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.PARCEL, onDemandVehicleStationsCentral, null,
                new ParcelData(trip.getLocations(), this));
    }

    @Override
    public void die() {
        parcelStorage.removeEntity(this);
    }

    @Override
    public void handleEvent(Event event) {
        onDemandVehicle = (OnDemandVehicle) event.getContent();
//		vehicle = onDemandVehicle.getVehicle();
//		rideAsPassengerActivity.usingVehicleAsPassenger(this.getId(), onDemandVehicle.getVehicleId(),
//				onDemandVehicle.getDemandTrip(this), this);
    }

    // TODO rework
    @Override
    public void tripEnded() {
        LOGGER.info("Parcel #{} dropped at: {}, last position of the trip should be: {}, {}",
                simpleId, getPosition().id, trip.getLastLocation().id, getPosition().equals(trip.getLastLocation()));
        if(!getPosition().equals(trip.getLastLocation())){
            try {
                throw new Exception("Demand not served properly for id: " + simpleId + " at position " +
                        getPosition() + " last position of the trip should be: " + trip.getLastLocation());
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            }
        }
        eventProcessor.addEvent(StatisticEvent.PARCEL_DROPPED_OFF, null, null,
                new DemandServiceStatistic(demandTime, realPickupTime, timeProvider.getCurrentSimTime(),
                        minDemandServiceDuration, getId(), onDemandVehicle.getId()));

        die();
    }

    // TODO rework
    @Override
    public void tripStarted(OnDemandVehicle vehicle) {
        if(state == DemandAgentState.DRIVING){
            try {
                throw new Exception(String.format("Demand Agent %s already driving in vehicle %s, it cannot be picked up by"
                        + "another vehicle %s", this, onDemandVehicle, vehicle));
            } catch (Exception ex) {
                Logger.getLogger(ParcelAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            state = DemandAgentState.DRIVING;
            realPickupTime = timeProvider.getCurrentSimTime();
            this.onDemandVehicle = vehicle;
        }
    }

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.PARCEL;
    }

    @Override
    public <T extends TransportEntity> T getTransportingEntity() {
        return (T) transportingEntity;
    }

    @Override
    public <T extends TransportEntity> void setTransportingEntity(T transportingEntity) {
        this.transportingEntity = transportingEntity;
    }

    @Override
    public void setLastFromPosition(SimulationNode lastTargetPosition) {
        this.lastFromPosition = lastFromPosition;
    }

    private void computeMinServiceDuration() {
        Trip<SimulationNode> minTrip = tripsUtil.createTrip(getPosition(), trip.getLastLocation());
        minDemandServiceDuration = tripsUtil.getTripDuration(minTrip);
    }

    public interface ParcelAgentFactory {
        public ParcelAgent create(String agentId, int id, TimeTrip<SimulationNode> osmNodeTrip);
    }
}
