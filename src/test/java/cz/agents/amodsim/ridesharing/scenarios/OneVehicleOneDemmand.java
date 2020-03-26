/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.agents.amodsim.ridesharing.scenarios;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.RidesharingEventData;
import cz.agents.amodsim.ridesharing.RidesharingTestEnvironment;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author David Fiedler
 */
public class OneVehicleOneDemmand {
	
	
	public void run(RidesharingTestEnvironment testEnvironment) throws Throwable{
		// bootstrap Guice
		Injector injector = testEnvironment.getInjector();
		
		// set roadgraph
		Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(3, injector.getInstance(Transformer.class), 1);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(1000, graph.getNode(1), graph.getNode(2)));
		
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(0));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DROP_OFF));
		
		testEnvironment.run(graph, trips, vehicalInitPositions, expectedEvents);
	}
}
