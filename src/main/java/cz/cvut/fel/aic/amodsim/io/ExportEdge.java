/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.io;

import cz.cvut.fel.aic.geographtools.GPSLocation;

/**
 *
 * @author fido
 */
public class ExportEdge {
    private final GPSLocation from;
    
    private final GPSLocation to;
    
    private final String id;
    
    private final int laneCount;
    
    private final float maxSpeed;
    
    private final int length;
    
    
    
    

    public GPSLocation getFrom() {
        return from;
    }

    public GPSLocation getTo() {
        return to;
    }

    public String getId() {
        return id;
    }

    public int getLaneCount() {
        return laneCount;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public int getLength() {
        return length;
    }

    
    
    
    
    public ExportEdge(GPSLocation from, GPSLocation to, String id, int laneCount, float maxSpeed, int length) {
        this.from = from;
        this.to = to;
        this.id = id;
        this.laneCount = laneCount;
        this.maxSpeed = maxSpeed;
        this.length = length;
    }
    
    
    

    
    
    
    
}