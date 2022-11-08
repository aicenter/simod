package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.statistics.Result;

public class ResultPF extends Result {

	private final int totalSharedRequests;

	private final int numberPassengersDropped;

	private final int numberPackagesDropped;

	public int getTotalSharedRequests() {
		return totalSharedRequests;
	}

	public int getNumberPassengersDropped() {
		return numberPassengersDropped;
	}

	public int getNumberPackagesDropped() {
		return numberPackagesDropped;
	}


	public ResultPF(long tickCount, double averageLoadTotal, int maxLoad, double averageKmWithPassenger,
					double averageKmToStartLocation, double averageKmToStation, double averageKmRebalancing,
					int numberOfDemandsNotServedFromNearestStation, int numberPassengersDropped, int numberPackagesDropped, int demandsCount,
					int numberOfVehicles, int numberOfRebalancingDropped, long totalDistanceWithPassenger,
					long totalDistanceToStartLocation, long totalDistanceToStation, long totalDistanceRebalancing,
					int totalSharedRequests) {
		super(tickCount, averageLoadTotal, maxLoad, averageKmWithPassenger, averageKmToStartLocation, averageKmToStation,
				averageKmRebalancing, numberOfDemandsNotServedFromNearestStation, numberPassengersDropped + numberPassengersDropped,
				demandsCount, numberOfVehicles, numberOfRebalancingDropped, totalDistanceWithPassenger,
				totalDistanceToStartLocation, totalDistanceToStation, totalDistanceRebalancing);
		this.totalSharedRequests = totalSharedRequests;
		this.numberPassengersDropped = numberPassengersDropped;
		this.numberPackagesDropped = numberPackagesDropped;
	}
}
