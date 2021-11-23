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
package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
@Singleton
public class DroppedDemandsAnalyzer {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DroppedDemandsAnalyzer.class);
		
        private final SimodConfig config;
	
	protected final OnDemandVehicleStorage vehicleStorage;
	
	private final PositionUtil positionUtil;
	
	private final double maxDistance = 10;
	
	protected final TravelTimeProvider travelTimeProvider;
	
	private final int maxDelayTime = 1000;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

	
	
	
	@Inject
	public DroppedDemandsAnalyzer(
			OnDemandVehicleStorage vehicleStorage, 
			PositionUtil positionUtil, 
			TravelTimeProvider travelTimeProvider, 
			SimodConfig config, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage,
			AgentpolisConfig agentpolisConfig) {
		this.vehicleStorage = vehicleStorage;
		this.positionUtil = positionUtil;
		this.travelTimeProvider = travelTimeProvider;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
                this.config = config;
		
		// max distance in meters between vehicle and request for the vehicle to be considered to serve the request
//		maxDistance = (double) config.ridesharing.maxProlongationInSeconds
//				* agentpolisConfig.maxVehicleSpeedInMeters;
		
		// the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
		// vehicle to be considered to serve the request
//		maxDelayTime = config.ridesharing.maxProlongationInSeconds  * 1000;
	}
	
	
	
	
	
	public void debugFail(PlanComputationRequest request, int[] usedVehiclesPerStation) {
		boolean freeVehicle = false;
		double bestEuclideanDistance = Double.MAX_VALUE;
		double bestTravelTimne = Double.MAX_VALUE;
		
		/*
		 * First check the nearest station 
		 */
		if(config.stations.on){
			OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(
							request.getFrom(), OnDemandvehicleStationStorage.NearestType.TRAVELTIME_FROM);
			double stationDistance;
			double travelTimeFromStation;
			if(usedVehiclesPerStation[nearestStation.getIndex()] >= nearestStation.getParkedVehiclesCount()){
				LOGGER.debug("Cannot serve the request from the nearest station, the nearest station {} is empty!", 
									nearestStation);
			}
			else{
				// euclidean distance check
				stationDistance = positionUtil.getPosition(nearestStation.getPosition())
								.distance(positionUtil.getPosition(request.getFrom()));

				if(stationDistance > maxDistance){
					LOGGER.debug("Cannot serve the request from the nearest station, the nearest station {} is too far! ({}m)", 
										nearestStation, stationDistance);
				}
				else{
					// real feasibility check 
					travelTimeFromStation = 
									travelTimeProvider.getExpectedTravelTime(nearestStation.getPosition(), request.getFrom());
					if(travelTimeFromStation > maxDelayTime){
						LOGGER.debug("Cannot serve the request from the nearest station, the traveltime from the "
											+ "nearest station {} greater than the maximum delay ({}ms)!", 
									nearestStation, maxDelayTime);
					}
					else{
						LOGGER.debug("Cannot serve the request from the nearest station {} from the reason unknown!", 
							nearestStation);
					}
				}
			}			
		}
		
		/*
		Then check the driving cars	
		*/
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
			LOGGER.info("Request " + requestId + ": Cannot serve request - No free vehicle");
		}
		else if(bestEuclideanDistance > maxDistance){
			LOGGER.info("Request " + requestId + ": Cannot serve request - Too big distance: " + bestEuclideanDistance + "m (max distance: " + maxDistance + ")");
		}
		else if(bestTravelTimne + delta > maxDelayTime){
			LOGGER.info("Request " + requestId + ": Cannot serve request - Too big traveltime to startLoaction: " + bestTravelTimne);
		}
		else{
			LOGGER.info("Request " + requestId + ": Cannot serve request - Some other problem - all nearby vehicle plans may be infeasible");
		}
	}
}
