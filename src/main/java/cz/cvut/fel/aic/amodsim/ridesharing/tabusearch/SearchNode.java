/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class SearchNode {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SearchNode.class);
    private static final int TW = 180*000; 
    
    final int i;
    long ti;
    long[] tw;
    final int vi;
    int qi;
    int Fi;
    
 
    
    public SearchNode(int id, SimulationNode node){
        this.i = id;
        this.vi = node.id;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 13;
        hash = 29 * hash + this.i;
        hash = 29 * hash + this.vi;
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
        if (this.i != other.i) {
            return false;
        }
        return true;
    }

    


}
