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
public class PickupEventContent extends OnDemandVehicleEventContent{
    
    private final int demandTripLength;

    public int getDemandTripLength() {
        return demandTripLength;
    }
    
    
    
    public PickupEventContent(long time, int demandId, String onDemandVehicleId, int demandTripLength) {
        super(time, demandId, onDemandVehicleId);
        this.demandTripLength = demandTripLength;
    }
    
}
