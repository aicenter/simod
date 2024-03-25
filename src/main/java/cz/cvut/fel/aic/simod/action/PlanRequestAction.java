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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.PlanComputationRequest;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Objects;

@JsonSerialize(using = PlanRequestAction.PlanRequestActionSerializer.class)
public abstract class PlanRequestAction extends TimeWindowAction {

	public static class PlanRequestActionSerializer extends JsonSerializer<PlanRequestAction> {
		@Override
		public void serialize(
			PlanRequestAction action,
			JsonGenerator gen,
			SerializerProvider serializers
		) throws IOException {
			gen.writeStartObject();
			gen.writeFieldName("request_index");
			gen.writeObject(action.getRequest().getId());
			gen.writeFieldName("type");
			if (action instanceof PlanActionPickup)
				gen.writeObject("pickup");
			else
				gen.writeObject("dropoff");
			gen.writeFieldName("position");
			gen.writeObject(action.getPosition().getIndex());
			gen.writeFieldName("min_time");
			gen.writeObject(action.getMinTime());
			gen.writeFieldName("max_time");
			gen.writeObject(action.getMaxTime());
			gen.writeEndObject();
		}
	}

	public final PlanComputationRequest request;


	public PlanComputationRequest getRequest() {
		return request;
	}


	public PlanRequestAction(
		TimeProvider timeProvider,
		PlanComputationRequest request,
		SimulationNode location,
		ZonedDateTime maxTime
	) {
		super(timeProvider, location, timeProvider.getInitDateTime(), maxTime);
		this.request = request;
	}

	public PlanRequestAction(
		TimeProvider timeProvider,
		PlanComputationRequest request,
		SimulationNode location,
		ZonedDateTime minTime,
		ZonedDateTime maxTime
	) {
		super(timeProvider, location, minTime, maxTime);
		this.request = request;
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
