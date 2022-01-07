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
package cz.cvut.fel.aic.simod.ridesharing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import java.util.Objects;

@JsonTypeName(value = "action")
@JsonTypeInfo(include=As.WRAPPER_OBJECT, use=Id.NAME)
public abstract class PlanRequestAction extends PlanAction{

	@JsonIgnore
	public final PlanComputationRequest request;
	
	/**
	 * Time constraint in seconds
	 */
	@JsonProperty("max_time")
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
	
	
	@JsonProperty("request_index")
	public int getRequestId(){
		return request.getId();
	}

	public abstract String getType();
	

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
