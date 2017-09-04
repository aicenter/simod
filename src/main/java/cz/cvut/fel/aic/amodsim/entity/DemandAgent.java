/*
 */
package cz.cvut.fel.aic.amodsim.entity;

import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
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
import cz.cvut.fel.aic.amodsim.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.amodsim.statistics.StatisticEvent;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements EventHandler, TransportableEntity {
    
    private final int simpleId;
	
	private final TimeTrip<Long> osmNodeTrip;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
	
	private final boolean precomputedPaths;
    
    private final EventProcessor eventProcessor;
    
    private final DemandStorage demandStorage;
    
    private final Map<Long,SimulationNode> nodesMappedByNodeSourceIds;
    
    private final StandardTimeProvider timeProvider;
    
    
    private DemandAgentState state;
    
    private PhysicalVehicle vehicle;
    
    private OnDemandVehicle onDemandVehicle;
    
    private Long demandTime;
    
    private TransportEntity transportEntity;
    
    private SimulationNode lastFromPosition;

    
    
    
    public int getSimpleId() {
        return simpleId;
    }

    
    
    
    public DemandAgentState getState() {
        return state;
    }

    public PhysicalVehicle getVehicle() {
        return vehicle;
    }

    public OnDemandVehicle getOnDemandVehicle() {
        return onDemandVehicle;
    }
    
    


    
    
    
    @Inject
	public DemandAgent(OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, EventProcessor eventProcessor, 
            DemandStorage demandStorage, Map<Long,SimulationNode> nodesMappedByNodeSourceIds, StandardTimeProvider timeProvider,
            @Named("precomputedPaths") boolean precomputedPaths, @Assisted String agentId, @Assisted int id,
            @Assisted TimeTrip<Long> osmNodeTrip) {
		super(agentId, nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().get(0)));
        this.simpleId = id;
		this.osmNodeTrip = osmNodeTrip;
		this.precomputedPaths = precomputedPaths;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.eventProcessor = eventProcessor;
        this.demandStorage = demandStorage;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
        this.timeProvider = timeProvider;
        state = DemandAgentState.WAITING;
	}

    
    

	@Override
	public void born() {
        demandStorage.addEntity(this);
		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null, 
                new DemandData(osmNodeTrip.getLocations(), this));
        demandTime = timeProvider.getCurrentSimTime();
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
        if(!getPosition().equals(nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().getLast()))){
            try {
                throw new Exception("Demand not served properly");
            } catch (Exception ex) {
                Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        eventProcessor.addEvent(StatisticEvent.DEMAND_PICKED_UP, null, null, 
                new DemandServiceStatistic(demandTime, timeProvider.getCurrentSimTime()));
        die();
    }

    public void tripStarted() {
        state = DemandAgentState.DRIVING;
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

    
    
    
    public interface DemandAgentFactory {
        public DemandAgent create(String agentId, int id, TimeTrip<Long> osmNodeTrip);
    }
	
}
