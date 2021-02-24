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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.TestModuleNoVisio;
import java.io.File;
import org.junit.BeforeClass;

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
	
//	@Test
//	public void testRealMap(){
//		
//		// set simulation time 
//		injector.getInstance(TestTimeProvider.class).setSimulationTime(180000);
//		
//		// prepare map
//		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
//		MapData mapData = mapInitializer.getMap();
//		injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
//		injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
//		
//		arrayOptimalVehiclePlanFinder = injector.getInstance(ArrayOptimalVehiclePlanFinder.class);
//		
//		NodesMappedByIndex nodesMappedByIndex = injector.getInstance(NodesMappedByIndex.class);
//		
//		TravelTimeProvider travelTimeProvider = injector.getInstance(TravelTimeProvider.class);
//		
//		// prepare test requests
//		TestPlanRequest request0 = new TestPlanRequest(
//				1390, config, nodesMappedByIndex.getNodeByIndex(15982), nodesMappedByIndex.getNodeByIndex(1071), 117, true,
//				travelTimeProvider);
//		TestPlanRequest request1 = new TestPlanRequest(
//				453, config, nodesMappedByIndex.getNodeByIndex(15982), nodesMappedByIndex.getNodeByIndex(1071), 57, true,
//				travelTimeProvider);
//		LinkedHashSet<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
//		onBoardRequests.add(request0);
//		onBoardRequests.add(request1);
//		LinkedHashSet<TestPlanRequest> requests = new LinkedHashSet<>();
//		requests.add(request0);
//				
//		// prepare test vehicle
//		SimulationNode position = nodesMappedByIndex.getNodeByIndex(19027);
//		DelayData delayData = new DelayData(9190L, 207407L, 12765);
//		SimulationNode targetNode = nodesMappedByIndex.getNodeByIndex(6928);
//		TestOnDemandVehicle testOnDemandVehicle = new TestOnDemandVehicle(config, targetNode, delayData, position);
//		TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, position, 
//				config.ridesharing.vehicleCapacity, testOnDemandVehicle);
//		
//		Plan plan = arrayOptimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, requests, 210, false);
//		
//		Assert.assertNotNull(plan);
//	}
	
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
