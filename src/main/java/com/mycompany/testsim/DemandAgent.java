/*
 */
package com.mycompany.testsim;

import com.google.inject.Injector;
import com.mycompany.testsim.io.Trip;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trips;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.DrivingFinishedActivityCallback;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.entity.vehicle.VehicleType;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.utils.VelocityConverter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements DrivingFinishedActivityCallback{
	
	private final Trip<Long> osmNodeTrip;
	
	private final Injector injector;
	
	private final Map<Long,Integer> nodeIdsMappedByNodeSourceIds;
	
	private final ShortestPathPlanner pathPlanner;
	
	Trips finalTrips;
	
	DriveVehicleActivity driveActivity;
	
	Vehicle vehicle;

	public DemandAgent(String agentId, EntityType agentType, Trip<Long> osmNodeTrip, Injector injector,
			Map<Long,Integer> nodeIdsMappedByNodeSourceIds, ShortestPathPlanner pathPlanner) {
		super(agentId, agentType);
		this.osmNodeTrip = osmNodeTrip;
		this.injector = injector;
		this.nodeIdsMappedByNodeSourceIds = nodeIdsMappedByNodeSourceIds;
		this.pathPlanner = pathPlanner;
	}

	@Override
	public void born() {
		driveActivity = injector.getInstance(DriveVehicleActivity.class);
		vehicle = new Vehicle("demand " + getId(), VehicleType.CAR, 5, 5, EGraphType.HIGHWAY);
		
//		LinkedList<TripItem> finalTrip = new LinkedList<>();

		List<Long> locations = osmNodeTrip.getLocations();
		long startNodeSourceId = locations.get(0);
		finalTrips = new Trips();
		for (int i = 1; i < locations.size(); i++) {
			Long targetNodeSourceId = locations.get(i);
			try {
				Trips trips = pathPlanner.findTrip(vehicle.getId(), nodeIdsMappedByNodeSourceIds.get(startNodeSourceId),
						nodeIdsMappedByNodeSourceIds.get(targetNodeSourceId));
				for (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<?> trip : trips) {
					finalTrips.addEndCurrentTrips(trip);
				}
				startNodeSourceId = targetNodeSourceId;
			} catch (TripPlannerException ex) {
				Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
			}
//			trip.add(new TripItem(nodeIdsMappedByNodeSourceIds.get(locationSourceId)));
			
		}
		
//		VehicleTrip vehicleTrip = new VehicleTrip(finalTrip, EGraphType.HIGHWAY, vehicle.getId());
		
		double velocityOfVehicle = VelocityConverter.kmph2mps(15);
		
//		injector.getInstance(VehicleStorage.class).addEntity(vehicle);
//		injector.getInstance(VehiclePositionModel.class).setNewEntityPosition(vehicle.getId(), sourcePos);
		injector.getInstance(EntityVelocityModel.class).addEntityMaxVelocity(vehicle.getId(), velocityOfVehicle);
				
		driveActivity.drive(getId(), vehicle, (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<TripItem>) 
				finalTrips.getAndRemoveFirstTrip(), this);
	}

	@Override
	public DescriptionImpl getDescription() {
		return null;
	}

	@Override
	public void finishedDriving() {
		if(finalTrips.hasTrip()){
			driveActivity.drive(getId(), vehicle, (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<TripItem>) 
					finalTrips.getAndRemoveFirstTrip(), this);
		}
		else{
			exit();
		}
	}
	
}
