package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public abstract class CHEdge {
	
	
	public final CHNode from;
	
	public final CHNode to;
	
	final int cost;
	
	public final int contractionDepth;
	
	final long id;

	CHEdge(CHNode fromNode, CHNode toNode, int cost, int contractionDepth) {
		this.cost = cost;
		this.contractionDepth = contractionDepth;
		from = fromNode;
		to = toNode;
		id = Long.parseLong(String.format("%d%d", from.node.id, to.node.id));
	}
	
	public abstract List<CHEdge> getUncontractedEdges();
	
//	public CHEdge cloneWithEdgeId(long edgeId) {
//        Preconditions.require(this.edgeId==PLACEHOLDER_ID_DO_NOT_SERIALIZE, contractionDepth>0);
//        return new DirectedEdge(edgeId, PLACEHOLDER_ID_NO_SOURCE_DATA_EQUIVALENT, from, to, driveTimeMs, accessOnly, first, second);
//    }
	
	
}
