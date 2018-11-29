/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 *
 * @author F.I.D.O.
 */
public abstract class Action {
	
	public final Request request;

	public Action(Request request) {
		this.request = request;
	}
	
	
	public abstract int getPossitionId();
	
	
}
