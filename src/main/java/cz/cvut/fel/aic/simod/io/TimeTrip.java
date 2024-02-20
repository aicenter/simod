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
package cz.cvut.fel.aic.simod.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.geographtools.WKTPrintableCoord;

import java.time.ZonedDateTime;

/**
 *
 * @author F-I-D-O
 * @param <L> location type
 */
public class TimeTrip<L extends WKTPrintableCoord> extends Trip<L>{
		
	private final ZonedDateTime startTime;
	
	private final ZonedDateTime endTime;

	
	
	
	public ZonedDateTime getStartTime() {
		return startTime;
	}

	public ZonedDateTime getEndTime() {
		return endTime;
	}
	
	
	@JsonCreator
	public TimeTrip(int tripId,@JsonProperty("startTime") ZonedDateTime startTime, @JsonProperty("endTime") ZonedDateTime endTime,
			@JsonProperty("locations") L... locations){
		super(tripId,locations);
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public TimeTrip(int tripId,ZonedDateTime startTime, L... locations){
		this(tripId,startTime, ZonedDateTime.now(), locations);
	}

	@Override
	@JsonIgnore
	public L removeFirstLocation() {
		return super.removeFirstLocation();
	}

	@JsonIgnore
	@Override
	public boolean isEmpty() {
		return super.isEmpty(); 
	}
	
	
	
	
}
