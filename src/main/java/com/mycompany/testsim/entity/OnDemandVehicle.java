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
public class OnDemandVehicle extends Agent implements EventHandler, DrivingFinishedActivityCallback{
    
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
    
    private VehicleTrip currentTrips;
    
    private VehicleTrip demandTrips;

    
    
    
    public VehicleTrip getCurrentTrips() {
        return currentTrips;
    }

    public VehicleTrip getDemandTrips() {
        return demandTrips.clone();
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
        
        vehicle = new Vehicle(vehicleId, DemandSimulationEntityType.VEHICLE, LENGTH, CAPACITY, EGraphType.HIGHWAY);
        
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
        for (Long location : locations) {
            demandNodes.add(nodesMappedByNodeSourceIds.get(location));
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
                waitInStation();
                break;
        }
    }

    private void driveToDemandStartLocation() {
        state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
        
		currentTrips = tripsUtil.createTrips(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()), 
                demandNodes.get(0).getId(), vehicle);
        
        demandTrips =  tripsUtil.locationsToTrips(demandNodes, precomputedPaths, vehicle);
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrips, this);
    }

    

    private void driveToTargetLocation() {
        state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        currentTrips = demandTrips;
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrips, this);
    }

    private void driveToNearestStation() {
        state = OnDemandVehicleState.DRIVING_TO_STATION;
        
        Node currentNode = demandNodes.get(demandNodes.size() - 1);
        
        targetStation = onDemandVehicleStationsCentral.getNearestStation(currentNode);
        
        currentTrips 
                = tripsUtil.createTrips(currentNode.getId(), targetStation.getPositionInGraph().getId(), vehicle);
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrips, this);
    }

    private void waitInStation() {
        state = OnDemandVehicleState.WAITING;
        targetStation.parkVehicle(this);
    }
    
    
    
    
    public interface OnDemandVehicleFactory {
        public OnDemandVehicle create(String id, Node startPosition);
    }
    
}
