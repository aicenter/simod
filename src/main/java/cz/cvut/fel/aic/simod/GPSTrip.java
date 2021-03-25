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
package cz.cvut.fel.aic.simod;

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
