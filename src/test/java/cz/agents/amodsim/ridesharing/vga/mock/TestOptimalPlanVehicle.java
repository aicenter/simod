/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class TestOptimalPlanVehicle implements IOptimalPlanVehicle{
	
	private final Set<PlanComputationRequest> requestsOnBoard;
	
	private final SimulationNode position;
	
	private final int capacity;

	public TestOptimalPlanVehicle(Set<PlanComputationRequest> requestsOnBoard, SimulationNode position, int capacity) {
		this.requestsOnBoard = requestsOnBoard;
		this.position = position;
		this.capacity = capacity;
	}
	
	

	@Override
	public Set<PlanComputationRequest> getRequestsOnBoard() {
		return requestsOnBoard;
	}

	@Override
	public SimulationNode getPosition() {
		return position;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

}
