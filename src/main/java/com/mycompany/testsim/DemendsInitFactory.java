/*
 */
package com.mycompany.testsim;

import com.google.inject.Injector;
import com.mycompany.testsim.io.Trip;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.creator.initializator.InitFactory;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author F-I-D-O
 */
public class DemendsInitFactory implements InitFactory {
	
	private final List<Trip<Long>> osmNodeTrips; 
	
	private final DemandEventHandler eventHandler;
	
	private final SimulationCreator simulationCreator;
	
	private Injector injector;
	
	
	
	
	public DemendsInitFactory(List<Trip<Long>> osmNodeTrips, SimulationCreator simulationCreator){
		this.osmNodeTrips = osmNodeTrips;
		eventHandler = new DemandEventHandler();
		this.simulationCreator = simulationCreator;
	}

	@Override
	public void initRestEnvironment(Injector injector) {
		this.injector = injector;
		
		EventProcessor eventProcessor = injector.getInstance(EventProcessor.class);
		
		eventHandler.init();
		
		for (Trip<Long> osmNodeTrip : osmNodeTrips) {
			eventProcessor.addEvent(null, eventHandler, null, osmNodeTrip, osmNodeTrip.getStartTime());
		}
	}
	
	
	
	private class DemandEventHandler extends EventHandlerAdapter{
		
		private long idCounter = 0;
		
		private Map<Long,Integer> nodeIdsMappedByNodeSourceIds;
		
		private ShortestPathPlanner pathPlanner;

		public void init() {
			this.nodeIdsMappedByNodeSourceIds 
					= injector.getInstance(HighwayNetwork.class).getNetwork().createSourceIdToNodeIdMap();
			pathPlanner = getPathPlanner();
		}
		
		

		@Override
		public void handleEvent(Event event) {
			Trip<Long> osmNodeTrip = (Trip<Long>) event.getContent();
			
			DemandAgent demandAgent = new DemandAgent(Long.toString(idCounter++), DemandSimulationEntityType.DEMAND, 
					osmNodeTrip, injector, nodeIdsMappedByNodeSourceIds, pathPlanner);
			
			simulationCreator.addAgent(demandAgent);
			
		}
		
		private ShortestPathPlanner getPathPlanner(){
			Set<GraphType> allowedGraphTypes = new HashSet<>();
			allowedGraphTypes.add(EGraphType.HIGHWAY);
			return ShortestPathPlanner.createShortestPathPlanner(injector, allowedGraphTypes);
		}
		
	}
}
