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
package cz.cvut.fel.aic.amodsim.statistics;

import com.google.inject.Inject;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
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
