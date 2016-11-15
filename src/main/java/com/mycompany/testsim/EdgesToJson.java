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

    private static File EXPERIMENT_DIR = new File("data/Prague");
    
    private static final int SRID = 2065;

    private static final String OSM_FILE = "prague-filtered-complete.osm";

    private static final String OUTPUT_FILE = "edges.json";

    private static final String OUTPUT_FILE_PAIRS = "data/Prague/edgePairs.json";
    
    public static void main(String[] args) {
        if (args.length >= 1) {
            EXPERIMENT_DIR = new File(args[0]);
        }
        Graph<RoadNode, RoadEdge> roadGraph = OsmUtil.getHigwayGraph(new File(EXPERIMENT_DIR, OSM_FILE), SRID);
        OsmUtil.edgesToJson(OsmUtil.buildSimulationGraph(roadGraph), new File(EXPERIMENT_DIR, OUTPUT_FILE));
        OsmUtil.edgePairsToJson(OsmUtil.buildSimulationGraph(roadGraph), new File(EXPERIMENT_DIR, OUTPUT_FILE_PAIRS));
    }
}
