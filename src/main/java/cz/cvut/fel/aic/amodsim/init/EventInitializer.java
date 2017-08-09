/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.config.Config;
import java.util.List;
import java.util.Random;

/**
 *
 * @author fido
 */
@Singleton
public class EventInitializer {
//    private static final double TRIP_MULTIPLICATION_FACTOR = 2.573;
//    private static final double TRIP_MULTIPLICATION_FACTOR = 13.63;
//    private static final double TRIP_MULTIPLICATION_FACTOR = 1.615;
//    private static final double TRIP_MULTIPLICATION_FACTOR = 3.433;
    
    private static final long TRIP_MULTIPLICATION_TIME_SHIFT = 60000;
    
    private static final long MAX_EVENTS = 0;
    
    private static final int RANDOM_SEED = 1;
    
    
    
    
    private final EventProcessor eventProcessor;

    private final DemandEventHandler demandEventHandler;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final Config config;
    
    
    private long eventCount;
    
    
    @Inject
    public EventInitializer(EventProcessor eventProcessor, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, Config config, 
            DemandEventHandler demandEventHandler) {
        this.eventProcessor = eventProcessor;
        this.demandEventHandler = demandEventHandler;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.config = config;
        eventCount = 0;
    }
    
    
    public void initialize(List<TimeTrip<Long>> osmNodeTrips, List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips, 
            Config config){
        Random random = new Random(RANDOM_SEED);
        
        for (TimeTrip<Long> osmNodeTrip : osmNodeTrips) {
            long startTime = osmNodeTrip.getStartTime() - config.agentpolis.startTime;
            if(startTime < 1 || startTime > config.agentpolis.simulationDurationInMillis){
                continue;
            }
            
            for(int i = 0; i < config.tripsMultiplier; i++){
                if(i + 1 >= config.tripsMultiplier){
                    double randomNum = random.nextDouble();
                    if(randomNum > config.tripsMultiplier - i){
                        break;
                    }
                }
                
                startTime = startTime + i * TRIP_MULTIPLICATION_TIME_SHIFT;
                eventProcessor.addEvent(null, demandEventHandler, null, osmNodeTrip, startTime);
                eventCount++;
                if(MAX_EVENTS != 0 && eventCount >= MAX_EVENTS){
                    return;
                }
            }
        }
        for (TimeTrip<OnDemandVehicleStation> rebalancingTrip : rebalancingTrips) {
            long startTime = rebalancingTrip.getStartTime() - config.agentpolis.startTime;
            if(startTime < 1 || startTime > config.agentpolis.simulationDurationInMillis){
                continue;
            }
            eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.REBALANCING, onDemandVehicleStationsCentral, 
                    null, rebalancingTrip, startTime);
        }
    }
    
    
    
    
    public static class DemandEventHandler extends EventHandlerAdapter{
		
		private final IdGenerator demandIdGenerator;
 
        private final DemandAgentFactory demandAgentFactory;
        
        private final SimulationCreator simulationCreator;
        
        
        
        
        @Inject
        public DemandEventHandler(IdGenerator demandIdGenerator, DemandAgentFactory demandAgentFactory,
                SimulationCreator simulationCreator) {
            this.demandIdGenerator = demandIdGenerator;
            this.demandAgentFactory = demandAgentFactory;
            this.simulationCreator = simulationCreator;
        }

        
		
		

		@Override
		public void handleEvent(Event event) {
			TimeTrip<Long> osmNodeTrip = (TimeTrip<Long>) event.getContent();
            
            int id = demandIdGenerator.getId();
			
			DemandAgent demandAgent = demandAgentFactory.create("Demand " + Integer.toString(id), id, osmNodeTrip);
            
            demandAgent.born();
			
//			simulationCreator.addAgent(demandAgent);
		}
	}    
}
