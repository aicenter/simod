package com.mycompany.testsim.io;

import java.util.ArrayList;

/**
 *
 * @author F-I-D-O
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
	
	

	public Trip(ArrayList<L> locations, long startTime, long endTime) {
		this.locations = locations;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
