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
public class DropOffAction extends Action{

	public DropOffAction(Request request) {
		super(request);
	}

	@Override
	public int getPossitionId() {
		return request.toId;
	}

}
