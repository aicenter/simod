/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.statistics.EdgesLoadByState;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Node;

import java.math.BigInteger;
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
				BigInteger edgeId = network.getEdge(currentNode, targetNode).getStaticId();
				countLoadForPosition(entityId, edgeId);
			}
			else{
				assertSame(onDemandVehicle.getState(), OnDemandVehicleState.WAITING);
			}
		}
	}
	
	
	
}
