/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

/**
 *
 * @author LocalAdmin
 */
public interface PlanComputationRequest {

	@Override
	public boolean equals(Object obj);
	
	public int getMaxPickupTime();
	
	public int getMaxDropoffTime();
	
	/**
	 * Returns request origin time in seconds.
	 * @return Request origin time in seconds.
	 */
	public int getOriginTime();
	
	/**
	 * Returns min travel time in seconds.
	 * @return Min travel time in seconds.
	 */
	public int getMinTravelTime();
	
	public SimulationNode getFrom();
	
	public SimulationNode getTo();
	
	public boolean isOnboard();
	
	public PlanActionPickup getPickUpAction();
	
	public PlanActionDropoff getDropOffAction();
	
	public DemandAgent getDemandAgent();
}
