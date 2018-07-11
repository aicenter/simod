/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.amodsim.graphbuilder.SimulationEdgeFactory;
import cz.cvut.fel.aic.amodsim.graphbuilder.SimulationNodeFactory;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.GraphCreator;
import cz.cvut.fel.aic.graphimporter.geojson.GeoJSONReader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 * @author david
 */
public class MapInitializer {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapInitializer.class);

    private final File mapFile;

    private final Transformer projection;

    private final Set<TransportMode> allowedOsmModes;


    @Inject
    public MapInitializer(Transformer projection, @Named("osm File") File mapFile, Set<TransportMode> allowedOsmModes) {
        this.mapFile = mapFile;
        this.projection = projection;
        this.allowedOsmModes = allowedOsmModes;
    }


    /**
     * init map
     *
     * @return map data with simulation graph
     */
    public MapData getMap() {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs = new HashMap<>();
//        OsmImporter importer = new OsmImporter(mapFile, allowedOsmModes, projection);
        /*String nodeFile = "C:\\AIC data\\Shared\\amod-data\\noc_vedcu/data/nodes.geojson";
        String edgeFile = "C:\\AIC data\\Shared\\amod-data\\noc_vedcu/data/edges.geojson";*/
        String nodeFile = "C:\\Users\\User\\Desktop\\owncloud\\all/nodes.geojson";
        String edgeFile = "C:\\Users\\User\\Desktop\\owncloud\\all/edges.geojson";
        GeoJSONReader importer = new GeoJSONReader(edgeFile, nodeFile, projection);

        GraphCreator<SimulationNode, SimulationEdge> graphCreator = new GraphCreator(
                true, true, importer, new SimulationNodeFactory(), new SimulationEdgeFactory());

        graphs.put(EGraphType.HIGHWAY, graphCreator.getMap());

        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);

        LOGGER.info("Graphs imported, highway graph details: {}", graphs.get(EGraphType.HIGHWAY));
        return new MapData(graphs, nodes);
    }

    /**
     * Build map data
     */
    private Map<Integer, SimulationNode> createAllGraphNodes(Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphByGraphType) {

        Map<Integer, SimulationNode> nodesFromAllGraphs = new HashMap<>();

        for (GraphType graphType : graphByGraphType.keySet()) {
            Graph<SimulationNode, SimulationEdge> graphStorageTmp = graphByGraphType.get(graphType);
            for (SimulationNode node : graphStorageTmp.getAllNodes()) {
                nodesFromAllGraphs.put(node.getId(), node);
            }

        }

        return nodesFromAllGraphs;

    }
}
