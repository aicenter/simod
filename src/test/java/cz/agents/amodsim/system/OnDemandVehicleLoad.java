/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.statistics.EdgesLoadByState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Node;
import static org.junit.Assert.assertSame;

/**
 *
 * @author fido
 */
public class OnDemandVehicleLoad extends EdgesLoadByState{
    
    @Inject
    public OnDemandVehicleLoad(HighwayNetwork highwayNetwork, OnDemandVehicleStorage onDemandVehicleStorage) {
        super(highwayNetwork, onDemandVehicleStorage);
    }

    @Override
    public void compute() {
        for (OnDemandVehicle onDemandVehicle : entityStorage) {
            String entityId = onDemandVehicle.getId();
            Node currentNode = onDemandVehicle.getPosition();
            Node targetNode = onDemandVehicle.getTargetNode();
            if(targetNode != null && !targetNode.equals(currentNode)){
                int edgeId = network.getEdge(currentNode.id, targetNode.id).getUniqueId();
                countLoadForPosition(entityId, edgeId);
            }
            else{
                assertSame(onDemandVehicle.getState(), OnDemandVehicleState.WAITING);
            }
        }
    }
    
    
    
}
