/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simulator.creator.initializator.MapInitFactory;
import cz.agents.agentpolis.simulator.creator.initializator.impl.MapData;
import cz.agents.basestructures.BoundingBox;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.GraphBuilder;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GTDGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 *
 * @author david
 */
public class MyMapInitFactory implements MapInitFactory{
	
	private static final Logger LOGGER = Logger.getLogger(MyMapInitFactory.class);
	
	private final int epsg;

	public MyMapInitFactory(int epsg) {
		this.epsg = epsg;
	}
	 
	 

	@Override
	public MapData initMap(File mapFile, Injector injector, long simulationDurationInMilisec) {
		Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs;
        try {
            graphs = deserializeGraphs(mapFile);
        } catch (Exception ex) {
            LOGGER.warn("Cannot perform deserialization of the cached graphs:" + ex.getMessage());
            LOGGER.warn("Generating graphs from the OSM");
            graphs = generateGraphsFromOSM(mapFile, injector, simulationDurationInMilisec);
            serializeGraphs(graphs, mapFile.getName() + ".ser");
        }
        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);
        BoundingBox bounds = computeBounds(nodes.values());
        LOGGER.info("Graphs imported, highway graph details: " + graphs.get(EGraphType.HIGHWAY));
        return new MapData(bounds, graphs, nodes);
	}
	
	private Map<GraphType, Graph<SimulationNode, SimulationEdge>> deserializeGraphs(File osmFile) 
			throws IOException, ClassNotFoundException {
        InputStream file = new FileInputStream(osmFile.getName() + ".ser");
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        return (Map<GraphType, Graph<SimulationNode, SimulationEdge>>) input.readObject();
    }
	
	private Map<GraphType, Graph<SimulationNode, SimulationEdge>> generateGraphsFromOSM(File mapFile, Injector injector, long simulationDurationInMilisec) {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs;
        ZonedDateTime initDate = ZonedDateTime.now();
        Graph<SimulationNode, SimulationEdge> highwayGraphFromOSM = createHighwayGraphFromPlannerGraph(mapFile, injector, simulationDurationInMilisec, initDate, 0);
        Graph<SimulationNode, SimulationEdge> highwayGraph = connectivity(highwayGraphFromOSM);

        graphs = new HashMap<>();
        graphs.put(EGraphType.HIGHWAY, highwayGraph);
        graphs.put(EGraphType.TRAMWAY, (new GraphBuilder()).createGraph());
        graphs.put(EGraphType.METROWAY, (new GraphBuilder()).createGraph());
        graphs.put(EGraphType.PEDESTRIAN, (new GraphBuilder()).createGraph());
        graphs.put(EGraphType.BIKEWAY, (new GraphBuilder()).createGraph());
        graphs.put(EGraphType.RAILWAY, (new GraphBuilder()).createGraph());
        return graphs;
    }
	
	private Graph<SimulationNode, SimulationEdge> createHighwayGraphFromPlannerGraph(File mapFile, Injector injector, 
			long durationInMilisec, ZonedDateTime initDate, long simulationDurationInMilisec) {
        LOGGER.info(epsg);
        GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(new Transformer(epsg), mapFile, Sets.immutableEnumSet
                (ModeOfTransport.CAR), null, null);
        Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();

        Graph<SimulationNode, SimulationEdge> simulationGraph = buildSimulationGraph(highwayGraph);

        return simulationGraph;
    }
	
	private Graph<SimulationNode, SimulationEdge> buildSimulationGraph(Graph<RoadNode, RoadEdge> highwayGraph) {
        GraphBuilder<SimulationNode, SimulationEdge> graphBuilder = GraphBuilder.createGraphBuilder();
        for (RoadNode roadNode : highwayGraph.getAllNodes()) {
            SimulationNode simulationNode = new SimulationNode(roadNode);
            graphBuilder.addNode(simulationNode);
        }
        for (RoadEdge roadEdge : highwayGraph.getAllEdges()) {
            SimulationEdge.SimulationEdgeBuilder edgeBuilder = new SimulationEdge.SimulationEdgeBuilder(roadEdge);
            graphBuilder.addEdge(edgeBuilder.build(roadEdge.fromId, roadEdge.toId));
        }
        return graphBuilder.createGraph();
    }

	
	private Graph<SimulationNode, SimulationEdge> connectivity(Graph<SimulationNode, SimulationEdge> graph) {

        DirectedGraph<Integer, ShortestPathPlanner.PlannerEdge> plannerGraph = prepareGraphForFindComponents(graph);

        StrongConnectivityInspector<Integer, ShortestPathPlanner.PlannerEdge> strongConnectivityInspector = new StrongConnectivityInspector<>(
                plannerGraph);

        if (strongConnectivityInspector.isStronglyConnected()) {
            return graph;
        }

        LOGGER.debug("The Highway map has more then one strong component, it will be selected the largest components");

        Set<Integer> strongestComponents = getTheLargestGraphComponent(strongConnectivityInspector);

        return createGraphBasedOnTheLargestComponent(graph, strongestComponents);
    }
	
	private DirectedGraph<Integer, ShortestPathPlanner.PlannerEdge> prepareGraphForFindComponents(Graph<SimulationNode, 
			SimulationEdge> graph) {

        DirectedGraph<Integer, ShortestPathPlanner.PlannerEdge> plannerGraph = new DefaultDirectedGraph<>(
				ShortestPathPlanner.PlannerEdge.class);
        Set<Integer> addedNodes = new HashSet<>();

        for (SimulationNode node : graph.getAllNodes()) {
            Integer fromPositionByNodeId = node.getId();
            if (!addedNodes.contains(fromPositionByNodeId)) {
                addedNodes.add(fromPositionByNodeId);
                plannerGraph.addVertex(fromPositionByNodeId);
            }

            for (SimulationEdge edge : graph.getOutEdges(node.getId())) {
                Integer toPositionByNodeId = edge.getToId();
                if (!addedNodes.contains(toPositionByNodeId)) {
                    addedNodes.add(toPositionByNodeId);
                    plannerGraph.addVertex(toPositionByNodeId);
                }

                ShortestPathPlanner.PlannerEdge plannerEdge = new ShortestPathPlanner.PlannerEdge(null, fromPositionByNodeId, toPositionByNodeId);
                plannerGraph.addEdge(fromPositionByNodeId, toPositionByNodeId, plannerEdge);
                // plannerGraph.setEdgeWeight(plannerEdge, edge.getLenght());
            }

        }
        return plannerGraph;
    }
	
	private Set<Integer> getTheLargestGraphComponent(
            StrongConnectivityInspector<Integer, ShortestPathPlanner.PlannerEdge> strongConnectivityInspector) {
        List<Set<Integer>> components = strongConnectivityInspector.stronglyConnectedSets();
        Collections.sort(components, (o1, o2) -> o2.size() - o1.size());
        return components.get(0);
    }
	
	private Graph<SimulationNode, SimulationEdge> createGraphBasedOnTheLargestComponent(Graph<SimulationNode, SimulationEdge> graph,
                                                                                        Set<Integer> strongestComponents) {
        GraphBuilder<SimulationNode, SimulationEdge> graphBuilder = new GraphBuilder<>();
        for (Integer nodeId : strongestComponents) {
            graphBuilder.addNode(graph.getNode(nodeId));
        }

        for (Integer nodeId : strongestComponents) {
            for (SimulationEdge edge : graph.getOutEdges(nodeId)) {
                if (strongestComponents.contains(edge.getToId())) {
                    graphBuilder.addEdge(graph.getEdge(nodeId, edge.getToId()));
                }
            }
        }
        return graphBuilder.createGraph();
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
