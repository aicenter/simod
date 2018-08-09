/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.mapInitialization.MapInitializer;
import cz.cvut.fel.aic.amodsim.io.ExportEdge;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.aic.amodsim.io.ExportEdgePair;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
public class OsmUtil {
//    public static Graph<RoadNode, RoadEdge> getHigwayGraph(File osmFile, int srid){
//        Transformer transformer = new Transformer(srid);
//
//        GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(transformer, osmFile,
//                Sets.immutableEnumSet(ModeOfTransport.CAR), null, null);
//        Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();
//
//        return highwayGraph;
//    }

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OsmUtil.class);
    
    public static void edgesToJson(Graph<SimulationNode, SimulationEdge> higwayGraph, File outputFile){
        ObjectMapper mapper = new ObjectMapper();

        LinkedList<ExportEdge> edges = new LinkedList<>();

        for (SimulationEdge edge : higwayGraph.getAllEdges()) {
            String id = Integer.toString(edge.getUniqueId());
            edges.add(new ExportEdge(higwayGraph.getNode(edge.fromId), higwayGraph.getNode(edge.toId), 
                    id, edge.getLanesCount(), edge.allowedMaxSpeedInMpS, 
                    edge.getLength()));
        }

        try {
            mapper.writeValue(outputFile, edges);
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }

//    public static Graph<OldSimulationNode, SimulationEdge> buildSimulationGraph(Graph<RoadNode, RoadEdge> highwayGraph) {
//        GraphBuilder<OldSimulationNode, SimulationEdge> graphBuilder = GraphBuilder.createGraphBuilder();
//        for (RoadNode roadNode : highwayGraph.getAllNodes()) {
//            OldSimulationNode simulationNode = new OldSimulationNode(roadNode);
//            graphBuilder.addNode(simulationNode);
//        }
//        for (RoadEdge roadEdge : highwayGraph.getAllEdges()) {
//            SimulationEdge.SimulationEdgeBuilder edgeBuilder = new SimulationEdge.SimulationEdgeBuilder(roadEdge);
//            graphBuilder.addEdge(edgeBuilder.build(roadEdge.fromId, roadEdge.toId));
//        }
//        return graphBuilder.createGraph();
//    }
    
    public static Graph<SimulationNode, SimulationEdge> getSimulationGraph(MapInitializer mapInitializer){
        return mapInitializer.getMap().graphByType.get(EGraphType.HIGHWAY);
    }

    static void edgePairsToJson(Graph<SimulationNode, SimulationEdge> higwayGraph, File outputFile) {
        ObjectMapper mapper = new ObjectMapper();
        
        HashSet<SimulationEdge> processedEdges = new HashSet<>();
        LinkedList<ExportEdgePair> edgePairs = new LinkedList<>();
        
        for (SimulationEdge simEdge1 : higwayGraph.getAllEdges()) {
            if(processedEdges.contains(simEdge1)){
                continue;
            }
            
            processedEdges.add(simEdge1);
            String id = Integer.toString(simEdge1.getUniqueId());
            ExportEdge expEdge1 = new ExportEdge(higwayGraph.getNode(simEdge1.fromId), 
                    higwayGraph.getNode(simEdge1.toId), id, 
                    simEdge1.getLanesCount(), simEdge1.allowedMaxSpeedInMpS, 
                    simEdge1.getLength());
            
            SimulationEdge simEdge2 = higwayGraph.getEdge(simEdge1.toId, simEdge1.fromId);
            if(simEdge2 == null){
                edgePairs.add(new ExportEdgePair(expEdge1, null));
            }
            else{
                processedEdges.add(simEdge2);
                id = Integer.toString(simEdge2.getUniqueId());
                ExportEdge expEdge2 = new ExportEdge(higwayGraph.getNode(simEdge2.fromId), 
                    higwayGraph.getNode(simEdge2.toId), id, 
                    simEdge2.getLanesCount(), simEdge2.allowedMaxSpeedInMpS, 
                    simEdge2.getLength());
                edgePairs.add(new ExportEdgePair(expEdge1, expEdge2));
            }
        }
        
        try {
            mapper.writeValue(outputFile, edgePairs);
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }
}
