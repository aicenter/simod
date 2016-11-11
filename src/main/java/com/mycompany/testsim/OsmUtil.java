/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.GraphBuilder;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GTDGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
public class OsmUtil {
    public static Graph<RoadNode, RoadEdge> getHigwayGraph(File osmFile, int srid){
        Transformer transformer = new Transformer(srid);
		
		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(transformer, osmFile, 
				Sets.immutableEnumSet(ModeOfTransport.CAR), null, null);
        Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();
        
        return highwayGraph;
    }
    
    public static void edgesToJson(Graph<SimulationNode, SimulationEdge> higwayGraph, File outputFile){
        ObjectMapper mapper = new ObjectMapper();
        
        LinkedList<ExportEdge> edges = new LinkedList<>();
        
        for (SimulationEdge edge : higwayGraph.getAllEdges()) {
            edges.add(new ExportEdge(higwayGraph.getNode(edge.getFromId()), higwayGraph.getNode(edge.getToId()), 
                    edge.getWayID(), edge.getLaneCount(EGraphType.HIGHWAY), edge.getAllowedMaxSpeedInMpS(), 
                    edge.getLength()));
        }
		
        try {
            mapper.writeValue(outputFile, edges);
        } catch (IOException ex) {
            Logger.getLogger(OsmUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Graph<SimulationNode, SimulationEdge> buildSimulationGraph(Graph<RoadNode, RoadEdge> highwayGraph) {
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
}
