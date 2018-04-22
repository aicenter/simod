package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.Collections;
import java.util.List;

public class DijkstraSolution  {

    public final int totalCost;
    public final List<CHNode> nodes;
    public final List<CHEdge> edges;
    
    private final DijkstraSolution preceding; // Can we remove this?
    
    public DijkstraSolution(int totalDriveTime, List<CHNode> nodes, List<CHEdge> edges) {
        this(totalDriveTime, nodes, edges, null);
    }

    public DijkstraSolution(int totalDriveTime, List<CHNode> nodes, List<CHEdge> edges, DijkstraSolution preceding) {
        Preconditions.checkNoneNull(nodes,edges);
        this.totalCost = totalDriveTime;
        if (preceding == null) {
            this.nodes = Collections.unmodifiableList(nodes);
            this.edges = Collections.unmodifiableList(edges);
            this.preceding = null;
        } else {
            this.nodes = new UnionList<>(preceding.nodes,nodes);
            this.edges = new UnionList<>(preceding.edges,edges);
            this.preceding = preceding;
        }
    }
    
    @Override
    public String toString() {
        if (nodes.isEmpty()) {
            return "Empty NodeList, length " + totalCost;
        } else {
            StringBuilder sb = new StringBuilder();
            for (CHNode n : nodes) {
                sb.append(n.node.id).append(",");
            }
            sb.append(String.format(" Duration %.2f secs (%.2f mins)", totalCost/1000.0, totalCost/60000.0));
            return sb.toString();
        }
    }
    
    public CHNode getFirstNode() {
        return nodes.get(0);
    }
    
    public CHNode getLastNode() {
        return nodes.get(nodes.size()-1);
    }
    
    public List<CHEdge> getDeltaEdges() {
        if (edges instanceof UnionList) {
            return ((UnionList)edges).getSecondSublist();
        } else {
            return edges;
        }
    }

    public DijkstraSolution getPreceding() {
        return preceding;
    }
}
