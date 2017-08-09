/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;

/**
 *
 * @author david
 */
public class GPSTrip {
	private final ArrayList<GPSLocation> locations;
	
	private final long startTime;
	
	private final long endTime;

	
	
	
	public ArrayList<GPSLocation> getLocations() {
		return locations;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
	
	

	public GPSTrip(ArrayList<GPSLocation> locations, long startTime, long endTime) {
		this.locations = locations;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	
}
