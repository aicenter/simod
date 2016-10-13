/*
 */
package com.mycompany.testsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.mycompany.testsim.DemandData;
import com.mycompany.testsim.DemandSimulationEntityType;
import com.mycompany.testsim.OnDemandVehicleStationsCentral;
import com.mycompany.testsim.event.OnDemandVehicleStationsCentralEvent;
import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.storage.DemandStorage;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripVisitior;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trips;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.RideAsPassengerActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.RideInVehicleActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.DrivingFinishedActivityCallback;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.PassengerActivityCallback;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.entity.vehicle.VehicleType;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.utils.VelocityConverter;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.Node;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements EventHandler, 
        PassengerActivityCallback<VehicleTrip>{
	
	private final Trip<Long> osmNodeTrip;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
	
	private final boolean precomputedPaths;
    
    private final EventProcessor eventProcessor;
    
    private final RideInVehicleActivity rideAsPassengerActivity;
    
    private final DemandStorage demandStorage;
    
    private final AgentPositionModel agentPositionModel;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;

    
    
    
    @Inject
	public DemandAgent(OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, EventProcessor eventProcessor, 
            RideInVehicleActivity rideAsPassengerActivity, DemandStorage demandStorage, 
            AgentPositionModel agentPositionModel, Map<Long,Node> nodesMappedByNodeSourceIds, 
            @Named("precomputedPaths") boolean precomputedPaths,@Assisted String agentId,
            @Assisted Trip<Long> osmNodeTrip) {
		super(agentId, DemandSimulationEntityType.DEMAND);
		this.osmNodeTrip = osmNodeTrip;
		this.precomputedPaths = precomputedPaths;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.eventProcessor = eventProcessor;
        this.rideAsPassengerActivity = rideAsPassengerActivity;
        this.demandStorage = demandStorage;
        this.agentPositionModel = agentPositionModel;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
	}
	
    
//	public DemandAgent(String agentId, EntityType agentType, Trip<Long> osmNodeTrip, 
//			Map<Long,Integer> nodeIdsMappedByNodeSourceIds, ShortestPathPlanner pathPlanner){
//		this(agentId, agentType, osmNodeTrip, nodeIdsMappedByNodeSourceIds, pathPlanner, true);
//	}

	@Override
	public void born() {
        demandStorage.addEntity(this);
        agentPositionModel.setNewEntityPosition(
                this.getId(), nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().get(0)).getId());
		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null, 
                new DemandData(osmNodeTrip.getLocations(), this));
	}

    @Override
    public void die() {
        demandStorage.removeEntity(this);
    }
    
    

	@Override
	public DescriptionImpl getDescription() {
		return null;
	}

    @Override
    public EventProcessor getEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleEvent(Event event) {
        OnDemandVehicle vehicle = (OnDemandVehicle) event.getContent();
        
        try{
            LinkedList<TripItem> passengerDesitnation = new LinkedList<>();
            passengerDesitnation.add(new TripItem(nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().get(0)).getId()));
            passengerDesitnation.add(new TripItem(nodesMappedByNodeSourceIds.get(osmNodeTrip.getLocations().get(osmNodeTrip.getLocations().size() - 1)).getId()));

            VehicleTrip passengerDriver = new VehicleTrip(passengerDesitnation, EGraphType.HIGHWAY, vehicle.getId());
            rideAsPassengerActivity.usingVehicleAsPassenger(this.getId(), vehicle.getVehicleId(), 
                passengerDriver, this);
           
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void doneFullTrip() {
        die();
    }

    @Override
    public void donePartTrip(VehicleTrip partNotDoneTrip) {
        die();
    }

    @Override
    public void tripFail(VehicleTrip failedTrip) {
        die();
    }

    
//    private void drive(){
//        driveActivity = injector.getInstance(DriveVehicleActivity.class);
//		vehicle = new Vehicle("demand " + getId(), VehicleType.CAR, 5, 5, EGraphType.HIGHWAY);
//		
////		LinkedList<TripItem> finalTrip = new LinkedList<>();
//
//		List<Long> locations = osmNodeTrip.getLocations();
//		long startNodeSourceId = locations.get(0);
//		finalTrips = new Trips();
//		
//		
//		for (int i = 1; i < locations.size(); i++) {
//			Long targetNodeSourceId = locations.get(i);
//			if(precomputedPaths){
//				LinkedList<TripItem> tripItems = new LinkedList<>();
//				tripItems.add(new TripItem(nodeIdsMappedByNodeSourceIds.get(startNodeSourceId)));
//				tripItems.add(new TripItem(nodeIdsMappedByNodeSourceIds.get(targetNodeSourceId)));
//
//				finalTrips.addEndCurrentTrips(new VehicleTrip(tripItems, EGraphType.HIGHWAY, vehicle.getId()));
//			}
//			else{
//				try {
//					Trips trips = pathPlanner.findTrip(vehicle.getId(), nodeIdsMappedByNodeSourceIds.get(startNodeSourceId),
//							nodeIdsMappedByNodeSourceIds.get(targetNodeSourceId));
//					for (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<?> trip : trips) {
//						finalTrips.addEndCurrentTrips(trip);
//					}
//					
//				} catch (TripPlannerException ex) {
//					Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
//				}
//	//			trip.add(new TripItem(nodeIdsMappedByNodeSourceIds.get(locationSourceId)));
//			}
//			startNodeSourceId = targetNodeSourceId;
//		}
//		
////		VehicleTrip vehicleTrip = new VehicleTrip(finalTrip, EGraphType.HIGHWAY, vehicle.getId());
//		
//		double velocityOfVehicle = VelocityConverter.kmph2mps(15);
//		
////		injector.getInstance(VehicleStorage.class).addEntity(vehicle);
////		injector.getInstance(VehiclePositionModel.class).setNewEntityPosition(vehicle.getId(), sourcePos);
//		injector.getInstance(EntityVelocityModel.class).addEntityMaxVelocity(vehicle.getId(), velocityOfVehicle);
//				
//		driveActivity.drive(getId(), vehicle, (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<TripItem>) 
//				finalTrips.getAndRemoveFirstTrip(), this);
//    }
    
    public interface DemandAgentFactory {
        public DemandAgent create(String agentId, Trip<Long> osmNodeTrip);
    }
	
}
