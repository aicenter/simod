/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.statistics;

import com.google.inject.Inject;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.CollectionUtil;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.load.AllEdgesLoad;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;

import java.util.HashMap;

/**
 *
 * @author fido
 */
public class EdgesLoadByState extends AllEdgesLoad{
    
    private final HashMap<OnDemandVehicleState,HashMap<String,Integer>> edgeLoadsPerState;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;

    public HashMap<OnDemandVehicleState, HashMap<String, Integer>> getEdgeLoadsPerState() {
        return edgeLoadsPerState;
    }

    
    
    
    @Inject
    public EdgesLoadByState(HighwayNetwork highwayNetwork, 
            OnDemandVehicleStorage onDemandVehicleStorage) {
        super(onDemandVehicleStorage, highwayNetwork);
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        edgeLoadsPerState = new HashMap<>();
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            edgeLoadsPerState.put(onDemandVehicleState, new HashMap<>());
        }
    }

    
    
    
    
    @Override
    protected void countLoadForPosition(String entityId, String edgeId) {
        super.countLoadForPosition(entityId, edgeId); 
        
        OnDemandVehicleState vehicleState = onDemandVehicleStorage.getEntityById(entityId).getState();
        
        CollectionUtil.incrementMapValue(edgeLoadsPerState.get(vehicleState), edgeId, 1);
    }

}
