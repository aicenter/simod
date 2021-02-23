/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.system;

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
