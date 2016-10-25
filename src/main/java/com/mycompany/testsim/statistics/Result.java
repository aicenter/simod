/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.statistics;

/**
 *
 * @author fido
 */
public class Result {
    private final long tickCount;
    
    private final double averageLoadTotal;
    
    private final int maxLoad;

    
    
    
    
    public long getTickCount() {
        return tickCount;
    }

    public double getAverageLoadTotal() {
        return averageLoadTotal;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    
    
    
    
    public Result(long tickCount, double averageLoadTotal, int maxLoad) {
        this.tickCount = tickCount;
        this.averageLoadTotal = averageLoadTotal;
        this.maxLoad = maxLoad;
    }
    
    
    
}
