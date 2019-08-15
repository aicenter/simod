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
//		{16955, 19314}, 
//{4657, 17102},
		//geting sample nodes
		int[][] indexPairs = {{15689,15688}, {15688,14923}, {14923,13544}, {13544,14922}, {14922,4360}, {4360,13546}, 
			{13546,4346}, {4346,4362}, {4362,4377}, {4377,4366}, {4366,28111}, {28111,4372}, {4372,4370}, {4370,4368},
			{4368,28399}, {28399,28407}, {28407,27937}, {27937,25418}, {25418,27935}, {27935,27936}, {27936,27085},
			{27085,27084}, {27084,24833}, {24833,26457}, {26457,26411}, {26411,7040}, {7040,26409}, {26409,10900}, 
			{10900,26244}, {26244,1997}, {1997,14005}, {14005,13957}, {13957,26412}, {26412,21106}, {21106,329}, 
			{329,328}, {328,5190}, {5190,23681}, {23681,23682}, {23682,24248}, {24248,23686}, {23686,23685}, 
			{23685,23673}, {23673,26006}, {26006,26007}, {26007,26008}, {26008,22706}, {22706,26003}, {26003,11181},
			{11181,1495}, {1495,10641}, {10641,21791}, {21791,21792}, {21792,24530}, {24530,22707}, {22707,11188}, 
			{11188,16650}, {16650,25994}, {25994,11870}, {11870,19355}, {19355,11869}, {11869,26447}, {26447,26465},
			{26465,26468}, {26468,27129}, {27129,26996}, {26996,26997}, {26997,27128}, {27128,26445}, {26445,26446}, 
			{26446,26444}, {26444,19541}, {19541,11885}, {11885,11883}, {11883,19540}, {19540,19539}, {19539,20380}, 
			{20380,20381}, {20381,11882}, {11882,11880}, {11880,11879}, {11879,168}, 
			
			{27, 28}, {28, 29}, {29, 30}, {54, 1187}, {15689, 168}, {22560, 21115}, {26703, 22224}, {23512, 19352}, {18668, 15085},
			{4633, 21024}, {22140, 10346}, {15326, 26730}, {2631, 8622}, {9267, 1733}, {20395, 16498}, {4022, 5426},
			{6867, 24091}, {12518, 27633}, {11423, 26684}, {20612, 10251}, {17653, 5138}, {11941, 13456}, {3368, 11997},
			{20132, 4090}, {2116, 15599}, {17876, 13775}, {25560, 8009}, {16713, 11898}, {22836, 11618}, {3531, 24351},
			{5427, 10356}, {3112, 2117}, {9971, 16605}, {11939, 16629}, {24479, 2175}, {19828, 17491}, {26011, 6643},
			{20601, 23700}, {18019, 7751}, {6594, 18624}, {17149, 21195}, {6187, 15303}, {18638, 10200}, {22181, 8446},
			{6051, 17246}, {12077, 6504}, {19843, 5916}, {6349, 19617}, {12254, 20646}, {11247, 5619}, {16686, 24345}, 
			{24182, 5026}, {19796, 6395}, {12488, 21820}, {15233, 8186}, {2945, 19956}, {42, 5297}, 
			{14288, 10222}, {22187, 7047}, {492, 12083}, {10338, 27188}, {5769, 1379}, {24753, 4394}, {16652, 1600},
			{6470, 18408}, {10680, 4348}, {26827, 16311}, {19960, 3195}, {21970, 25790}, {2135, 26379},
			{18362, 10159}, {12758, 18813}, {19860, 14746}, {24763, 10913}, {752, 5242}, {8879, 21476}, {25301, 16390},
			{872, 22998}, {5026, 26731}, {17656, 14746}, {24434, 22792}, {24350, 10147}, {13566, 4153}, {19380, 18341},
			{15727, 26578}, {11820, 6294}, {8651, 22471}, {23268, 1618}, {21853, 13592}, {10201, 12315}, {26673, 10922}, 
			{7801, 23589}, {26391, 14967}, {7783, 17028}, {9970, 15818}, {22247, 15913}, {21496, 4838}, {24724, 4103},
			{22593, 19452}, {9841, 10982}, {2314, 9757},  {9724, 20574}};
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
			double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
			double durationDm = distanceMatrixTravelTimeProvider.getExpectedTravelTime(from, to);
			LOGGER.debug("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", from, 
					from.getIndex(), to, to.getIndex(), durationAstar, durationDm, durationAstar - durationDm);
//			Assert.assertEquals(distanceAstar, distanceDm, 19000);
		}
			
	}				
}
