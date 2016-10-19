/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.mycompany.testsim.DemandSimulationEntityType;
import com.mycompany.testsim.OnDemandVehicleStationsCentral;
import com.mycompany.testsim.PlanningAgent;
import com.mycompany.testsim.TripsUtil;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.DrivingFinishedActivityCallback;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.utils.VelocityConverter;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fido
 */
public class OnDemandVehicle extends Agent implements EventHandler, DrivingFinishedActivityCallback, PlanningAgent {
    
    private static final double LENGTH = 4;
    
    private static final int CAPACITY = 5;
    
    private static final int VELOCITY = 15;
    
    
    
    
    
    private final Vehicle vehicle;
    
    private final DriveVehicleActivity driveVehicleActivity;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;
    
    private final TripsUtil tripsUtil;
    
    private final VehiclePositionModel vehiclePositionModel;
    
    private final boolean precomputedPaths;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    
    
    private List<Node> demandNodes;
    
    private OnDemandVehicleState state;
    
    private OnDemandVehicleStation targetStation;
    
    private VehicleTrip currentTrip;
    
    private VehicleTrip demandTrip;
	
	private VehicleTrip tripToStation;
	
	private VehicleTrip completeTrip;
    
    
    
    public VehicleTrip getCurrentTrips() {
        return currentTrip;
    }

    public VehicleTrip getDemandTrip() {
        return demandTrip.clone();
    }

    public OnDemandVehicleState getState() {
        return state;
    }
    
    
    
    
    
    @Inject
    public OnDemandVehicle(DriveVehicleActivity driveVehicleActivity, Map<Long,Node> nodesMappedByNodeSourceIds, 
            VehicleStorage vehicleStorage, EntityVelocityModel entityVelocityModel, 
            VehiclePositionModel vehiclePositionModel, TripsUtil tripsUtil, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral,
            @Named("precomputedPaths") boolean precomputedPaths, @Assisted String vehicleId, 
            @Assisted Node startPosition) {
        super(vehicleId + " - autonomus agent", DemandSimulationEntityType.ON_DEMAND_VEHICLE);
        this.driveVehicleActivity = driveVehicleActivity;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
        this.tripsUtil = tripsUtil;
        this.vehiclePositionModel = vehiclePositionModel;
        this.precomputedPaths = precomputedPaths;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        
        vehicle = new Vehicle(vehicleId + " - vehicle", DemandSimulationEntityType.VEHICLE, LENGTH, CAPACITY, EGraphType.HIGHWAY);
        
        vehicleStorage.addEntity(vehicle);
        entityVelocityModel.addEntityMaxVelocity(vehicle.getId(), VelocityConverter.kmph2mps(VELOCITY));
        vehiclePositionModel.setNewEntityPosition(vehicle.getId(), startPosition.getId());
        state = OnDemandVehicleState.WAITING;
    }

    @Override
    public DescriptionImpl getDescription() {
        return null;
    }

    @Override
    public EventProcessor getEventProcessor() {
        return null;
    }
    
    public String getVehicleId(){
        return vehicle.getId();
    }

    @Override
    public void handleEvent(Event event) {
        List<Long> locations = (List<Long>) event.getContent();
        demandNodes = new ArrayList<>();
		if(precomputedPaths){
			for (Long location : locations) {
				demandNodes.add(nodesMappedByNodeSourceIds.get(location));
				if(nodesMappedByNodeSourceIds.get(location) == null){
					System.out.println("com.mycompany.testsim.entity.OnDemandVehicle.handleEvent()");
				}
			}
		}
		else{
			demandNodes.add(nodesMappedByNodeSourceIds.get(locations.get(0)));
			demandNodes.add(nodesMappedByNodeSourceIds.get(locations.get(locations.size() - 1)));
		}
        driveToDemandStartLocation();
    }
    
    @Override
    public void finishedDriving() {
        switch(state){
            case DRIVING_TO_START_LOCATION:
                driveToTargetLocation();
                break;
            case DRIVING_TO_TARGET_LOCATION:
                driveToNearestStation();
                break;
            case DRIVING_TO_STATION:
            case REBALANCING:
                waitInStation();
                break;
        }
    }

    private void driveToDemandStartLocation() {
        
        if(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()) ==  demandNodes.get(0).getId()){
			currentTrip = null;
		}
		else{
			currentTrip = tripsUtil.createTrip(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()), 
                demandNodes.get(0).getId(), vehicle);
		}
        
        demandTrip = tripsUtil.locationsToTrips(demandNodes, precomputedPaths, vehicle);
		
		demandTrip.getLocations().remove();
		
		Node demandEndNode = demandNodes.get(demandNodes.size() - 1);
		
		targetStation = onDemandVehicleStationsCentral.getNearestReadyStation(demandEndNode);
		
		if(demandEndNode.getId() == targetStation.getPositionInGraph().getId()){
			tripToStation = null;
		}
		else{
			tripToStation = tripsUtil.createTrip(demandEndNode.getId(), 
					targetStation.getPositionInGraph().getId(), vehicle);
		}
		
		completeTrip = TripsUtil.mergeTrips(currentTrip, demandTrip, tripToStation);
		
		if(currentTrip == null){
			driveToTargetLocation();
			return;
		}
		
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
//				
		driveVehicleActivity.drive(getId(), vehicle, currentTrip, this);
    }

    

    private void driveToTargetLocation() {
        state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        currentTrip = demandTrip;
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrip, this);
    }

    private void driveToNearestStation() {
		if(tripToStation == null){
			waitInStation();
			return;
		}
		
        state = OnDemandVehicleState.DRIVING_TO_STATION;

        currentTrip = tripToStation;  
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrip, this);
    }

    private void waitInStation() {
        state = OnDemandVehicleState.WAITING;
		completeTrip = null;
        targetStation.parkVehicle(this);
    }

	@Override
	public VehicleTrip getCurrentPlan() {
		return completeTrip;
	}
	
	public Node getDemandTarget(){
		return demandNodes.get(demandNodes.size() - 1);
	}

    void driveToStation(OnDemandVehicleStation targetStation) {
        state = OnDemandVehicleState.REBALANCING;
        
        currentTrip = tripsUtil.createTrip(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()), 
                targetStation.getPositionInGraph().getId(), vehicle);
        
        this.targetStation = targetStation;
        
        driveVehicleActivity.drive(getId(), vehicle, currentTrip, this);
    }
    
    
    
    
    public interface OnDemandVehicleFactory {
        public OnDemandVehicle create(String id, Node startPosition);
    }
    
}
