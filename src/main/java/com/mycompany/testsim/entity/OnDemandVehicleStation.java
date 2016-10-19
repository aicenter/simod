/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.mycompany.testsim.DemandData;
import com.mycompany.testsim.DemandSimulationEntityType;
import com.mycompany.testsim.storage.OnDemandvehicleStationStorage;
import com.mycompany.testsim.event.OnDemandVehicleStationEvent;
import com.mycompany.testsim.entity.OnDemandVehicle.OnDemandVehicleFactory;
import com.mycompany.testsim.io.TimeTrip;
import com.mycompany.testsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.Node;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
public class OnDemandVehicleStation extends AgentPolisEntity implements EventHandler{

    private final LinkedList<OnDemandVehicle> parkedVehicles;
    
    private final EventProcessor eventProcessor;
    
    private final VehiclePositionModel vehiclePositionModel;
    
//    private final GPSLocation gpsLocation;
    
    private final Node positionInGraph;

    
    
    
    
//    public GPSLocation getGpsLocation() {
//        return gpsLocation;
//    }

    public Node getPositionInGraph() {
        return positionInGraph;
    }
    
    
    
    
    
    
    @Inject
    public OnDemandVehicleStation(EventProcessor eventProcessor, OnDemandVehicleFactory onDemandVehicleFactory, 
            NearestElementUtils nearestElementUtils, OnDemandvehicleStationStorage onDemandVehicleStationStorage,
			OnDemandVehicleStorage onDemandVehicleStorage, @Assisted String id, AgentPositionModel agentPositionModel,
			@Assisted Node node, @Assisted int initialVehicleCount,
            VehiclePositionModel vehiclePositionModel) {
        super(id);
        this.eventProcessor = eventProcessor;
        positionInGraph = node;
        parkedVehicles = new LinkedList<>();
        for (int i = 0; i < initialVehicleCount; i++) {
			String onDemandVehicelId = String.format("%s-%d", id, i);
			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, positionInGraph);
            parkedVehicles.add(newVehicle);
			onDemandVehicleStorage.addEntity(newVehicle);
			agentPositionModel.setNewEntityPosition(newVehicle.getId(), positionInGraph.getId());
        }
        onDemandVehicleStationStorage.addEntity(this);
        this.vehiclePositionModel = vehiclePositionModel;
    }
    
    
    

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.ON_DEMAND_VEHICLE_STATION;
    }

    @Override
    public DescriptionImpl getDescription() {
        return null;
    }
    
//    public void setNearestNode(final Node node){
//        positionInGraph = node;
//        
//    }

    @Override
    public EventProcessor getEventProcessor() {
        return null;
    }

    @Override
    public void handleEvent(Event event) {
        OnDemandVehicleStationEvent eventType = (OnDemandVehicleStationEvent) event.getType();
        switch(eventType){
            case TRIP:
                handleTripRequest((DemandData) event.getContent());
                break;
        }
    }
    
    public void parkVehicle(OnDemandVehicle onDemandVehicle){
        parkedVehicles.add(onDemandVehicle);
    }

    private void handleTripRequest(DemandData demandData) {
        
        // hack for demands that starts and ends in the same position
        if(demandData.locations.get(0).equals(demandData.locations.get(demandData.locations.size() - 1))){
            return;
        }
        
        OnDemandVehicle vehicle = getVehicle();
        if(vehicle != null){
            eventProcessor.addEvent(null, vehicle, null, demandData.locations);
            eventProcessor.addEvent(null, demandData.demandAgent, null, vehicle);
        }
    }

    public void rebalance(TimeTrip<OnDemandVehicleStation> rebalancingTrip) {
        OnDemandVehicle vehicle = getVehicle();
        if(vehicle != null){
            if(rebalancingTrip.getLocations().getLast() == this){
                System.out.println("com.mycompany.testsim.entity.OnDemandVehicleStation.rebalance()");
            }
            if(rebalancingTrip.getLocations().getLast().getPositionInGraph().getId()
                    == vehiclePositionModel.getEntityPositionByNodeId(vehicle.getVehicleId())){
                System.out.println("com.mycompany.testsim.entity.OnDemandVehicleStation.rebalance()");
            }
//            if(this.getPositionInGraph() = vehicle)
            vehicle.driveToStation(rebalancingTrip.getLocations().getLast());
        }
    }
    
    public boolean isEmpty(){
        return parkedVehicles.isEmpty();
    }

    private OnDemandVehicle getVehicle() {
        return parkedVehicles.poll();
    }
    
    
    
    
    
    public interface OnDemandVehicleStationFactory {
        public OnDemandVehicleStation create(String id, Node node, int initialVehicleCount);
    }
    
}
