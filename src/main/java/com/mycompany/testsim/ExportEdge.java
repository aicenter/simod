/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import cz.agents.basestructures.GPSLocation;

/**
 *
 * @author fido
 */
public class ExportEdge {
    private final GPSLocation from;
    
    private final GPSLocation to;
    
    private final long id;
    
    private final int laneCount;
    
    private final float maxSpeed;
    
    private final int length;
    
    
    
    

    public GPSLocation getFrom() {
        return from;
    }

    public GPSLocation getTo() {
        return to;
    }

    public long getId() {
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

    
    
    
    
    public ExportEdge(GPSLocation from, GPSLocation to, long id, int laneCount, float maxSpeed, int length) {
        this.from = from;
        this.to = to;
        this.id = id;
        this.laneCount = laneCount;
        this.maxSpeed = maxSpeed;
        this.length = length;
    }
    
    
    

    
    
    
    
}
