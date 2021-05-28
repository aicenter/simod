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
package cz.cvut.fel.aic.simod.statistics;

/**
 *
 * @author fido
 */
public class Result {
	private final long tickCount;
	
	private final double averageLoadTotal;
	
	private final int maxLoad;
	
	private final double averageKmWithOrder;
	
	private final double averageKmToStartLocation;
	
	private final double averageKmToStation;
	
	private final double averageKmRebalancing;
	
	private final int numberOfDemandsNotServedFromNearestStation;
	
	private final int numberOfDemandsDropped;

	private final int numberOfParcelsDropped;
	
	private final int demandsCount;

	private final int parcelsCount;
	
	private final int numberOfVehicles;
	
	private final int numberOfRebalancingDropped;
	
	private final long totalDistanceWithOrder;
	
	private final long totalDistanceToStartLocation;
	
	private final long totalDistanceToStation;
	
	private final long totalDistanceRebalancing;

	
	
	
	
	public long getTickCount() {
		return tickCount;
	}

	public double getAverageLoadTotal() {
		return averageLoadTotal;
	}

	public int getMaxLoad() {
		return maxLoad;
	}

	public double getAverageKmWithOrder() {
		return averageKmWithOrder;
	}

	public double getAverageKmToStartLocation() {
		return averageKmToStartLocation;
	}

	public double getAverageKmToStation() {
		return averageKmToStation;
	}

	public double getAverageKmRebalancing() {
		return averageKmRebalancing;
	}

	public int getNumberOfDemandsNotServedFromNearestStation() {
		return numberOfDemandsNotServedFromNearestStation;
	}

	public int getNumberOfDemandsDropped() {
		return numberOfDemandsDropped;
	}

	public int getDemandsCount() {
		return demandsCount;
	}

	public int getNumberOfParcelsDropped() {
		return numberOfParcelsDropped;
	}

	public int getParcelsCount() {
		return parcelsCount;
	}

	public int getNumberOfVehicles() {
		return numberOfVehicles;
	}

	public int getNumberOfRebalancingDropped() {
		return numberOfRebalancingDropped;
	}

	public long getTotalDistanceWithOrder() {
		return totalDistanceWithOrder;
	}

	public long getTotalDistanceToStartLocation() {
		return totalDistanceToStartLocation;
	}

	public long getTotalDistanceToStation() {
		return totalDistanceToStation;
	}

	public long getTotalDistanceRebalancing() {
		return totalDistanceRebalancing;
	}
	
	
	
	
	
	
	

	public Result(long tickCount, double averageLoadTotal, int maxLoad, double averageKmWithOrder,
				  double averageKmToStartLocation, double averageKmToStation, double averageKmRebalancing,
				  int numberOfDemandsNotServedFromNearestStation, int numberOfDemandsDropped, int demandsCount,
				  int numberOfParcelsDropped, int parcelsCount, int numberOfVehicles, int numberOfRebalancingDropped,
				  long totalDistanceWithOrder, long totalDistanceToStartLocation, long totalDistanceToStation,
				  long totalDistanceRebalancing) {
		this.tickCount = tickCount;
		this.averageLoadTotal = averageLoadTotal;
		this.maxLoad = maxLoad;
		this.averageKmWithOrder = averageKmWithOrder;
		this.averageKmToStartLocation = averageKmToStartLocation;
		this.averageKmToStation = averageKmToStation;
		this.averageKmRebalancing = averageKmRebalancing;
		this.numberOfDemandsNotServedFromNearestStation = numberOfDemandsNotServedFromNearestStation;
		this.numberOfDemandsDropped = numberOfDemandsDropped;
		this.demandsCount = demandsCount;
		this.numberOfParcelsDropped = numberOfParcelsDropped;
		this.parcelsCount = parcelsCount;
		this.numberOfVehicles = numberOfVehicles;
		this.numberOfRebalancingDropped = numberOfRebalancingDropped;
		this.totalDistanceWithOrder = totalDistanceWithOrder;
		this.totalDistanceToStartLocation = totalDistanceToStartLocation;
		this.totalDistanceToStation = totalDistanceToStation;
		this.totalDistanceRebalancing = totalDistanceRebalancing;
	}
	
	
	
}
