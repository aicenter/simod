/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

/**
 *
 * @author olga
 */
public class SearchNode {
    final int id;
    int realTime;
    final int simNodeId;
        
    public SearchNode(int id, int simNodeId){
        this.id = id;
        this.simNodeId = simNodeId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + this.id;
        hash = 29 * hash + this.simNodeId;
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
