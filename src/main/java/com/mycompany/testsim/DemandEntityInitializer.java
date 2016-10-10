/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.simulationon.OnDemandVehicleStation;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class DemandEntityInitializer {
    private final OnDemandvehicleStationStorage onDemandVehicleStationStorage;
    
    private final NearestElementUtils nearestElementUtils;

    
    
    @Inject
    public DemandEntityInitializer(OnDemandvehicleStationStorage onDemandVehicleStationStorage, 
            NearestElementUtils nearestElementUtils) {
        this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
        this.nearestElementUtils = nearestElementUtils;
    }

    void initialize(List<Trip<Long>> osmNodesList, List<OnDemandVehicleStation> onDemandVehicleStations, 
            List<Trip<OnDemandVehicleStation>> rebalancingTrips) {
        for (OnDemandVehicleStation onDemandVehicleStation : onDemandVehicleStations) {
            onDemandVehicleStation.setNearestNode(nearestElementUtils.getNearestElement(
                    onDemandVehicleStation.getGpsLocation(), EGraphType.HIGHWAY));
            onDemandVehicleStationStorage.addEntity(onDemandVehicleStation);
        }
    }
    

    
    
    
}
