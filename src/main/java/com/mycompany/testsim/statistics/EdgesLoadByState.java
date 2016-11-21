/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.statistics;

import com.google.inject.Inject;
import com.mycompany.testsim.entity.OnDemandVehicleState;
import com.mycompany.testsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.CollectionUtil;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.AllEdgesLoad;
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
    public EdgesLoadByState(AgentPositionModel entityPositionModel, HighwayNetwork highwayNetwork, 
            OnDemandVehicleStorage onDemandVehicleStorage) {
        super(entityPositionModel, highwayNetwork);
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        edgeLoadsPerState = new HashMap<>();
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            edgeLoadsPerState.put(onDemandVehicleState, new HashMap<>());
        }
    }

    
    
    
    
    @Override
    protected void countLoadForPosition(String entityId, int currentNodeId, int targetNodeId, String edgeId) {
        super.countLoadForPosition(entityId, currentNodeId, targetNodeId, edgeId); 
        
        OnDemandVehicleState vehicleState = onDemandVehicleStorage.getEntityById(entityId).getState();
        
        CollectionUtil.incrementMapValue(edgeLoadsPerState.get(vehicleState), edgeId, 1);
    }

}
