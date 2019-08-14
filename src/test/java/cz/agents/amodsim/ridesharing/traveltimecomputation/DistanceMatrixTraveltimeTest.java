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
		int[][] indexPairs = {{22560,13974}, {13974,13975}, {13975,13971}, {13971,13978}, {13978,13973}, {13973,13998}, 
			{13998,24540}, {24540,13996}, {13996,407}, {407,406}, {406,6996}, {6996,26518}, {26518,328}, {328,5190}, 
			{5190,23681}, {23681,13959}, {13959,23659}, {23659,7869}, {7869,23690}, {23690,23689}, {23689,21333}, 
			{21333,26773}, {26773,15106}, {15106,12592}, {12592,10692}, {10692,10693}, {10693,24242}, {24242,18999}, 
			{18999,15227}, {15227,15228}, {15228,11793}, {11793,11794}, {11794,24832}, {24832,6718}, {6718,3837}, 
			{3837,3836}, {3836,3835}, {3835,3834}, {3834,3833}, {3833,3832}, {3832,6716}, {6716,25546}, {25546,6706}, 
			{6706,26396}, {26396,22945}, {22945,14089}, {14089,6147}, {6147,21744}, {21744,1846}, {1846,1850}, 
			{1850,20746}, {20746,27189}, {27189,27190}, {27190,27197}, {27197,27188}, {27188,21932}, {21932,1524}, 
			{1524,21938}, {21938,9314}, {9314,27824}, {27824,1531}, {1531,24585}, {24585,11363}, {11363,1536}, 
			{1536,28161}, {28161,916}, {916,13865}, {13865,1790}, {1790,857}, {857,13867}, {13867,2547}, {2547,23873}, 
			{23873,13880}, {13880,1033}, {1033,859}, {859,858}, {858,626}, {626,627}, {627,13695}, {13695,7379}, 
			{7379,21886}, {21886,622}, {622,391}, {391,207}, {207,208}, {208,14154}, {14154,23192}, {23192,23193}, 
			{23193,4288}, {4288,12958}, {12958,12959}, {12959,9965}, {9965,9966}, {9966,7635}, {7635,2610}, {2610,593}, 
			{593,7638}, {7638,7639}, {7639,6606}, {6606,2387}, {2387,3093}, {3093,12295}, {12295,24772}, {24772,7619}, 
			{7619,12290}, {12290,21481}, {21481,20754}, {20754,3016}, {3016,7604}, {7604,13660}, {13660,3089}, 
			{3089,2390}, {2390,19774}, {19774,6607}, {6607,6608}, {6608,7586}, {7586,2394}, {2394,3959}, {3959,17547}, 
			{17547,13658}, {13658,49}, {49,22751}, {22751,21880}, {21880,21878}, {21878,21879}, {21879,3314}, 
			{3314,3315}, {3315,22695}, {22695,21118}, {21118,22700}, {22700,12117}, {12117,12118}, {12118,16366}, 
			{16366,21116}, {21116,16301}, {16301,21115}, 
			
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
