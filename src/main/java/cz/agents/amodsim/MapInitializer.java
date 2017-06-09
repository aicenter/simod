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
import cz.agents.agentpolis.simulator.visualization.visio.Bounds;
import cz.agents.amodsim.graphbuilder.SimulationGraphBuilder;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simulator.creator.initializator.impl.MapData;
import cz.agents.agentpolis.simulator.visualization.visio.Bounds;
import cz.agents.basestructures.BoundingBox;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author david
 */
public class MapInitializer {

    private static final Logger LOGGER = Logger.getLogger(MapInitializer.class);

    private final int srid;

    private final File mapFile;

    private final SimulationGraphBuilder roadNetworkGraphBuilder;

    /**
     * Constructor
     *
     * @param srid                    coordinate system
     * @param mapFile
     * @param roadNetworkGraphBuilder
     */
    @Inject
    public MapInitializer(@Named("mapSrid") int srid, @Named("osm File") File mapFile,
                          SimulationGraphBuilder roadNetworkGraphBuilder) {
        this.srid = srid;
        this.mapFile = mapFile;
        this.roadNetworkGraphBuilder = roadNetworkGraphBuilder;
    }


    /**
     * init map
     *
     * @return map data with simulation graph
     */
    public MapData getMap() {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs;
        try {
            graphs = deserializeGraphs(mapFile);
//            throw new IOException(); // TODO: debug, remove it afterwards
        } catch (Exception ex) {
            LOGGER.warn("Cannot perform deserialization of the cached graphs:" + ex.getMessage());
            LOGGER.warn("Generating graphs from the OSM");
            graphs = generateGraphsFromOSM(mapFile);
            serializeGraphs(graphs, mapFile.getAbsolutePath() + ".ser");
        }
        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);
        Bounds bounds = computeBounds(nodes.values());

        LOGGER.info("Graphs imported, highway graph details: " + graphs.get(EGraphType.HIGHWAY));
        return new MapData(bounds, graphs, nodes);
    }

    /**
     * Graph build section
     */
    private Map<GraphType, Graph<SimulationNode, SimulationEdge>> generateGraphsFromOSM(File mapFile) {
        Graph<SimulationNode, SimulationEdge> roadNetworkGraphFromOSM = roadNetworkGraphBuilder.build();
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs = new HashMap<>();
        graphs.put(EGraphType.HIGHWAY, roadNetworkGraphFromOSM);
        //graphs.put(EGraphType.TRAMWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.METROWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.PEDESTRIAN, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.BIKEWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.RAILWAY, (new GraphBuilder()).createGraph());
        return graphs;
    }


    /**
     * Serialization section
     */
    private Map<GraphType, Graph<SimulationNode, SimulationEdge>> deserializeGraphs(File osmFile)
            throws IOException, ClassNotFoundException {
        InputStream file = new FileInputStream(osmFile.getAbsolutePath() + ".ser");
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        return (Map<GraphType, Graph<SimulationNode, SimulationEdge>>) input.readObject();
    }

    private void serializeGraphs(Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs, String outputFilename) {
        try (
                OutputStream file = new FileOutputStream(outputFilename);
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);
        ) {
            output.writeObject(graphs);
        } catch (IOException ex) {
            LOGGER.warn("Graphs serialization failed, " + ex.getMessage());
        }
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

    private Bounds computeBounds(Collection<SimulationNode> nodes) {

        double latMin = Double.POSITIVE_INFINITY;
        int latMinProjected = 0;

        double latMax = Double.NEGATIVE_INFINITY;
        int latMaxProjected = 0;

        double lonMin = Double.POSITIVE_INFINITY;
        int lonMinProjected = 0;

        double lonMax = Double.NEGATIVE_INFINITY;
        int lonMaxProjected = 0;

        Node latMinNode = null;
        Node latMaxNode = null;
        Node lonMinNode = null;
        Node lonMaxNode = null;

        for (Node node : nodes) {
            double lat = node.getLatitude();
            int latProjected = node.getLatProjected();

            if (lat < latMin) {
                latMin = lat;
                latMinNode = node;
            } else if (lat > latMax) {
                latMax = lat;
                latMaxNode = node;
            }
            if (lon < lonMin) {
                lonMin = lon;
                lonMinNode = node;
            } else if (lon > lonMax) {
                lonMax = lon;
                lonMaxNode = node;
            }

        }
        GPSLocation minNode = new GPSLocation(latMinNode.getLatitude(), lonMinNode.getLongitude(), latMinNode.getLatProjected(), lonMinNode.getLonProjected());
        GPSLocation maxNode = new GPSLocation(latMaxNode.getLatitude(), lonMaxNode.getLongitude(), latMaxNode.getLatProjected(), lonMaxNode.getLonProjected());
        return new Bounds(minNode, maxNode);
    }

}
