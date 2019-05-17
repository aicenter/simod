/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.event;

import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;

/**
 *
 * @author david
 */
public class RebalancingEventContent extends OnDemandVehicleEventContent {
	
	public final OnDemandVehicleStation from;
	
	public final OnDemandVehicleStation to;
	
	public RebalancingEventContent(long time, int demandId, String onDemandVehicleId, OnDemandVehicleStation from, 
			OnDemandVehicleStation to) {
		super(time, demandId, onDemandVehicleId);
		this.from = from;
		this.to = to;
	}
	
	
}
