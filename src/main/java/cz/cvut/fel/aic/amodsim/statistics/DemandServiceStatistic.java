/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

/**
 *
 * @author fido
 */
public class DemandServiceStatistic {
    private final long demandTime;
    
    private final long pickupTime;

    public long getDemandTime() {
        return demandTime;
    }

    public long getPickupTime() {
        return pickupTime;
    }

    public DemandServiceStatistic(long demandTime, long pickupTime) {
        this.demandTime = demandTime;
        this.pickupTime = pickupTime;
    }
    
    
}
