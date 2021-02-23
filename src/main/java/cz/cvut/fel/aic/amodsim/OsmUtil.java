/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.io.ExportEdge;
import cz.cvut.fel.aic.amodsim.io.ExportEdgePair;
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
//	public static Graph<RoadNode, RoadEdge> getHigwayGraph(File osmFile, int srid){
//		Transformer transformer = new Transformer(srid);
//
//		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(transformer, osmFile,
//				Sets.immutableEnumSet(ModeOfTransport.CAR), null, null);
//		Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();
//
//		return highwayGraph;
//	}

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OsmUtil.class);
	
	public static void edgesToJson(Graph<SimulationNode, SimulationEdge> higwayGraph, File outputFile){
		ObjectMapper mapper = new ObjectMapper();

		LinkedList<ExportEdge> edges = new LinkedList<>();

		for (SimulationEdge edge : higwayGraph.getAllEdges()) {
			String id = edge.getStaticId().toString();
			edges.add(new ExportEdge(edge.getFromNode(), edge.getToNode(), 
					id, edge.getLanesCount(), edge.getAllowedMaxSpeedInCmPerSecond(), 
					edge.getLengthCm()));
		}

		try {
			mapper.writeValue(outputFile, edges);
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}

//	public static Graph<OldSimulationNode, SimulationEdge> buildSimulationGraph(Graph<RoadNode, RoadEdge> highwayGraph) {
//		GraphBuilder<OldSimulationNode, SimulationEdge> graphBuilder = GraphBuilder.createGraphBuilder();
//		for (RoadNode roadNode : highwayGraph.getAllNodes()) {
//			OldSimulationNode simulationNode = new OldSimulationNode(roadNode);
//			graphBuilder.addNode(simulationNode);
//		}
//		for (RoadEdge roadEdge : highwayGraph.getAllEdges()) {
//			SimulationEdge.SimulationEdgeBuilder edgeBuilder = new SimulationEdge.SimulationEdgeBuilder(roadEdge);
//			graphBuilder.addEdge(edgeBuilder.build(roadEdge.fromId, roadEdge.toId));
//		}
//		return graphBuilder.createGraph();
//	}
	
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
			String id = simEdge1.getStaticId().toString();
			ExportEdge expEdge1 = new ExportEdge(simEdge1.getFromNode(), 
					simEdge1.getToNode(), id, 
					simEdge1.getLanesCount(), simEdge1.getAllowedMaxSpeedInCmPerSecond(), 
					simEdge1.getLengthCm());
			
			SimulationEdge simEdge2 = higwayGraph.getEdge(simEdge1.getToNode(), simEdge1.getFromNode());
			if(simEdge2 == null){
				edgePairs.add(new ExportEdgePair(expEdge1, null));
			}
			else{
				processedEdges.add(simEdge2);                              
				id = simEdge2.getStaticId().toString();
				ExportEdge expEdge2 = new ExportEdge(simEdge2.getFromNode(), 
					simEdge2.getToNode(), id, 
					simEdge2.getLanesCount(), simEdge2.getAllowedMaxSpeedInCmPerSecond(), 
					simEdge2.getLengthCm());
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
