
package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.AbstractList;
import java.util.List;


public class NodeListFromEdgeList extends AbstractList<CHNode> {
    private final List<CHEdge> edges;

    public NodeListFromEdgeList(List<CHEdge> edges) {
        Preconditions.checkNoneNull(edges);
        Preconditions.require(edges.size() > 0);
        
        if (edges.size() > 1) {
            if (edges.get(0).to != edges.get(1).from) {
                throw new RuntimeException("This class only supports forward edge lists at the moment, sorry.");
            }
        }
        
        this.edges = edges;
    }
    
    @Override
    public CHNode get(int index) {
        if (index==0) {
            return edges.get(0).from;
        } else {
            return edges.get(index-1).to;
        }
    }

    @Override
    public int size() {
        return edges.size()+1;
    }

}
