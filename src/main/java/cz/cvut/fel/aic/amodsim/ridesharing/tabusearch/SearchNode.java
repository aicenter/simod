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
    private final short type;
    long time;
    int nodeId;
    int tripId;
        
    public SearchNode(long time, int nodeId){
        this.time = time;
        this.nodeId = nodeId;
        this.type = 0;
    }

    public SearchNode(long time, int nodeId,  int tripId, short type){
        this.time = time;
        this.nodeId = nodeId;
        this.tripId = tripId; 
        this.type = type;
    }
        
    public boolean isStart(){
        return type == 1;
    }
    public boolean isTarget(){
        return type == 2;
    }

    public boolean isTrip(){
        return type != 0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 53 * hash + this.nodeId;
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
        if (this.time != other.time) {
            return false;
        }
        if (this.nodeId != other.nodeId) {
            return false;
        }
        return true;
    }
}
