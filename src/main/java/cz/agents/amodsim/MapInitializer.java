/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simulator.creator.initializator.impl.MapData;
import cz.agents.amodsim.graphbuilder.SimulationNodeFactory;
import cz.agents.amodsim.graphbuilder.structurebuilders.SimulationEdgeFactory;
import cz.agents.basestructures.BoundingBox;
//import cz.agents.agentpolis.simulator.visualization.visio.Bounds;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GraphCreator;
import cz.agents.gtdgraphimporter.osm.OsmImporter;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author david
 */
public class MapInitializer {

    private static final Logger LOGGER = Logger.getLogger(MapInitializer.class);

    private final File mapFile;
    
    private final Transformer projection;
    
    private final Set<ModeOfTransport> allowedOsmModes;


    @Inject
    public MapInitializer(Transformer projection, @Named("osm File") File mapFile, Set<ModeOfTransport> allowedOsmModes) {
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
        OsmImporter importer = new OsmImporter(mapFile, allowedOsmModes, projection);
        
        GraphCreator<SimulationNode,SimulationEdge> graphCreator = new GraphCreator(projection, 
                true, true, importer, new SimulationNodeFactory(), new SimulationEdgeFactory());
        
        graphs.put(EGraphType.HIGHWAY, graphCreator.getMap());

        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);
        BoundingBox bounds = computeBounds(nodes.values());

        LOGGER.info("Graphs imported, highway graph details: " + graphs.get(EGraphType.HIGHWAY));
        return new MapData(bounds, graphs, nodes);
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

//    private Bounds computeBounds(Collection<SimulationNode> nodes) {
//
//        double latMin = Double.POSITIVE_INFINITY;
//        int latMinProjected = 0;
//
//        double latMax = Double.NEGATIVE_INFINITY;
//        int latMaxProjected = 0;
//
//        double lonMin = Double.POSITIVE_INFINITY;
//        int lonMinProjected = 0;
//
//        double lonMax = Double.NEGATIVE_INFINITY;
//        int lonMaxProjected = 0;
//
//        Node latMinNode = null;
//        Node latMaxNode = null;
//        Node lonMinNode = null;
//        Node lonMaxNode = null;
//
//        for (Node node : nodes) {
//            double lat = node.getLatitude();
//            double lon = node.getLongitude();
//
//            if (lat < latMin) {
//                latMin = lat;
//                latMinNode = node;
//            } else if (lat > latMax) {
//                latMax = lat;
//                latMaxNode = node;
//            }
//            if (lon < lonMin) {
//                lonMin = lon;
//                lonMinNode = node;
//            } else if (lon > lonMax) {
//                lonMax = lon;
//                lonMaxNode = node;
//            }
//
//        }
//        GPSLocation minNode = new GPSLocation(latMinNode.getLatitude(), lonMinNode.getLongitude(), latMinNode.getLatProjected(), lonMinNode.getLonProjected());
//        GPSLocation maxNode = new GPSLocation(latMaxNode.getLatitude(), lonMaxNode.getLongitude(), latMaxNode.getLatProjected(), lonMaxNode.getLonProjected());
//        return new Bounds(minNode, maxNode);
//    }
    
    private BoundingBox computeBounds(Collection<SimulationNode> nodes) {
        double latMin = Double.POSITIVE_INFINITY;
        double latMax = Double.NEGATIVE_INFINITY;

        double lonMin = Double.POSITIVE_INFINITY;
        double lonMax = Double.NEGATIVE_INFINITY;

        for (Node node : nodes) {
            double lat = node.getLatitude();
            double lon = node.getLongitude();

            if (lat < latMin) latMin = lat;
            else if (lat > latMax) latMax = lat;
            if (lon < lonMin) lonMin = lon;
            else if (lon > lonMax) lonMax = lon;
        }
        return new BoundingBox((int) (lonMin * 1E6), (int) (latMin * 1E6), (int) (lonMax * 1E6), (int) (latMax * 1E6));
    }

}
