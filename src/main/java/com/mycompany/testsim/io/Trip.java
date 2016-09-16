package com.mycompany.testsim.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;

/**
 *
 * @author F-I-D-O
 * @param <L>
 */
public class Trip<L> {
	private final ArrayList<L> locations;
	
	private final long startTime;
	
	private final long endTime;

	
	
	
	public ArrayList<L> getLocations() {
		return locations;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
	
	
	@JsonCreator
	public Trip(@JsonProperty("locations") ArrayList<L> locations, @JsonProperty("startTime") long startTime, 
			@JsonProperty("endTime") long endTime) {
		this.locations = locations;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
