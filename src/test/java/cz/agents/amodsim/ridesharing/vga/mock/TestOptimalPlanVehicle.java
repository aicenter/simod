/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
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
