package cz.agents.amodsim.storage;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.amodsim.entity.OnDemandVehicleStation;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import java.util.HashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandvehicleStationStorage extends EntityStorage<OnDemandVehicleStation>{
    
    
    @Inject
    public OnDemandvehicleStationStorage() {
        super();
    }
    
}