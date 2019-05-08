package cz.cvut.fel.aic.amodsim.statistics;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.load.AllEdgesLoad;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.entity.DriveAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.DriveAgentStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.HashMap;

public class DriveAgentEdgesLoadByState extends AllEdgesLoad<DriveAgent, DriveAgentStorage>{
    
    private final HashMap<OnDemandVehicleState,HashMap<Integer,Integer>> edgeLoadsPerState;    

    public HashMap<OnDemandVehicleState, HashMap<Integer, Integer>> getEdgeLoadsPerState() {
        return edgeLoadsPerState;
    }    
    
    @Inject
    public DriveAgentEdgesLoadByState(HighwayNetwork highwayNetwork, 
            DriveAgentStorage driveAgentStorage) {
        super(driveAgentStorage, highwayNetwork);
        edgeLoadsPerState = new HashMap<>();
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            edgeLoadsPerState.put(onDemandVehicleState, new HashMap<>());
        }
    }   
    
    @Override
    protected void countLoadForPosition(String entityId, int edgeId) {
        super.countLoadForPosition(entityId, edgeId); 
        
        //OnDemandVehicleState vehicleState = entityStorage.getEntityById(entityId).getState();
        
        //CollectionUtil.incrementMapValue(edgeLoadsPerState.get(vehicleState), edgeId, 1);
    }
}
