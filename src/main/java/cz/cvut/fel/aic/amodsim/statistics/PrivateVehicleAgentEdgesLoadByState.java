package cz.cvut.fel.aic.amodsim.statistics;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.load.AllEdgesLoad;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.entity.PrivateVehicleAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.storage.PrivateVehicleAgentStorage;
import java.util.HashMap;

public class PrivateVehicleAgentEdgesLoadByState extends AllEdgesLoad<PrivateVehicleAgent, PrivateVehicleAgentStorage>{
    
    private final HashMap<OnDemandVehicleState,HashMap<Integer,Integer>> edgeLoadsPerState;    

    public HashMap<OnDemandVehicleState, HashMap<Integer, Integer>> getEdgeLoadsPerState() {
        return edgeLoadsPerState;
    }    
    
    @Inject
    public PrivateVehicleAgentEdgesLoadByState(HighwayNetwork highwayNetwork, 
            PrivateVehicleAgentStorage driveAgentStorage) {
        super(driveAgentStorage, highwayNetwork);
        edgeLoadsPerState = new HashMap<>();
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            edgeLoadsPerState.put(onDemandVehicleState, new HashMap<>());
        }
    }   
    
    @Override
    protected void countLoadForPosition(String entityId, int edgeId) {
        super.countLoadForPosition(entityId, edgeId); 
    }
}
