/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.event;

/**
 *
 * @author fido
 */
public class OnDemandVehicleEventContent {
    private final long time;
    
    private final int demandId;
	
	private final String onDemandVehicleId;

    public long getTime() {
        return time;
    }

    public int getDemandId() {
        return demandId;
    }

	public String getOnDemandVehicleId() {
		return onDemandVehicleId;
	}
	
	

	public OnDemandVehicleEventContent(long time, int demandId, String onDemandVehicleId) {
		this.time = time;
		this.demandId = demandId;
		this.onDemandVehicleId = onDemandVehicleId;
	}
}
