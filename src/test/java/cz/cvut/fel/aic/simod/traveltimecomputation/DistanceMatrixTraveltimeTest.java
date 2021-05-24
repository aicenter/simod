///*
// * Copyright (c) 2021 Czech Technical University in Prague.
// *
// * This file is part of Amodsim project.
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package cz.cvut.fel.aic.simod.traveltimecomputation;
//
//import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
//import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
//import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
//import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
//import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
//import cz.cvut.fel.aic.agentpolis.simulator.MapData;
//import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
//import cz.cvut.fel.aic.simod.config.SimodConfig;
//import cz.cvut.fel.aic.simod.traveltimecomputation.common.TestAgentPolisInitializer;
//import cz.cvut.fel.aic.simod.traveltimecomputation.common.TestDistanceMatrixTravelTimeProvider;
//import cz.cvut.fel.aic.simod.traveltimecomputation.common.TestModule;
//import org.junit.Assert;
//import org.junit.Test;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author david
// */
//public class DistanceMatrixTraveltimeTest {
//	
//	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixTraveltimeTest.class);
//	
//	@Test
//	public void test(){
//		AmodsimConfig config = new AmodsimConfig();
//            SimodConfig config = new SimodConfig();
//		TestAgentPolisInitializer agentPolisInitializer 
//						= new TestAgentPolisInitializer(new TestModule(config));
//		Injector injector = agentPolisInitializer.initialize();
//
//		// prepare map
//		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
//		MapData mapData = mapInitializer.getMap();
//		injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
//		injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
//		Map<Integer, SimulationNode> map = injector.getInstance(AllNetworkNodes.class).getAllNetworkNodes();
//		// travel time providers
//		AstarTravelTimeProvider astarTravelTimeProvider = 
//				injector.getInstance(AstarTravelTimeProvider.class);
//		TestDistanceMatrixTravelTimeProvider distanceMatrixTravelTimeProvider 
//						= injector.getInstance(TestDistanceMatrixTravelTimeProvider.class);
//
//		for (int i = 0; i < map.size(); i++) {
//			SimulationNode from = map.get(i);
//			for (int j = 0; j < map.size(); j++) {
//				SimulationNode to = map.get(j);
//				double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
//				double durationDm = distanceMatrixTravelTimeProvider.getExpectedTravelTime(from, to);
//				LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", from, 
//								from.getIndex(), to, to.getIndex(), durationAstar, durationDm, durationAstar - durationDm);
//				Assert.assertEquals(durationAstar, durationDm, 30);
//			}
//		}			
//	}				
//}
