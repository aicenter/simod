/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.simulationon;

import com.mycompany.testsim.DemandSimulationEntityType;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;

/**
 *
 * @author fido
 */
public class OnDemandVehicle extends Vehicle{
    
    private static final double LENGTH = 4;
    
    private static final int CAPACITY = 5;
    
    public OnDemandVehicle(String vehicleId) {
        super(vehicleId, DemandSimulationEntityType.ON_DEMAND_VEHICLE, LENGTH, CAPACITY, EGraphType.HIGHWAY);
    }
    
}
