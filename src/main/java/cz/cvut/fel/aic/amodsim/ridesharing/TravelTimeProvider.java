/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

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
