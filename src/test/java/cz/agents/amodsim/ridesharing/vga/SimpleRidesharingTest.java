/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.RidesharingEventData;
import cz.agents.amodsim.ridesharing.vga.common.VGASystemTestScenario;
import cz.cvut.fel.aic.agentpolis.VisualTests;
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
public class SimpleRidesharingTest {
	
	@Test
    public void run() throws Throwable{
		// bootstrap Guice
		VGASystemTestScenario scenario = new VGASystemTestScenario();
		Injector injector = scenario.getInjector();
		
		// set roadgraph
        Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 1);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		trips.add(new TimeTrip<>(graph.getNode(1), graph.getNode(3), 1000));
		trips.add(new TimeTrip<>(graph.getNode(2), graph.getNode(4), 1000));
		
		List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(0));
		
		// expected events
		List<RidesharingEventData> expectedEvents = new LinkedList<>();
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.PICKUP));
		expectedEvents.add(new RidesharingEventData("0", 0, OnDemandVehicleEvent.DROP_OFF));
		expectedEvents.add(new RidesharingEventData("0", 1, OnDemandVehicleEvent.DROP_OFF));
        
        scenario.run(graph, trips, vehicalInitPositions, expectedEvents);
    }
	
	public static void main(String[] args) {
        VisualTests.runVisualTest(SimpleRidesharingTest.class);
    }
}
