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
    private static final int TW = 180*000; 
    
    
    final int id;
    
    int realTime;
    long[] tw;
    final double[] point;
    final SimulationNode[] nodes;
 

    
    private SearchNode(int id, SimulationNode obj, long startTime){
        this.id = id;
        point = new double[]{obj.getLatitude(), obj.getLongitude()};
        nodes = new SimulationNode[]{obj};
        tw = new long[]{startTime, startTime + TW};
    }
    
    private SearchNode(int id, SimulationEdge obj, long startTime, 
        double[]point){
        this.id = id;
        this.point = point;
        nodes = new SimulationNode[]{obj.fromNode, obj.toNode};
        tw = new long[]{startTime, startTime + TW};
    }
    
    public static SearchNode createNode(int id, Object[] obj, long startTime) {
        if(obj[0].getClass() == SimulationNode.class){
            return new SearchNode(id, (SimulationNode) obj[0], startTime);

        } else if (obj[0].getClass() == SimulationNode.class) {
            return new  SearchNode(id, (SimulationEdge) obj[0], startTime,
                (double[]) obj[1]);
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
