/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
		
		// set roadgraph - grid 5x4
		Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 4);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		// demand trips
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(graph.getNode(17), graph.getNode(3), 1000));
		trips.add(new TimeTrip<>(graph.getNode(16), graph.getNode(14), 3000));
		trips.add(new TimeTrip<>(graph.getNode(5), graph.getNode(10), 4000));
		trips.add(new TimeTrip<>(graph.getNode(12), graph.getNode(9), 7000));
		trips.add(new TimeTrip<>(graph.getNode(11), graph.getNode(0), 8000));
		
		// vehicles
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(15));
		vehicalInitPositions.add(graph.getNode(0));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 2, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 2, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 3, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 4, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 3, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("1", 4, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DROP_OFF));
		
		testEnvironment.run(graph, trips, vehicalInitPositions, expectedEvents);
	}
}
