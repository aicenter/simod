/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.simulationon;

import com.mycompany.testsim.DemandSimulationEntityType;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.basestructures.GPSLocation;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fido
 */
public class OnDemandVehicleStation extends AgentPolisEntity{
    
    private final GPSLocation gpsLocation;
    
    private final List<OnDemandVehicle> parkedVehicles;

    
    
    
    
    public GPSLocation getGpsLocation() {
        return gpsLocation;
    }
    
    
    
    
    
    public OnDemandVehicleStation(String id, double lat, double lon, int latProjected, int lonProjected, int initialVehicleCount) {
        super(id);
        gpsLocation = new GPSLocation(lat, lon, latProjected, lonProjected);
        parkedVehicles = new ArrayList<>();
        for (int i = 0; i < initialVehicleCount; i++) {
            parkedVehicles.add(new OnDemandVehicle(Integer.toString(i)));
        }
    }
    
    
    

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.ON_DEMAND_VEHICLE_STATION;
    }

    @Override
    public DescriptionImpl getDescription() {
        return null;
    }
    
}
