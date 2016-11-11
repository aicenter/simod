/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import cz.agents.basestructures.Graph;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.io.File;

/**
 *
 * @author fido
 */
public class EdgesToJson {
    
    private static final int SRID = 2065;
    
    private static final File OSM_FILE = new File("data/Prague/prague-filtered-complete.osm");
    
    private static final File OUTPUT_FILE = new File("data/Prague/edges.json");
    
    
    public static void main(String[] args) {
        Graph<RoadNode, RoadEdge> roadGraph = OsmUtil.getHigwayGraph(OSM_FILE, SRID);
        OsmUtil.edgesToJson(OsmUtil.buildSimulationGraph(roadGraph), OUTPUT_FILE);
    }
}
