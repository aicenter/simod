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
package cz.cvut.fel.aic.simod.visual.ridesharing.scenarios;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.visual.ridesharing.RidesharingEventData;
import cz.cvut.fel.aic.simod.visual.ridesharing.RidesharingTestEnvironment;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author LocalAdmin
 */
public class DroppingScenario {
	
	@Test
	public void run(RidesharingTestEnvironment testEnvironment) throws Throwable{
		// bootstrap Guice
		Injector injector = testEnvironment.getInjector();
		
		// set roadgraph
		Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(10, injector.getInstance(Transformer.class), 1);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(0,
			ZonedDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneId.systemDefault()), graph.getNode(1), graph.getNode(3)));
		trips.add(new TimeTrip<>(1, ZonedDateTime.ofInstant(Instant.ofEpochSecond(2), ZoneId.systemDefault()), graph.getNode(9), graph.getNode(8)));
		
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(0));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DROP_OFF));
		
		testEnvironment.run(graph, trips, vehicalInitPositions, expectedEvents);
	}
}
