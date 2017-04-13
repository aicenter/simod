/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.statistics;

/**
 *
 * @author fido
 */
public class OnDemandVehicleEventContent {
    private final long time;
    
    private final int id;

    public long getTime() {
        return time;
    }

    public int getId() {
        return id;
    }

    public OnDemandVehicleEventContent(long time, int id) {
        this.time = time;
        this.id = id;
    }
    
    
}
