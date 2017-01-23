/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadEdgeExtended;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadNodeExtended;
import cz.agents.amodsim.config.Config;
import cz.agents.basestructures.Graph;
import java.io.File;

/**
 *
 * @author fido
 */
public class EdgesToJson {
    
    public static void main(String[] args) {
        
        Config config = new Configuration().load();

        Graph<RoadNodeExtended, RoadEdgeExtended> roadGraph 
                = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);
        OsmUtil.edgesToJson(roadGraph, new File(config.agentpolis.edgesFilePath));
        OsmUtil.edgePairsToJson(roadGraph, new File(config.agentpolis.edgePairsFilePath));
    }
}
