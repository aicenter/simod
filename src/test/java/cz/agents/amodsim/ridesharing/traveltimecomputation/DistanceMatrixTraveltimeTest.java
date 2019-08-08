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
		
		//geting sample nodes
		int[][] indexPairs = {{54, 1187}, {15689, 168}, {22560, 21115}, {26703, 22224}};
		NodesMappedByIndex nodesMappedByIndex = injector.getInstance(NodesMappedByIndex.class);
		SimulationNode[][] nodePairs = new SimulationNode[indexPairs.length][2];
		for(int i = 0; i < indexPairs.length; i++){
			nodePairs[i][0] = nodesMappedByIndex.getNodeByIndex(indexPairs[i][0]);
			nodePairs[i][1] = nodesMappedByIndex.getNodeByIndex(indexPairs[i][1]);
		}
		
		// travel time providers
		AstarTravelTimeProvider astarTravelTimeProvider = injector.getInstance(AstarTravelTimeProvider.class);
		DistanceMatrixTravelTimeProvider distanceMatrixTravelTimeProvider 
				= injector.getInstance(DistanceMatrixTravelTimeProvider.class);
		
		for(int i = 0; i < indexPairs.length; i++){
			SimulationNode from = nodePairs[i][0];
			SimulationNode to = nodePairs[i][1];
			double distanceAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
			double distanceDm = distanceMatrixTravelTimeProvider.getExpectedTravelTime(from, to);
			LOGGER.debug("From {} to {}, astar distance: {}, dm distance: {}", from, to, distanceAstar, distanceDm);
			Assert.assertEquals(distanceAstar, distanceDm, 5000);
		}
			
	}				
}
