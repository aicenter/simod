/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.DemandAgent;
import com.mycompany.testsim.entity.DemandAgent.DemandAgentFactory;
import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class EventInitializer {
    private final EventProcessor eventProcessor;

    private final DemandEventHandler demandEventHandler;
    
    private final SimulationCreator simulationCreator;
    
    private final DemandAgentFactory demandAgentFactory;
    
    
    
    @Inject
    public EventInitializer(EventProcessor eventProcessor, SimulationCreator simulationCreator,
            DemandAgentFactory demandAgentFactory) {
        this.eventProcessor = eventProcessor;
        this.simulationCreator = simulationCreator;
        this.demandEventHandler = new DemandEventHandler();
        this.demandAgentFactory = demandAgentFactory;
    }
    
    
    public void initialize(List<Trip<Long>> osmNodeTrips, List<Trip<OnDemandVehicleStation>> rebalancingTrips){
        for (Trip<Long> osmNodeTrip : osmNodeTrips) {
			eventProcessor.addEvent(null, demandEventHandler, null, osmNodeTrip, osmNodeTrip.getStartTime());
		}
    }
    
    public class DemandEventHandler extends EventHandlerAdapter{
		
		private long idCounter = 0;
        
		
		

		@Override
		public void handleEvent(Event event) {
			Trip<Long> osmNodeTrip = (Trip<Long>) event.getContent();
			
			DemandAgent demandAgent = demandAgentFactory.create(Long.toString(idCounter++), osmNodeTrip);
			
			simulationCreator.addAgent(demandAgent);
		}
	}
    
}
