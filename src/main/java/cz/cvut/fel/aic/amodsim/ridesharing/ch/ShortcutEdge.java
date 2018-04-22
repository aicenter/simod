package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class ShortcutEdge extends CHEdge{
	
	private final UnionList<CHEdge> uncontractedEdges;
	
	final CHEdge shortcutFirstEdge;
	
	final CHEdge shortcutSecondEdge;
	
	
	
	
	public ShortcutEdge(CHEdge first, CHEdge second) {
        this(first.from, second.to, first.cost + second.cost, first, second);
    }
	
	public ShortcutEdge(CHNode fromNode, CHNode toNode, int cost, CHEdge shortcutFirstEdge, CHEdge shortcutSecondEdge) {
		super(fromNode, toNode, cost, Math.max(shortcutFirstEdge.contractionDepth, shortcutSecondEdge.contractionDepth));
		this.shortcutFirstEdge = shortcutFirstEdge;
		this.shortcutSecondEdge = shortcutSecondEdge;
		uncontractedEdges = new UnionList<>(shortcutFirstEdge.getUncontractedEdges(), 
				shortcutSecondEdge.getUncontractedEdges());
	}

	
	
	
	public List<CHEdge> getUncontractedEdges() {
        return uncontractedEdges;
    }
	
}
