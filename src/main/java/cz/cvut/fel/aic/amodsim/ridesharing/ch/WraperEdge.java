package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import cz.cvut.fel.aic.geographtools.Edge;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class WraperEdge extends CHEdge{
	public final Edge edge;

	WraperEdge(long edgeId, CHNode fromNode, CHNode toNode, int cost) {
		this(new Edge(fromNode.node.id, toNode.node.id, 0), fromNode, toNode, cost);
	}
	
	public WraperEdge(Edge edge, CHNode fromNode, CHNode toNode, int cost) {
		super(fromNode, toNode, cost, 0);
		this.edge = edge;
	}

	

	@Override
	public List<CHEdge> getUncontractedEdges() {
		return Collections.singletonList(this);
	}
}
