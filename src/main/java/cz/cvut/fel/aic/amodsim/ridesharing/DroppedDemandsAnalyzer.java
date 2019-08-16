/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

/**
 *
 * @author david
 */
@Singleton
public class DroppedDemandsAnalyzer {
	
	protected final OnDemandVehicleStorage vehicleStorage;
	
	private final PositionUtil positionUtil;
	
	private final double maxDistance;
	
	protected final TravelTimeProvider travelTimeProvider;
	
	private final int maxDelayTime;

	
	
	
	@Inject
	public DroppedDemandsAnalyzer(OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, 
			TravelTimeProvider travelTimeProvider, AmodsimConfig config) {
		this.vehicleStorage = vehicleStorage;
		this.positionUtil = positionUtil;
		this.travelTimeProvider = travelTimeProvider;
		
		// max distance in meters between vehicle and request for the vehicle to be considered to serve the request
		maxDistance = (double) config.ridesharing.maxProlongationInSeconds 
				* config.vehicleSpeedInMeters;
		
		// the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
		// vehicle to be considered to serve the request
		maxDelayTime = config.ridesharing.maxProlongationInSeconds  * 1000;
	}
	
	
	
	
	
	public void debugFail(PlanComputationRequest request) {
		boolean freeVehicle = false;
		double bestEuclideanDistance = Double.MAX_VALUE;
		double bestTravelTimne = Double.MAX_VALUE;
		
		for(OnDemandVehicle tVvehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
			if(vehicle.hasFreeCapacity()){
				freeVehicle = true;
				
				// cartesian distance check
				double distance = positionUtil.getPosition(vehicle.getPosition())
						.distance(positionUtil.getPosition(request.getFrom()));

				if(distance < bestEuclideanDistance){
					bestEuclideanDistance = distance;
				}
				
				
				if(distance < maxDistance){
					// real feasibility check 
					double travelTime = 
							travelTimeProvider.getTravelTime(vehicle, request.getFrom());


					if(travelTime < bestTravelTimne){
						bestTravelTimne = travelTime;
					}
				}
			}
		}	
		int delta = 5000;
		String requestId = request.getDemandAgent().getId();
		if(!freeVehicle){
			System.out.println("Request " + requestId + ": Cannot serve request - No free vehicle");
		}
		else if(bestEuclideanDistance > maxDistance){
			System.out.println("Request " + requestId + ": Cannot serve request - Too big distance: " + bestEuclideanDistance + "m (max distance: " + maxDistance + ")");
		}
		else if(bestTravelTimne + delta > maxDelayTime){
			System.out.println("Request " + requestId + ": Cannot serve request - Too big traveltime to startLoaction: " + bestTravelTimne);
		}
		else{
			System.out.println("Request " + requestId + ": Cannot serve request - Some other problem - all nearby vehicle plans infeasible?");
		}
	}
}
