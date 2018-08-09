/*
 */
package cz.cvut.fel.aic.amodsim.entity;

import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.storage.DemandStorage;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandSimulationEntityType;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.amodsim.statistics.StatisticEvent;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements EventHandler, TransportableEntity {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DemandAgent.class);
    
    private final int simpleId;
	
	private final TimeTrip<SimulationNode> trip;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
	
	private final boolean precomputedPaths;
    
    private final EventProcessor eventProcessor;
    
    private final DemandStorage demandStorage;
    
    private final Map<Long,SimulationNode> nodesMappedByNodeSourceIds;
    
    private final StandardTimeProvider timeProvider;
	
	private final Statistics statistics;
	
	private final TripsUtil tripsUtil;
    
    
    private DemandAgentState state;
    
    private OnDemandVehicle vehicle;
    
    private OnDemandVehicle onDemandVehicle;
    
    private long demandTime;
    
    private TransportEntity transportEntity;
    
    private SimulationNode lastFromPosition;
	
	private long scheduledPickupDelay;
	
	private long realPickupTime = 0;
	
	private long minDemandServiceDuration;
	
	// only to save compuatational time it|s 
//	private long currentServiceDuration;
	
	private boolean dropped;

    
    
    
    public int getSimpleId() {
        return simpleId;
    }

	public void setScheduledPickupDelay(long scheduledPickupDelay) {
		this.scheduledPickupDelay = scheduledPickupDelay;
	}

	public long getScheduledPickupDelay() {
		return scheduledPickupDelay;
	}

	public long getRealPickupTime() {
		return realPickupTime;
	}

	public long getDemandTime() {
		return demandTime;
	}

    public DemandAgentState getState() {
        return state;
    }

    public OnDemandVehicle getVehicle() {
        return vehicle;
    }

    public OnDemandVehicle getOnDemandVehicle() {
        return onDemandVehicle;
    }

	public long getMinDemandServiceDuration() {
		return minDemandServiceDuration;
	}

	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	public boolean isDropped() {
		return dropped;
	}
	
	

    
    
    
    @Inject
	public DemandAgent(OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, EventProcessor eventProcessor, 
            DemandStorage demandStorage, Map<Long,SimulationNode> nodesMappedByNodeSourceIds, 
			StandardTimeProvider timeProvider, Statistics statistics, TripsUtil tripsUtil,
            @Named("precomputedPaths") boolean precomputedPaths, @Assisted String agentId, @Assisted int id,
            @Assisted TimeTrip<SimulationNode> trip) {
		super(agentId, trip.getLocations().get(0));
        this.simpleId = id;
		this.trip = trip;
		this.precomputedPaths = precomputedPaths;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.eventProcessor = eventProcessor;
        this.demandStorage = demandStorage;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
        this.timeProvider = timeProvider;
		this.statistics = statistics;
		this.tripsUtil = tripsUtil;
        state = DemandAgentState.WAITING;
		dropped = false;
	}

    
    

	@Override
	public void born() {
        demandStorage.addEntity(this);
		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null, 
                new DemandData(trip.getLocations(), this));
        demandTime = timeProvider.getCurrentSimTime();
		computeMinServiceDuration();
	}

    @Override
    public void die() {
        demandStorage.removeEntity(this);
    }

    @Override
    public EventProcessor getEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleEvent(Event event) {
        onDemandVehicle = (OnDemandVehicle) event.getContent();
//        vehicle = onDemandVehicle.getVehicle();
//        rideAsPassengerActivity.usingVehicleAsPassenger(this.getId(), onDemandVehicle.getVehicleId(), 
//                onDemandVehicle.getDemandTrip(this), this);
    }


    public void tripEnded() {
        if(!getPosition().equals(trip.getLocations().getLast())){
            try {
                throw new Exception("Demand not served properly");
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            }
        }
        eventProcessor.addEvent(StatisticEvent.DEMAND_DROPPED_OFF, null, null, 
                new DemandServiceStatistic(demandTime, realPickupTime, timeProvider.getCurrentSimTime(), 
						minDemandServiceDuration,
						getId(), vehicle.getId()));
		
        die();
    }

    public void tripStarted(OnDemandVehicle vehicle) {
        state = DemandAgentState.DRIVING;
		realPickupTime = timeProvider.getCurrentSimTime();
		this.vehicle = vehicle;
    }

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.DEMAND;
    }

    @Override
    public <T extends TransportEntity> T getTransportingEntity() {
        return (T) transportEntity;
    }

    @Override
    public <T extends TransportEntity> void setTransportingEntity(T transportingEntity) {
        this.transportEntity = transportingEntity;
    }

    @Override
    public void setLastFromPosition(SimulationNode lastFromPosition) {
        this.lastFromPosition = lastFromPosition;
    }

	private void computeMinServiceDuration() {
		Trip<SimulationNode> minTrip = tripsUtil.createTrip(getPosition().id, trip.getLocations().getLast().id);
		minDemandServiceDuration = (long) (tripsUtil.getTripDurationInSeconds(minTrip) * 1000);
	}

    
    
    
    public interface DemandAgentFactory {
        public DemandAgent create(String agentId, int id, TimeTrip<SimulationNode> osmNodeTrip);
    }
	
}
