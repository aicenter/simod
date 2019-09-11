/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.NodesMappedByIndex;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.DistanceMatrixTravelTimeProvider;
import java.io.File;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
public class DistanceMatrixTraveltimeTest {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixTraveltimeTest.class);
	
	@Test
	public void test(){
		AmodsimConfig config = new AmodsimConfig();
		// Guice configuration
		AgentPolisInitializer agentPolisInitializer 
				= new AgentPolisInitializer(new TestModule(config));
		Injector injector = agentPolisInitializer.initialize();
		
		// config changes
		
		// prepare map
		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
		MapData mapData = mapInitializer.getMap();
		injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
		injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
                //Map<Integer, SimulationNode> map = injector.getInstance(AllNetworkNodes.class).getAllNetworkNodes();
                NodesMappedByIndex nodesMappedByIndex = injector.getInstance(NodesMappedByIndex.class);
                // travel time providers
		AstarTravelTimeProvider astarTravelTimeProvider = injector.getInstance(AstarTravelTimeProvider.class);
		/*TestDistanceMatrixTravelTimeProvider distanceMatrixTravelTimeProvider 
				= injector.getInstance(TestDistanceMatrixTravelTimeProvider.class);
                
                for (int i = 0; i < map.size(); i++) {
                    SimulationNode from = map.get(i);
                    for (int j = 0; j < map.size(); j++) {
                        SimulationNode to = map.get(i);
                       	double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
			double durationDm = distanceMatrixTravelTimeProvider.getExpectedTravelTime(from, to);
			LOGGER.debug("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", from, 
					from.getIndex(), to, to.getIndex(), durationAstar, durationDm, durationAstar - durationDm);
//			Assert.assertEquals(distanceAstar, distanceDm, 19000);
                    }
                }	*/		
	}				
}
