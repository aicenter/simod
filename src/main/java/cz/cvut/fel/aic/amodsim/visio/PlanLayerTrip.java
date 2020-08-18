/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.visio;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;

/**
 *
 * @author david
 */
public class PlanLayerTrip extends Trip<SimulationNode> {
	private final PlanRequestAction task;

	
	
	
	public PlanRequestAction getTask() {
		return task;
	}
	
	

	public PlanLayerTrip(int tripId,PlanRequestAction task, SimulationNode... locations) {
		super(tripId,locations);
		this.task = task;
	}
	
	
}
