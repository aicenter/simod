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
package cz.cvut.fel.aic.simod.statistics;

/**
 *
 * @author fido
 */
public class DemandServiceStatistic {
	private final long demandTime;
	
	private final long pickupTime;
	
	private final long dropoffTime;
	
	private final long minPossibleServiceDelay;
	
	private final String demandId;
	
	private final String vehicleId;

	public long getDemandTime() {
		return demandTime;
	}

	public long getDropoffTime() {
		return dropoffTime;
	}

	public long getPickupTime() {
		return pickupTime;
	}
	
	public long getMinPossibleServiceDelay() {
		return minPossibleServiceDelay;
	}

	public String getDemandId() {
		return demandId;
	}

	public String getVehicleId() {
		return vehicleId;
	}
	
	

	public DemandServiceStatistic(long demandTime, long pickupTime, long dropoffTime, long minPossibleServiceDelay,
			String demandId, 
			String vehicleId) {
		this.demandTime = demandTime;
		this.pickupTime = pickupTime;
		this.dropoffTime = dropoffTime;
		this.minPossibleServiceDelay = minPossibleServiceDelay;
		this.demandId = demandId;
		this.vehicleId = vehicleId;
	}
	
	
}
