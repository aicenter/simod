/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.vga.common.TestModuleNoVisio;
import cz.agents.amodsim.ridesharing.vga.mock.TestOptimalPlanVehicle;
import cz.agents.amodsim.ridesharing.vga.mock.TestPlanRequest;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.DelayData;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.NodesMappedByIndex;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.File;
import java.util.LinkedHashSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import cz.agents.amodsim.ridesharing.vga.mock.TestOnDemandVehicle;
import cz.agents.amodsim.ridesharing.vga.mock.TestTimeProvider;

/**
 *
 * @author F.I.D.O.
 */
public class ArrayOptimalVehiclePlanFinderTest {
	
	private static ArrayOptimalVehiclePlanFinder arrayOptimalVehiclePlanFinder;
	
	private static Injector injector;
	
	private static AmodsimConfig config;
	
	@BeforeClass
	public static void prepare(){
		config = new AmodsimConfig();
		
		File localConfigFile = null;

		// Guice configuration
		AgentPolisInitializer agentPolisInitializer 
				= new AgentPolisInitializer(new TestModuleNoVisio(config, localConfigFile));
		injector = agentPolisInitializer.initialize();
		
		// config changes
		config.ridesharing.batchPeriod = 0;
		config.ridesharing.maxProlongationInSeconds = 240;
		
		
	}
	
	@Test
	public void testRealMap(){
		
		// set simulation time 
		injector.getInstance(TestTimeProvider.class).setSimulationTime(180000);
		
		// prepare map
		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
		MapData mapData = mapInitializer.getMap();
		injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
		injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
		
		arrayOptimalVehiclePlanFinder = injector.getInstance(ArrayOptimalVehiclePlanFinder.class);
		
		NodesMappedByIndex nodesMappedByIndex = injector.getInstance(NodesMappedByIndex.class);
		
		TravelTimeProvider travelTimeProvider = injector.getInstance(TravelTimeProvider.class);
		
		// prepare test requests
		TestPlanRequest request0 = new TestPlanRequest(
				668, config, nodesMappedByIndex.getNodeByIndex(6901), nodesMappedByIndex.getNodeByIndex(15140), 72, true,
				travelTimeProvider);
		TestPlanRequest request1 = new TestPlanRequest(
				1628, config, nodesMappedByIndex.getNodeByIndex(6901), nodesMappedByIndex.getNodeByIndex(15140), 132, true,
				travelTimeProvider);
		TestPlanRequest request2 = new TestPlanRequest(
				33, config, nodesMappedByIndex.getNodeByIndex(1968), nodesMappedByIndex.getNodeByIndex(21709), 5, false,
				travelTimeProvider);
		LinkedHashSet<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
		onBoardRequests.add(request0);
		onBoardRequests.add(request1);
		LinkedHashSet<TestPlanRequest> requests = new LinkedHashSet<>();
		requests.add(request1);
		requests.add(request2);
				
		// prepare test vehicle
		SimulationNode position = nodesMappedByIndex.getNodeByIndex(6901);
		DelayData delayData = new DelayData(24235L, 177845L, 33633);
		SimulationNode targetNode = nodesMappedByIndex.getNodeByIndex(6949);
		TestOnDemandVehicle testOnDemandVehicle = new TestOnDemandVehicle(config, targetNode, delayData, position);
		TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, position, 
				config.ridesharing.vehicleCapacity, testOnDemandVehicle);
		
		Plan plan = arrayOptimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, requests, 180, false);
		
		Assert.assertNotNull(plan);
	}
	
//	@Test
//	public void testFailCase(){
//		// set roadgraph - grid 5x4
//		Graph<SimulationNode, SimulationEdge> graph 
//				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 4);
//		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
//		
//		// prepare test requests
//		LinkedHashSet<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
//		TestPlanRequest request0 = new TestPlanRequest(0, config, graph.getNode(16), graph.getNode(14), 3, false);
//		TestPlanRequest request1 = new TestPlanRequest(1, config, graph.getNode(17), graph.getNode(3), 1, true);
//		TestPlanRequest request2 = new TestPlanRequest(2, config, graph.getNode(5), graph.getNode(10), 4, false);
//		TestPlanRequest request4 = new TestPlanRequest(4, config, graph.getNode(11), graph.getNode(0), 8, false);
//		onBoardRequests.add(request1);
//		LinkedHashSet<TestPlanRequest> requests = new LinkedHashSet<>();
//		requests.add(request0);
//		requests.add(request1);
//		requests.add(request2);
//		requests.add(request4);
//				
//		// prepare test vehicle
//		SimulationNode position = graph.getNode(16);
//		TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, position, 
//				config.ridesharing.vehicleCapacity);
//		
//		Plan plan = arrayOptimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, requests, 8, false);
//		
//		Assert.assertNull(plan);
//	}
	
//	@Test
//	public void testSimpleDifferentTimeCase(){
//		// set roadgraph - grid 5x4
//		Graph<SimulationNode, SimulationEdge> graph 
//				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 4);
//		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
//		
//		// prepare test requests
//		LinkedHashSet<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
//		TestPlanRequest request0 = new TestPlanRequest(0, config, graph.getNode(16), graph.getNode(14), 3, false);
//		TestPlanRequest request1 = new TestPlanRequest(1, config, graph.getNode(17), graph.getNode(3), 1, true);
//		TestPlanRequest request2 = new TestPlanRequest(2, config, graph.getNode(5), graph.getNode(10), 4, false);
//		TestPlanRequest request4 = new TestPlanRequest(4, config, graph.getNode(11), graph.getNode(0), 8, false);
//		onBoardRequests.add(request1);
//		LinkedHashSet<TestPlanRequest> requests = new LinkedHashSet<>();
//		requests.add(request0);
//		requests.add(request1);
//		requests.add(request2);
//		requests.add(request4);
//				
//		// prepare test vehicle
//		SimulationNode position = graph.getNode(16);
//		TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, position, 
//				config.ridesharing.vehicleCapacity);
//		
//		Plan plan = arrayOptimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, requests, 8, false);
//		
//		Assert.assertNull(plan);
//	}
}
