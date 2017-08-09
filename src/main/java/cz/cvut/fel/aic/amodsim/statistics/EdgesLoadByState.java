/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

import com.google.inject.Inject;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.load.AllEdgesLoad;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;

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
