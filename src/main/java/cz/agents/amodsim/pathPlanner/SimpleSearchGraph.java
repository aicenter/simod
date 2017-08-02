/*
 */
package cz.agents.amodsim.pathPlanner;

import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import cz.agents.multimodalplanning.searchstructures.nodes.RoadSearchNode;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import cz.agents.planningalgorithms.SearchGraph;
import cz.agents.planningalgorithms.SearchNode;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author F-I-D-O
 */
public class SimpleSearchGraph extends SearchGraph<SimpleSearchNode>{
	
	private final Graph<RoadNode,RoadEdge> graph;
	
	private final int speed;

	
	
	
	public SimpleSearchGraph(Graph<RoadNode,RoadEdge> graph, int speed) {
		this.graph = graph;
		this.speed = speed;
	}
	
	


	@Override
	public Set<SimpleSearchNode> successorsOf(SimpleSearchNode vertex) {
		Set<SimpleSearchNode> successors = new HashSet<>();

        for (RoadEdge edge : graph.getOutEdges(vertex.nodeId)) {
            RoadNode successorNode = graph.getNode(edge.toId);
			int arrivalTimeInTargetNode = edge.getArrival(vertex.getArrivalTime(), speed);

			SimpleSearchNode successor = new SimpleSearchNode(successorNode.id, vertex, arrivalTimeInTargetNode);
			successors.add(successor);
        }

        return successors;
	}

	@Override
	public SimpleSearchNode predecessorOf(SimpleSearchNode vertex) {
		return vertex.getPredecessor();
	}
	
}
