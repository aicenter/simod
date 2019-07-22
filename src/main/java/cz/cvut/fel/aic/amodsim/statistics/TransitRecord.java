/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import java.math.BigInteger;

/**
 *
 * @author david
 */
class TransitRecord {
	public final long time;
	
	public final BigInteger staticId;
	
	public final OnDemandVehicleState vehicleState;

	public TransitRecord(long time, BigInteger staticId, OnDemandVehicleState vehicleState) {
		this.time = time;
		this.staticId = staticId;
		this.vehicleState = vehicleState;
	}
	
	
	
}
