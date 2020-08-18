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

import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 *
 * @author fiedlda1
 */
public abstract class TravelTimeProvider {
	
	private final TimeProvider timeProvider;

	protected long callCount;
	
	
	public TravelTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
                callCount = 0;
	}
        
        public long getCallCount() {
		return callCount;
	}
	
	
	
	/**
	 * Get expected travel time in milliseconds,
	 * @param entity Entity to compute the time for. Warning, this is currently not implemented, calling this method 
	 * is identical to call getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB)
	 * @param positionA From position.
	 * @param positionB To position.
	 * @return Expected travel time in milliseconds.
	 */
	public abstract long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB);
	
	/**
	 * Get expected travel time in milliseconds,
	 * @param positionA From position.
	 * @param positionB To position.
	 * @return Expected travel time in milliseconds.
	 */
	public long getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB) {
		return getTravelTime(null, positionA, positionB);
	}
	
	
	/**
	 * Traveltime for the moving entity from its current position to the target position. Note that you should always 
	 * use this method to compute the time from the current location, as it computes with the precise position in the 
	 * roadgraph, that is usually outside the network nodes.
	 * @param entity
	 * @param targetPosition
	 * @return 
	 */
	public long getTravelTime(MovingEntity entity, SimulationNode targetPosition){
		
		// entity is not moving
		if(entity.getDelayData() == null){
			return getTravelTime(entity, entity.getPosition(), targetPosition);
		}
		
		
		long timeToNextNode = entity.getDelayData().getRemainingTime(timeProvider.getCurrentSimTime());
		long timeFromNextNodeToTarget = getTravelTime(entity, entity.getTargetNode(), targetPosition);
		return timeToNextNode + timeFromNextNodeToTarget;
	}

	public void printCalls() {
    }

    public void close() {

	}
}
