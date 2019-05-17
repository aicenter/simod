/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;

/**
 *
 * @author david
 */
public class PlanActionCurrentPosition extends PlanAction{
	
	public PlanActionCurrentPosition(SimulationNode position) {
		super(position);
	}
}
