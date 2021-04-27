/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class ComplexScenario {
	
	@Test
	public void run(RidesharingTestEnvironment testEnvironment) throws Throwable{
		// bootstrap Guice
		Injector injector = testEnvironment.getInjector();
		
		// config
		testEnvironment.getConfig().ridesharing.maximumRelativeDiscomfort = 3.0;
                testEnvironment.getConfig().ridesharing.weightParameter = 0.2;
		
		// set roadgraph - grid 5x4
		Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 4);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		// demand trips
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(0,1000, graph.getNode(17), graph.getNode(3)));
		trips.add(new TimeTrip<>(1,3000, graph.getNode(16), graph.getNode(14)));
		trips.add(new TimeTrip<>(2,4000, graph.getNode(5), graph.getNode(10)));
		trips.add(new TimeTrip<>(3,7000, graph.getNode(12), graph.getNode(9)));
		trips.add(new TimeTrip<>(4,8000, graph.getNode(11), graph.getNode(0)));
		
		// vehicles
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(15));
		vehicalInitPositions.add(graph.getNode(0));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.DEMAND_PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 2, OnDemandVehicleEvent.DEMAND_PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DEMAND_PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 2, OnDemandVehicleEvent.DEMAND_DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 3, OnDemandVehicleEvent.DEMAND_PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 4, OnDemandVehicleEvent.DEMAND_PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.DEMAND_DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 3, OnDemandVehicleEvent.DEMAND_DROP_OFF));
		expectedEvents.add(new RidesharingEventData("1", 4, OnDemandVehicleEvent.DEMAND_DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DEMAND_DROP_OFF));
		
		testEnvironment.run(graph, trips, vehicalInitPositions, expectedEvents);
	}
}
