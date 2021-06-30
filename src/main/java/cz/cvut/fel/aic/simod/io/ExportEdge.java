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

import cz.cvut.fel.aic.geographtools.GPSLocation;

/**
 *
 * @author fido
 */
public class ExportEdge {
	private final GPSLocation from;
	
	private final GPSLocation to;
	
	private final String id;
	
	private final int laneCount;
	
	private final double maxSpeed;
	
	private final int length;
	
	
	
	

	public GPSLocation getFrom() {
		return from;
	}

	public GPSLocation getTo() {
		return to;
	}

	public String getId() {
		return id;
	}

	public int getLaneCount() {
		return laneCount;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public int getLength() {
		return length;
	}

	
	
	
	
	public ExportEdge(GPSLocation from, GPSLocation to, String id, int laneCount, double maxSpeed, int length) {
		this.from = from;
		this.to = to;
		this.id = id;
		this.laneCount = laneCount;
		this.maxSpeed = maxSpeed;
		this.length = length;
	}
	
	
	

	
	
	
	
}
