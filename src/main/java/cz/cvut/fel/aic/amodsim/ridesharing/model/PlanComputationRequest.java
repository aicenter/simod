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
package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

/**
 *
 * @author LocalAdmin
 */
public interface PlanComputationRequest {

	@Override
	public boolean equals(Object obj);

	
	/**
	 * Returns max pickup time in seconds.
	 * @return max pickup time in seconds.
	 */
	public int getMaxPickupTime();
	
	/**
	 * Returns max dropoff time in seconds.
	 * @return max dropoff time in seconds.
	 */
	public int getMaxDropoffTime();
	
	/**
	 * Returns request origin time in seconds.
	 * @return Request origin time in seconds.
	 */
	public int getOriginTime();
	
	/**
	 * Returns min travel time in seconds.
	 * @return Min travel time in seconds.
	 */
	public int getMinTravelTime();
	
	public SimulationNode getFrom();
	
	public SimulationNode getTo();
	
	public boolean isOnboard();
	
	public PlanActionPickup getPickUpAction();
	
	public PlanActionDropoff getDropOffAction();
	
	public DemandAgent getDemandAgent();
	
	public int getId();
	
	public void setOnboard(boolean onboard);
}
