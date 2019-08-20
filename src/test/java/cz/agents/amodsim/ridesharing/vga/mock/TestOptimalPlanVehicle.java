/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.util.LinkedHashSet;

/**
 *
 * @author F.I.D.O.
 */
public class TestOptimalPlanVehicle implements IOptimalPlanVehicle{
	
	private final LinkedHashSet<PlanComputationRequest> requestsOnBoard;
	
	private final SimulationNode position;
	
	private final int capacity;
	
	private final TestOnDemandVehicle testOnDemandVehicle;

	public TestOptimalPlanVehicle(LinkedHashSet<PlanComputationRequest> requestsOnBoard, SimulationNode position, 
			int capacity, TestOnDemandVehicle testOnDemandVehicle) {
		this.requestsOnBoard = requestsOnBoard;
		this.position = position;
		this.capacity = capacity;
		this.testOnDemandVehicle = testOnDemandVehicle;
	}
	
	

	@Override
	public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() {
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

	@Override
	public String getId() {
		return "TestOptimalPlanVehicle";
	}

	@Override
	public MovingEntity getRealVehicle() {
		return testOnDemandVehicle;
	}

}
