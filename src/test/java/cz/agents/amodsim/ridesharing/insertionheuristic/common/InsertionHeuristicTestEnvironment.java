/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.insertionheuristic.common;

import cz.agents.amodsim.ridesharing.RidesharingEventData;
import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.EventOrderStorage;
import cz.agents.amodsim.ridesharing.RidesharingTestEnvironment;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.init.EventInitializer;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;

/**
 *
 * @author David Fiedler
 */
public class InsertionHeuristicTestEnvironment implements RidesharingTestEnvironment{
	
	public final AmodsimConfig config;
	
	private final Injector injector;

	@Override
	public Injector getInjector() {
		return injector;
	}
	
	

	public InsertionHeuristicTestEnvironment() {
		config = new AmodsimConfig();
        
        File localConfigFile = null;

        // Guice configuration
        AgentPolisInitializer agentPolisInitializer 
				= new AgentPolisInitializer(new TestModule(config, localConfigFile));
        injector = agentPolisInitializer.initialize();
		
		// config changes
		config.ridesharing.batchPeriod = 0;
		config.ridesharing.maximumRelativeDiscomfort = 2.0;
		config.ridesharing.discomfortConstraint = "relative";
	}
	
	
	
	
	
	@Override
	public void run(Graph<SimulationNode, SimulationEdge> graph, List<TimeTrip<SimulationNode>> trips,
			List<SimulationNode> vehicalInitPositions, List<RidesharingEventData> expectedEvents) throws Throwable{

        SimulationCreator creator = injector.getInstance(SimulationCreator.class);

        // prepare map
		MapInitializer mapInitializer = injector.getInstance(SimpleMapInitializer.class);
		creator.prepareSimulation(mapInitializer.getMap());
		
		// requests
		injector.getInstance(EventInitializer.class).initialize(trips, new ArrayList<>());
        
		// vehicles
		OnDemandVehicleFactorySpec onDemandVehicleFactory = injector.getInstance(OnDemandVehicleFactorySpec.class);
		OnDemandVehicleStorage onDemandVehicleStorage = injector.getInstance(OnDemandVehicleStorage.class);
		int counter = 0;
		for (SimulationNode vehiclePosition: vehicalInitPositions) {
			String onDemandVehicelId = String.format("%s", counter);
			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, vehiclePosition);
			onDemandVehicleStorage.addEntity(newVehicle);
			counter++;
        }
		
		EventOrderStorage eventOrderStorage = injector.getInstance(EventOrderStorage.class);
        
        creator.startSimulation();
		
		
		// TESTING EVENT ORDER
		List<Event> realEvents = eventOrderStorage.getOnDemandVehicleEvents();
		
		Assert.assertEquals("Event count", expectedEvents.size(), realEvents.size());
		Iterator<RidesharingEventData> expectedEventsIterator = expectedEvents.iterator();
		counter = 1;
		for(Event event: realEvents){
			RidesharingEventData expectedEvent = expectedEventsIterator.next();
			OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
			
			Assert.assertEquals(
					String.format("%s. event vehicle", counter), 
					expectedEvent.onDemandVehicleId, eventContent.getOnDemandVehicleId());
			Assert.assertEquals(
					String.format("%s. event demand", counter),
					expectedEvent.demandId, eventContent.getDemandId());
			Assert.assertEquals(
					String.format("%s. event type", counter),
					expectedEvent.eventType, event.getType());
			counter++;
		}
    }

	@Override
	public AmodsimConfig getConfig() {
		return config;
	}
	
	
}
