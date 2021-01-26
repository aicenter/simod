/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common.TestDistanceMatrixTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common.TestAgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common.TestModule;
import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.NodesMappedByIndex;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
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
            TestAgentPolisInitializer agentPolisInitializer 
                            = new TestAgentPolisInitializer(new TestModule(config));
            Injector injector = agentPolisInitializer.initialize();

            // prepare map
            MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
            MapData mapData = mapInitializer.getMap();
            injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
            injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
            Map<Integer, SimulationNode> map = injector.getInstance(AllNetworkNodes.class).getAllNetworkNodes();
            // travel time providers
            AstarTravelTimeProvider astarTravelTimeProvider = 
                    injector.getInstance(AstarTravelTimeProvider.class);
            TestDistanceMatrixTravelTimeProvider distanceMatrixTravelTimeProvider 
                            = injector.getInstance(TestDistanceMatrixTravelTimeProvider.class);
                            
            for (int i = 0; i < map.size(); i++) {
                SimulationNode from = map.get(i);
                for (int j = 0; j < map.size(); j++) {
                    SimulationNode to = map.get(j);
                    double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
                    double durationDm = distanceMatrixTravelTimeProvider.getExpectedTravelTime(from, to);
                    LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", from, 
                                    from.getIndex(), to, to.getIndex(), durationAstar, durationDm, durationAstar - durationDm);
                    Assert.assertEquals(durationAstar, durationDm, 1);
                }
            }			
	}				
}
