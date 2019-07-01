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
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 *
 * @author fiedlda1
 */
public interface TravelTimeProvider {
	
	/**
	 * Get expected travel time in milliseconds,
	 * @param entity Entity to compute the time for. Warning, this is currently not implemented, calling this method 
	 * is identical to call getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB)
	 * @param positionA From position.
	 * @param positionB To position.
	 * @return Expected travel time in milliseconds.
	 */
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB);
	
	/**
	 * Get expected travel time in milliseconds,
	 * @param positionA From position.
	 * @param positionB To position.
	 * @return Expected travel time in milliseconds.
	 */
	public double getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB);
}
