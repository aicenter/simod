/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

/**
 *
 * @author fido
 */
public class Result {
    private final long tickCount;
    
    private final double averageLoadTotal;
    
    private final int maxLoad;
    
    private final double averageKmWithPassenger;
    
    private final double averageKmToStartLocation;
    
    private final double averageKmToStation;
    
    private final double averageKmRebalancing;
    
    private final int numberOfDemandsNotServedFromNearestStation;
    
    private final int numberOfDemandsDropped;
    
    private final int demandsCount;
    
    private final int numberOfVehicles;
    
    private final int numberOfRebalancingDropped;
	
	private final long totalDistanceWithPassenger;
	
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

    public double getAverageKmWithPassenger() {
        return averageKmWithPassenger;
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

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public int getNumberOfRebalancingDropped() {
        return numberOfRebalancingDropped;
    }

	public long getTotalDistanceWithPassenger() {
		return totalDistanceWithPassenger;
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
	
	
    
    
    
    
    

    public Result(long tickCount, double averageLoadTotal, int maxLoad, double averageKmWithPassenger, 
            double averageKmToStartLocation, double averageKmToStation, double averageKmRebalancing,
            int numberOfDemandsNotServedFromNearestStation, int numberOfDemandsDropped, int demandsCount,
            int numberOfVehicles, int numberOfRebalancingDropped, long totalDistanceWithPassenger, 
			long totalDistanceToStartLocation, long totalDistanceToStation, long totalDistanceRebalancing) {
        this.tickCount = tickCount;
        this.averageLoadTotal = averageLoadTotal;
        this.maxLoad = maxLoad;
        this.averageKmWithPassenger = averageKmWithPassenger;
        this.averageKmToStartLocation = averageKmToStartLocation;
        this.averageKmToStation = averageKmToStation;
        this.averageKmRebalancing = averageKmRebalancing;
        this.numberOfDemandsNotServedFromNearestStation = numberOfDemandsNotServedFromNearestStation;
        this.numberOfDemandsDropped = numberOfDemandsDropped;
        this.demandsCount = demandsCount;
        this.numberOfVehicles = numberOfVehicles;
        this.numberOfRebalancingDropped = numberOfRebalancingDropped;
		this.totalDistanceWithPassenger = totalDistanceWithPassenger;
		this.totalDistanceToStartLocation = totalDistanceToStartLocation;
		this.totalDistanceToStation = totalDistanceToStation;
		this.totalDistanceRebalancing = totalDistanceRebalancing;
    }
    
    
    
}
