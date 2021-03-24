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
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import cz.cvut.fel.aic.amodsim.traveltimecomputation.TNRTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.traveltimecomputation.AstarTravelTimeProvider;
import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common.ShortestPathsTestModule;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common.TestAgentPolisInitializer;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * A test verifying that the Transit Node Routing provider returns the same distances as the A* provider.
 *
 * @author Michal Cvach
 */
public class TNRTraveltimeTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixTraveltimeTest.class);

    @Test
    public void test(){
        AmodsimConfig config = new AmodsimConfig();
        // Guice configuration
        TestAgentPolisInitializer agentPolisInitializer
                = new TestAgentPolisInitializer(new ShortestPathsTestModule(config));
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
        TNRTravelTimeProvider tnrTravelTimeProvider
                = injector.getInstance(TNRTravelTimeProvider.class);

        for (int i = 0; i < 20; i++) {
            SimulationNode from = map.get((i * 1300) % map.size());
            for (int j = 0; j < 20; j++) {
                SimulationNode to = map.get(((j * 897) + 2000) % map.size());
                double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
                double durationTNR = tnrTravelTimeProvider.getExpectedTravelTime(from, to);
                LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, TNR distance: {}, difference {}", from,
                        from.getIndex(), to, to.getIndex(), durationAstar, durationTNR, durationAstar - durationTNR);
                Assert.assertEquals(durationAstar, durationTNR, 30);
            }
        }
    }
}
