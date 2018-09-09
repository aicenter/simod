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
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB);
    public double getTravelTime(SimulationNode positionA, SimulationNode positionB);
}
