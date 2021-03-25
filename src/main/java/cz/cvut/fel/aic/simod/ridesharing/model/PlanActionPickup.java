/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionPickup extends PlanRequestAction {

	/**
	 * Pickup action.
	 * @param request Request
	 * @param node Position where action takes place.
	 * @param maxTime Time constraint in seconds.
	 */
	public PlanActionPickup(PlanComputationRequest request, SimulationNode node, int maxTime) {
		super(request, node, maxTime);
	}

   

	@Override
	public String toString() {
		return String.format("Pick up demand %s at node %s", request.getDemandAgent().getId(), location.id);
	}


}
