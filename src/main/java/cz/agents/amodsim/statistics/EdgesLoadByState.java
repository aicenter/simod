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
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;

import java.util.HashMap;

/**
 *
 * @author fido
 */
public class EdgesLoadByState extends AllEdgesLoad<OnDemandVehicle, OnDemandVehicleStorage>{
    
    private final HashMap<OnDemandVehicleState,HashMap<Integer,Integer>> edgeLoadsPerState;
    
    
    

    public HashMap<OnDemandVehicleState, HashMap<Integer, Integer>> getEdgeLoadsPerState() {
        return edgeLoadsPerState;
    }

    
    
    
    @Inject
    public EdgesLoadByState(HighwayNetwork highwayNetwork, 
            OnDemandVehicleStorage onDemandVehicleStorage) {
        super(onDemandVehicleStorage, highwayNetwork);
        edgeLoadsPerState = new HashMap<>();
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            edgeLoadsPerState.put(onDemandVehicleState, new HashMap<>());
        }
    }

    
    
    
    
    @Override
    protected void countLoadForPosition(String entityId, int edgeId) {
        super.countLoadForPosition(entityId, edgeId); 
        
        OnDemandVehicleState vehicleState = entityStorage.getEntityById(entityId).getState();
        
        CollectionUtil.incrementMapValue(edgeLoadsPerState.get(vehicleState), edgeId, 1);
    }

}
