
package cz.cvut.fel.aic.amodsim;

import ninja.fido.config.Configuration;
import cz.cvut.fel.aic.amodsim.config.Config;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;

/**
 *
 * @author fido
 */
public class OsmStatistic {
    public static void main(String[] args) {
        Config config = Configuration.load(new Config());
        
//        Graph<SimulationNode, SimulationEdge> highwayGraph = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);
        
//        System.out.println("Edges total: " + highwayGraph.getAllEdges().size());
//        
//        System.out.println("Nodes total: " + highwayGraph.getAllNodes().size());
    }
}