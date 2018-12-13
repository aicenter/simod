/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanPickup;

/**
 *
 * @author LocalAdmin
 */
public interface PlanComputationRequest {

	@Override
	public boolean equals(Object obj);
	
	public int getMaxPickupTime();
	
	public int getMaxDropoffTime();
	
	public int getOriginTime();
	
	public int getMinTravelTime();
	
	public SimulationNode getFrom();
	
	public SimulationNode getTo();
	
	public boolean isOnboard();
	
	public VGAVehiclePlanPickup getPickUpAction();
	
	public VGAVehiclePlanDropoff getDropOffAction();
}
