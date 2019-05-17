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

/**
 *
 * @author David Fiedler
 */
public class Weight1 {
	

    public void run(RidesharingTestEnvironment testEnvironment) throws Throwable{
		// bootstrap Guice
		Injector injector = testEnvironment.getInjector();
		
		// config
		testEnvironment.getConfig().ridesharing.weightParameter = 1.0;
		testEnvironment.getConfig().ridesharing.maximumRelativeDiscomfort = 3.0;
		
		// set roadgraph - grid 5x4
        Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(4, injector.getInstance(Transformer.class), 2);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		// demand trips
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(graph.getNode(1), graph.getNode(6), 1000));
		trips.add(new TimeTrip<>(graph.getNode(5), graph.getNode(7), 3000));
		
		// vehicles
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(0));
		vehicalInitPositions.add(graph.getNode(4));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("1", 1, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("1", 1, OnDemandVehicleEvent.DROP_OFF));
        
        testEnvironment.run(graph, trips, vehicalInitPositions, expectedEvents);
    }
}
