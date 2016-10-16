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
import com.mycompany.testsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Node;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fido
 */
public class OnDemandVehicleStation extends AgentPolisEntity implements EventHandler{

    private final List<OnDemandVehicle> parkedVehicles;
    
    private final EventProcessor eventProcessor;
    
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
			@Assisted("lat") double lat, @Assisted("lon") double lon, @Assisted int initialVehicleCount) {
        super(id);
        this.eventProcessor = eventProcessor;
        positionInGraph = nearestElementUtils.getNearestElement(new GPSLocation(lat, lon, 0, 0), EGraphType.HIGHWAY);
//        gpsLocation = new GPSLocation(positionInGraph.getLatitude(), positionInGraph.getLongitude(), 0, 0);
        parkedVehicles = new ArrayList<>();
        for (int i = 0; i < initialVehicleCount; i++) {
			String onDemandVehicelId = String.format("%s-%d", id, i);
			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, positionInGraph);
            parkedVehicles.add(newVehicle);
			onDemandVehicleStorage.addEntity(newVehicle);
			agentPositionModel.setNewEntityPosition(newVehicle.getId(), positionInGraph.getId());
        }
        onDemandVehicleStationStorage.addEntity(this);
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
        OnDemandVehicle vehicle = parkedVehicles.remove(0);
        eventProcessor.addEvent(null, vehicle, null, demandData.locations);
        eventProcessor.addEvent(null, demandData.demandAgent, null, vehicle);
    }
    
    public interface OnDemandVehicleStationFactory {
        public OnDemandVehicleStation create(String id, @Assisted("lat") double lat, @Assisted("lon") double lon, 
                int initialVehicleCount);
    }
    
}
