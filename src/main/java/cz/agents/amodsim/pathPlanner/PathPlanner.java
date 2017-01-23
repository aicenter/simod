/*
 */
package cz.agents.amodsim.pathPlanner;

import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadEdgeExtended;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.RoadNodeExtended;
import cz.agents.basestructures.Graph;
import cz.agents.planningalgorithms.singlecriteria.algorithms.DijkstraSimpleGraphSingleGoal;
import cz.agents.planningalgorithms.singlecriteria.structures.SingleCriteriaVertexEvaluator;
import cz.agents.planningalgorithms.singlecriteria.structures.SingleIDGoalChecker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author F-I-D-O
 */
public class PathPlanner {
	
	private final Graph<RoadNodeExtended,RoadEdgeExtended> highwayGraph;
	
	private final SimpleSearchGraph searchGraph;
	
	private final Map<Long, Integer> nodeIdsMappedBySourceIds;
	
	private final SimpleEvaluator evaluator;
	
	private DijkstraSimpleGraphSingleGoal<SimpleSearchNode> engine;

	public PathPlanner(Graph<RoadNodeExtended, RoadEdgeExtended> highwayGraph) {
		this.highwayGraph = highwayGraph;
		searchGraph = new SimpleSearchGraph(highwayGraph, 30);
		nodeIdsMappedBySourceIds = highwayGraph.createSourceIdToNodeIdMap();
		evaluator = new SimpleEvaluator();
	}
	
	
	
	public List<Long> findPath(long startNodeSourceId, long targetNodeSourceId){
		Set<SimpleSearchNode> originSearchNodeSet = getStratVerticlesSet(startNodeSourceId);
		engine = new DijkstraSimpleGraphSingleGoal<>(searchGraph, originSearchNodeSet, evaluator, 
				new SingleIDGoalChecker<>(nodeIdsMappedBySourceIds.get(targetNodeSourceId)));
		engine.run();
		
		List<Long> sourceIdsPath = new ArrayList<>();
		for(SimpleSearchNode node : engine.getPath().getNodeSequence()){
			sourceIdsPath.add(highwayGraph.getNode(node.nodeId).getSourceId());
		}
        
		return sourceIdsPath;
	}

	private Set<SimpleSearchNode> getStratVerticlesSet(long startNodeSourceId) {
		Set<SimpleSearchNode> originSearchNodeSet = new HashSet<>();
		originSearchNodeSet.add(new SimpleSearchNode(nodeIdsMappedBySourceIds.get(startNodeSourceId), null, 0));	
		return originSearchNodeSet;
	}
	
	
	
	private class SimpleEvaluator extends SingleCriteriaVertexEvaluator<SimpleSearchNode>{

		@Override
		public int computePrimaryKey(SimpleSearchNode v) {
			return v.getArrivalTime();
		}

		@Override
		public int computeSecondaryKey(SimpleSearchNode v) {
			return 0;
		}
	}
	
	
	
}
