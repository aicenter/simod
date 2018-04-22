package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class CHNode {
	public static final int UNCONTRACTED = Integer.MAX_VALUE;
	
	public static void sortNeighborListsAll(Collection<CHNode> nodes) {
        for (CHNode n : nodes) {
            n.sortNeighborLists();
        }
    }
	
	
	
	final Node node;
	
	public final List<CHEdge> outEdges;
	
	public final List<CHEdge> inEdges;
	
	
	public int contractionOrder = UNCONTRACTED;
	
	
	

	CHNode(Node node) {
		this.node = node;
		outEdges = new LinkedList<>();
		inEdges = new LinkedList<>();
	}

	CHNode(int nodeId, float latitude, float longitude) {
		this(new Node(nodeId, 0, longitude, longitude, 0, 0, 0));
	}
	
	void addOutEdge(CHEdge chEdge){
		outEdges.add(chEdge);
	}
	
	void addInEdge(CHEdge chEdge){
		inEdges.add(chEdge);
	}
	
	boolean isContracted() {
        return contractionOrder!=UNCONTRACTED;
    }
	
	int getCountOutgoingUncontractedEdges() {
        int count = 0;
        for (CHEdge edge : outEdges) {
            if (edge.to.contractionOrder == UNCONTRACTED)
                count++;
        }
        return count;
    }

    int getCountIncomingUncontractedEdges() {
        int count = 0;
        for (CHEdge edge : outEdges) {
            if (edge.from.contractionOrder == UNCONTRACTED)
                count++;
        }
        return count;
    }
	
	public List<CHNode> getNeighbors() {
        HashSet<CHNode> neighbors = new HashSet<>();
        for (CHEdge edge : outEdges) {
            neighbors.add(edge.to);
        }
        for (CHEdge edge : inEdges) {
            neighbors.add(edge.from);
        }
        return new ArrayList(neighbors);
    }
	
	/**
     * Sort incoming and outgoing lists of edges. Follows the following rules:
     * 1. Higher contraction order first. We do this so, if we want to find 
     * connected nodes with a higher contraction order than this one, they'll
     * be at the start of the list. When all nodes are contracted, every node
     * will have a different contraction order.
     * 2. Shorter distance first. If contraction orders are equal for two edges,
     * it means either the node on the other end is uncontracted. Shorter edges
     * are usually more interesting, and we want the sort order to be 
     * unambiguous, so this is our second means of ordering.
     * 3. If results are equal for both those tests, sort by edge ID, which 
     * should always be unique, to give us an unambiguous ordering.
     */
    
    public void sortNeighborLists() {
        Collections.sort(outEdges, new Comparator<CHEdge>() {
			
            @Override
            public int compare(CHEdge firstEdge, CHEdge secondEdge) {
                if (secondEdge.to.contractionOrder != firstEdge.to.contractionOrder) {
                    return Integer.compare(secondEdge.to.contractionOrder, firstEdge.to.contractionOrder);
                } else if (secondEdge.cost != firstEdge.cost) {
                    return Integer.compare(firstEdge.cost, secondEdge.cost);
                } else {
                    return Long.compare(firstEdge.to.node.id, secondEdge.to.node.id);
                }
            }
        });
        
        Collections.sort(inEdges, new Comparator<CHEdge>() {
            @Override
            public int compare(CHEdge firstEdge, CHEdge secondEdge) {
                if (secondEdge.from.contractionOrder != firstEdge.from.contractionOrder) {
                    return Integer.compare(secondEdge.from.contractionOrder, firstEdge.from.contractionOrder);
                } else if (secondEdge.cost != firstEdge.cost) {
                    return Integer.compare(firstEdge.cost, secondEdge.cost);
                } else{
                    return Long.compare(firstEdge.from.node.id, secondEdge.from.node.id);
                } 
            }
        });
    }
	
	
}
