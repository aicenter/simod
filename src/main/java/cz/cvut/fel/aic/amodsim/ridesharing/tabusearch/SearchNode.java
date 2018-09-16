/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class SearchNode {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SearchNode.class);
    
    final int id;
    int[] realTime;
    final int[] pointE6;
    final SimulationNode[] nodes;
 
    
    private SearchNode(int id, SimulationNode obj){
        this.id = id;
        pointE6 = new int[]{obj.latE6, obj.lonE6};
        nodes = new SimulationNode[]{obj};
        realTime = new int[2];
    }
    
    private SearchNode(int id, SimulationEdge obj, int[]point){
        this.id = id;
        pointE6 = point;
        nodes = new SimulationNode[]{obj.fromNode, obj.toNode};
        realTime = new int[2];
    }
    
    public static SearchNode createNode(int id, Object[] obj) {
        if(obj[0].getClass() == SimulationNode.class){
            return new SearchNode(id, (SimulationNode) obj[0]);

        } else if (obj[0].getClass() == SimulationNode.class) {
            GPSLocation loc = (GPSLocation) obj[1];
            return new  SearchNode(id, (SimulationEdge) obj[0], new int[]{loc.latE6, loc.lonE6});
        }
        LOGGER.error("first element of array is neither a node, nor an edge.");
        return null;
    }
    

    @Override
    public int hashCode() {
        int hash = 13;
        hash = 29 * hash + this.id;
        hash = 29 * hash + this.nodes[0].id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SearchNode other = (SearchNode) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    


}
