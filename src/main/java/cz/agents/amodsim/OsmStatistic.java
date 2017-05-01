
package cz.agents.amodsim;

import ninja.fido.config.Configuration;
import cz.agents.amodsim.config.Config;
import cz.agents.basestructures.Graph;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.io.File;

/**
 *
 * @author fido
 */
public class OsmStatistic {
    public static void main(String[] args) {
        Config config = Configuration.load(new Config());
        
        Graph<RoadNode, RoadEdge> highwayGraph = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);
        
        System.out.println("Edges total: " + highwayGraph.getAllEdges().size());
        
        System.out.println("Nodes total: " + highwayGraph.getAllNodes().size());
    }
}
