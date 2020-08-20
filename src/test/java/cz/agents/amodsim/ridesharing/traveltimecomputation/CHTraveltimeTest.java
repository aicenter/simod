package cz.agents.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.CHTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TNRAFTravelTimeProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A test verifying that the Contraction Hierarchies provider returns the same distances as the A* provider.
 *
 * @author Michal Cvach
 */
public class CHTraveltimeTest {
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
        CHTravelTimeProvider chTravelTimeProvider
                = injector.getInstance(CHTravelTimeProvider.class);

        for (int i = 0; i < 20; i++) {
            SimulationNode from = map.get((i * 1300) % map.size());
            for (int j = 0; j < 20; j++) {
                SimulationNode to = map.get(((j * 897) + 2000) % map.size());
                double durationAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
                double durationCH = chTravelTimeProvider.getExpectedTravelTime(from, to);
                LOGGER.debug("From {}(index {}) to {}(index {}), astar distance: {}, CH distance: {}, difference {}", from,
                        from.getIndex(), to, to.getIndex(), durationAstar, durationCH, durationAstar - durationCH);
                Assert.assertEquals(durationAstar, durationCH, 5);
            }
        }
    }
}
