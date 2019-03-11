/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing;

import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;

/**
 *
 * @author David Fiedler
 */
public class RidesharingEventData {
	public final String onDemandVehicleId;
	
	public final int demandId;
	
	public final OnDemandVehicleEvent eventType;

	public RidesharingEventData(String onDemandVehicleId, int demandId, OnDemandVehicleEvent eventType) {
		this.onDemandVehicleId = onDemandVehicleId;
		this.demandId = demandId;
		this.eventType = eventType;
	}
	
	
}
