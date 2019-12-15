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
package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import java.util.Objects;

public abstract class PlanRequestAction extends PlanAction{

	public final PlanComputationRequest request;
	
	
	/**
	 * Time constraint in seconds
	 */
	private final int maxTime;
	
	public PlanComputationRequest getRequest() { 
		return request; 
	}

	/**
	 * Getter for max time.
	 * @return Time constraint in seconds
	 */
	public int getMaxTime() {
		return maxTime;
	}
	
	

	public PlanRequestAction(PlanComputationRequest request, SimulationNode location, int maxTime) {
		super(location);
		this.request = request;
		this.maxTime = maxTime;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PlanRequestAction other = (PlanRequestAction) obj;
		if (!Objects.equals(this.request, other.request)) {
			return false;
		}
		return this.getClass().equals(obj.getClass());
	}

	
}
