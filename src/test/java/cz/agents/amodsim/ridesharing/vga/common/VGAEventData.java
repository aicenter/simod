/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.common;

import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;

/**
 *
 * @author David Fiedler
 */
public class VGAEventData {
	final String onDemandVehicleId;
	
	final int demandId;
	
	final OnDemandVehicleEvent eventType;

	public VGAEventData(String onDemandVehicleId, int demandId, OnDemandVehicleEvent eventType) {
		this.onDemandVehicleId = onDemandVehicleId;
		this.demandId = demandId;
		this.eventType = eventType;
	}
	
	
}
