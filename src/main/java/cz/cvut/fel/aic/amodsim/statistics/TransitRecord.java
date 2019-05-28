/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;

/**
 *
 * @author david
 */
class TransitRecord {
	public final long time;
	
	public final long osmId;
	
	public final OnDemandVehicleState vehicleState;

	public TransitRecord(long time, long osmId, OnDemandVehicleState vehicleState) {
		this.time = time;
		this.osmId = osmId;
		this.vehicleState = vehicleState;
	}
	
	
	
}
