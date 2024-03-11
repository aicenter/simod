/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.action;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.PlanComputationRequest;

public class PlanActionPickup extends PlanRequestAction {

	private final int minTime;



	/**
	 * Getter for min time.
	 * @return Time constraint in seconds
	 */
	public int getMinTime() {
		return minTime;
	}


	/**
	 * Pickup action.
	 * @param request Request
	 * @param node Position where action takes place.
	 * @param maxTime Time constraint in seconds.
	 */
	public PlanActionPickup(PlanComputationRequest request, SimulationNode node, int minTime, int maxTime) {
		super(request, node, maxTime);
		this.minTime = minTime;
	}

   

	@Override
	public String toString() {
		return String.format("Pick up demand %s at node %s", request.getDemandAgent().getId(), location.id);
	}


}
