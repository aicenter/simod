/*
 */
package cz.agents.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.agents.amodsim.DemandData;
import cz.agents.amodsim.DemandSimulationEntityType;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.storage.DemandStorage;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.time.TimeProvider;
import cz.agents.agentpolis.simmodel.Agent;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.statistics.DemandServiceStatistic;
import cz.agents.amodsim.statistics.StatisticEvent;
import cz.agents.basestructures.Node;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements EventHandler {
	
	private final TimeTrip<Long> osmNodeTrip;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
	
	private final boolean precomputedPaths;
    
    private final EventProcessor eventProcessor;
    
    private final DemandStorage demandStorage;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;
    
    private final TimeProvider timeProvider;
    
    
    private DemandAgentState state;
    
    private Vehicle vehicle;
    
    private OnDemandVehicle onDemandVehicle;
    
    private Long demandTime;

    
    
    
    
    public DemandAgentState getState() {
        return state;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public OnDemandVehicle getOnDemandVehicle() {
        return onDemandVehicle;
    }
    
    


    
    
    
    @Inject
	public DemandAgent(OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, EventProcessor eventProcessor, 
            DemandStorage demandStorage, Map<Long,Node> nodesMappedByNodeSourceIds, TimeProvider timeProvider,
            @Named("precomputedPaths") boolean precomputedPaths, @Assisted String agentId,
            @Assisted TimeTrip<Long> osmNodeTrip) {
		super(agentId, DemandSimulationEntityType.DEMAND);
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
        setPosition(nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().get(0)));
		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null, 
                new DemandData(osmNodeTrip.getLocations(), this));
        demandTime = timeProvider.getCurrentSimTime();
	}

    @Override
    public void die() {
        demandStorage.removeEntity(this);
    }
    
    

	@Override
	public DescriptionImpl getDescription() {
		return null;
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
        state = DemandAgentState.RIDING;
    }

    
    
    
    public interface DemandAgentFactory {
        public DemandAgent create(String agentId, TimeTrip<Long> osmNodeTrip);
    }
	
}
