/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.visual.ridesharing.insertionheuristic.common;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.SimpleTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.entity.vehicle.SimpleMoDVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.init.RequestsInitializer;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.visual.ridesharing.EventOrderStorage;
import cz.cvut.fel.aic.simod.visual.ridesharing.RidesharingEventData;
import cz.cvut.fel.aic.simod.visual.ridesharing.RidesharingTestEnvironment;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;

/**
 *
 * @author David Fiedler
 */
public class InsertionHeuristicTestEnvironment implements RidesharingTestEnvironment{
	
	public final SimodConfig config;
	
	private final Injector injector;

	@Override
	public Injector getInjector() {
		return injector;
	}
        
	public InsertionHeuristicTestEnvironment() {
		config = new SimodConfig();
		
		File localConfigFile = null;

		// Guice configuration
		AgentPolisInitializer agentPolisInitializer
				= new AgentPolisInitializer(new TestModule(config, localConfigFile));
		injector = agentPolisInitializer.initialize();
                
		// config changes
		config.ridesharing.batchPeriod = 0;
		config.maxTravelTimeDelay.relative = 2.0;
		config.maxTravelTimeDelay.mode = "relative";
		config.stations.on = false;
	}
	
	@Override
	public void run(Graph<SimulationNode, SimulationEdge> graph, List<TimeTrip<SimulationNode>> trips,
			List<SimulationNode> vehicalInitPositions, List<RidesharingEventData> expectedEvents) throws Throwable{

		SimulationCreator creator = injector.getInstance(SimulationCreator.class);

		// prepare map
		MapInitializer mapInitializer = injector.getInstance(SimpleMapInitializer.class);
		creator.prepareSimulation(mapInitializer.getMap());
		
		// requests
		injector.getInstance(RequestsInitializer.class).initialize();
                
		// vehicles
		OnDemandVehicleFactorySpec onDemandVehicleFactory = injector.getInstance(OnDemandVehicleFactorySpec.class);
		OnDemandVehicleStorage onDemandVehicleStorage = injector.getInstance(OnDemandVehicleStorage.class);
		int counter = 0;               
                                                            
		for (SimulationNode vehiclePosition: vehicalInitPositions) {
			String onDemandVehicelId = String.format("%s", counter);

			// physical vehicle creation
			SimpleMoDVehicle vehicle = new SimpleMoDVehicle(
				onDemandVehicelId + " - vehicle",
				DemandSimulationEntityType.VEHICLE,
				2,
				EGraphType.HIGHWAY,
				vehiclePosition,
				30,
				config.ridesharing.vehicleCapacity
			);

			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(
				onDemandVehicelId, vehiclePosition, vehicle, null, null, OnDemandVehicleState.WAITING
			);
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
	public SimodConfig getConfig() {
		return config;
	}
	
	
}
