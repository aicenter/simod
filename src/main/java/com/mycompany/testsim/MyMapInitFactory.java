/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.google.common.collect.Sets;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.builder.RoadNetworkGraphBuilder;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.builder.SimulationNetworkGraphBuilder;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadEdgeExtended;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadNodeExtended;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simulator.creator.initializator.MapInitFactory;
import cz.agents.agentpolis.simulator.creator.initializator.impl.MapData;
import cz.agents.basestructures.BoundingBox;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author david
 */
public class MyMapInitFactory implements MapInitFactory {

    private static final Logger LOGGER = Logger.getLogger(MyMapInitFactory.class);

    private final int epsg;

    /**
     * Constructor
     *
     * @param epsg coordinate system
     */
    public MyMapInitFactory(int epsg) {
        this.epsg = epsg;
    }


    /**
     * init map
     * @param mapFile osm map
     * @param simulationDurationInMilisec not in use, so place whatever number you want
     * @return map data with simulation graph
     */
    @Override
    public MapData initMap(File mapFile, long simulationDurationInMilisec) {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs;
        try {
            graphs = deserializeGraphs(mapFile);
            throw new IOException(); // TODO: debug, remove it afterwards
        } catch (Exception ex) {
            LOGGER.warn("Cannot perform deserialization of the cached graphs:" + ex.getMessage());
            LOGGER.warn("Generating graphs from the OSM");
            graphs = generateGraphsFromOSM(mapFile);
            serializeGraphs(graphs, mapFile.getName() + ".ser");
        }
        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);
        BoundingBox bounds = computeBounds(nodes.values());
        LOGGER.info("Graphs imported, highway graph details: " + graphs.get(EGraphType.HIGHWAY));
        return new MapData(bounds, graphs, nodes);
    }

    /**
     * Graph build section
     */
    private Map<GraphType, Graph<SimulationNode, SimulationEdge>> generateGraphsFromOSM(File mapFile) {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs;
        Graph<RoadNodeExtended, RoadEdgeExtended> highwayGraphFromOSM = createHighwayGraphFromPlannerGraph(mapFile);

        SimulationNetworkGraphBuilder simulationNetworkGraphBuilder = new SimulationNetworkGraphBuilder();
        Graph<SimulationNode, SimulationEdge> simulationHighwayGraph = simulationNetworkGraphBuilder.buildSimulationGraph(highwayGraphFromOSM);
        Graph<SimulationNode, SimulationEdge> highwayGraph = simulationNetworkGraphBuilder.connectivity(simulationHighwayGraph);

        graphs = new HashMap<>();
        graphs.put(EGraphType.HIGHWAY, highwayGraph);
        //graphs.put(EGraphType.TRAMWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.METROWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.PEDESTRIAN, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.BIKEWAY, (new GraphBuilder()).createGraph());
        //graphs.put(EGraphType.RAILWAY, (new GraphBuilder()).createGraph());
        return graphs;
    }

    private Graph<RoadNodeExtended, RoadEdgeExtended> createHighwayGraphFromPlannerGraph(File mapFile){
        LOGGER.info(epsg);
        RoadNetworkGraphBuilder roadNetworkGraphBuilder = new RoadNetworkGraphBuilder(new Transformer(epsg), mapFile, Sets.immutableEnumSet
                (ModeOfTransport.CAR));
        return roadNetworkGraphBuilder.build();
    }

    /**
     * Serialization section
     */
    private Map<GraphType, Graph<SimulationNode, SimulationEdge>> deserializeGraphs(File osmFile)
            throws IOException, ClassNotFoundException {
        InputStream file = new FileInputStream(osmFile.getName() + ".ser");
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
