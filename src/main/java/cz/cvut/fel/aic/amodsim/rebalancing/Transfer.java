/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.rebalancing;

import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;

/**
 *
 * @author David Fiedler
 */
public class Transfer {
	public final OnDemandVehicleStation from;
	
	public final OnDemandVehicleStation to;
	
	public int amount;

	public Transfer(OnDemandVehicleStation from, OnDemandVehicleStation to) {
		this.from = from;
		this.to = to;
	}
	
	
}
