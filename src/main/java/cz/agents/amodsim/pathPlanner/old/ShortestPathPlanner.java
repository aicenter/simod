package cz.agents.amodsim.pathPlanner.old;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;


import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.HashMap;
import java.util.Map;

/**
 * The planner reuses DijkstraShortestPath from JGraphT
 *
 * @author Zbynek Moler
 */
public class ShortestPathPlanner {

	private final DirectedWeightedMultigraph<Integer, PlannerEdge> plannerGraph;
	
	private final Graph<? extends Node,? extends Edge> graph;
	
	private Map<Long,Integer> nodeIdsMappedByNodeSourceIds;

	private ShortestPathPlanner(Graph<? extends Node,? extends Edge> graph, 
			DirectedWeightedMultigraph<Integer, PlannerEdge> plannerGraph) {
		super();
		this.plannerGraph = plannerGraph;
		this.graph = graph;
	}
	
	public LinkedList<Long> findTrip(long startNodeById, long destinationNodeById) throws TripPlannerException {
		if(nodeIdsMappedByNodeSourceIds == null){
			nodeIdsMappedByNodeSourceIds = new HashMap<>(); 
			for (Node node : graph.getAllNodes()) {
				nodeIdsMappedByNodeSourceIds.put(node.getSourceId(), node.getId());
			}
		}
		
		LinkedList<Integer> nodeIds = findTrip(nodeIdsMappedByNodeSourceIds.get(startNodeById), 
				nodeIdsMappedByNodeSourceIds.get(destinationNodeById));
		LinkedList<Long> nodeSourceIds = new LinkedList<>();
		for (Integer nodeId : nodeIds) {
			nodeSourceIds.add(graph.getNode(nodeId).getSourceId());
		}
		return null;
	}

	public LinkedList<Integer> findTrip(int startNodeById, int destinationNodeById) throws TripPlannerException {

		assert startNodeById != destinationNodeById : "Start finding position should not be the same as end finding "
				+ "position";
		List<PlannerEdge> plannerEdges = findShortestPath(startNodeById, destinationNodeById);
		return createTrips(plannerEdges);
	}

	private List<PlannerEdge> findShortestPath(int fromPositionByNodeId, int toPositionByNodeId)
			throws TripPlannerException {
		DijkstraShortestPath<Integer, PlannerEdge> dijkstraShortestPath = new DijkstraShortestPath<>(plannerGraph,
				fromPositionByNodeId, toPositionByNodeId);
		List<PlannerEdge> plannerEdges = dijkstraShortestPath.getPathEdgeList();
		if (plannerEdges == null) {
			throw new TripPlannerException(fromPositionByNodeId, toPositionByNodeId);
		}

		return plannerEdges;

	}

	private LinkedList<Integer> createTrips(List<PlannerEdge> plannerEdges) {
		LinkedList<Integer> path = new LinkedList<>();

		if (plannerEdges.size() > 0) {
			PlannerEdge plannerEdge = plannerEdges.get(0);
			
			path.add(plannerEdge.fromPositionByNodeId);

			for (PlannerEdge plannerEdgeInner : plannerEdges) {
				path.add(plannerEdgeInner.toPositionByNodeId);
			}
		}
		return path;
	}

	public static ShortestPathPlanner createShortestPathPlanner(Graph<? extends Node,? extends Edge> graph) {
		DirectedWeightedMultigraph<Integer, PlannerEdge> plannerGraph = new DirectedWeightedMultigraph<>(PlannerEdge
				.class);
		Set<Integer> addedNodes = new HashSet<>();
		for (Node node : graph.getAllNodes()) {
			int fromPositionByNodeId = node.id;
			if (!addedNodes.contains(fromPositionByNodeId)) {
				addedNodes.add(fromPositionByNodeId);
				plannerGraph.addVertex(fromPositionByNodeId);
			}

			for (Edge edge : graph.getOutEdges(node.id)) {
				int toPositionByNodeId = edge.toId;
				if (!addedNodes.contains(toPositionByNodeId)) {
					addedNodes.add(toPositionByNodeId);
					plannerGraph.addVertex(toPositionByNodeId);
				}

				PlannerEdge plannerEdge = new PlannerEdge(fromPositionByNodeId, toPositionByNodeId);
				plannerGraph.addEdge(fromPositionByNodeId, toPositionByNodeId, plannerEdge);
				plannerGraph.setEdgeWeight(plannerEdge, edge.length);
			}
		}
		return new ShortestPathPlanner(graph, plannerGraph);
	}

	public static class PlannerEdge extends DefaultWeightedEdge {

		private static final long serialVersionUID = -7578001556242196908L;

		public final int fromPositionByNodeId;
		public final int toPositionByNodeId;

		public PlannerEdge(int fromPositionByNodeId, int toPositionByNodeId) {
			super();
			this.fromPositionByNodeId = fromPositionByNodeId;
			this.toPositionByNodeId = toPositionByNodeId;
		}

	}

}
