/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 *
 * @author david
 */
public class PlanAction {
	protected final SimulationNode location;
	
	
		
	public SimulationNode getPosition(){
		return location;
	}
	
	

	public PlanAction(SimulationNode location) {
		this.location = location;
	}
	
	
}
