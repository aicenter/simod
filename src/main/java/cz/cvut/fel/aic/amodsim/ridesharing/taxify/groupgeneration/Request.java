/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.TravelTimeProviderTaxify;

/**
 *
 * @author F.I.D.O.
 */
public class Request {
	
//	private final TripTaxify trip;
	
	public final long time;
	
	public final int fromId;
	
	public final int toId;
	
	public final long minTravelTime;
	
	public final long maxPickUpTime;
	
	public final long maxDropOffTime;

	
	

	public Request(long time, int fromId, int toId, TravelTimeProviderTaxify travelTimeProvider, 
			AmodsimConfig config) {
//		this.trip = trip;
		this.time = time;
		this.fromId = fromId;
		this.toId = toId;
		minTravelTime = (long) travelTimeProvider.getTravelTimeInMillis(fromId, toId);
		
		long maximumProlongation = (long) ((config.amodsim.ridesharing.maxRelativeDiscomfort) * minTravelTime);
		maxPickUpTime = time + maximumProlongation;
		maxDropOffTime = time + minTravelTime + maximumProlongation;
	}
	
	
}
