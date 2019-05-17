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
