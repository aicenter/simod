/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.storage.OnDemandvehicleStationStorage;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class EntityInitializer {
    private final OnDemandvehicleStationStorage onDemandVehicleStationStorage;
    
    private final NearestElementUtils nearestElementUtils;

    
    
    @Inject
    public EntityInitializer(OnDemandvehicleStationStorage onDemandVehicleStationStorage, 
            NearestElementUtils nearestElementUtils) {
        this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
        this.nearestElementUtils = nearestElementUtils;
    }

    public void initialize(List<OnDemandVehicleStation> onDemandVehicleStations) {
//        for (OnDemandVehicleStation onDemandVehicleStation : onDemandVehicleStations) {
//            onDemandVehicleStation.setNearestNode(nearestElementUtils.getNearestElement(
//                    onDemandVehicleStation.getGpsLocation(), EGraphType.HIGHWAY));
//            onDemandVehicleStationStorage.addEntity(onDemandVehicleStation);
//        }
    }
    

    
    
    
}
