/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class EuclideanTravelTimeProvider implements TravelTimeProvider{
	
	private final PositionUtil positionUtil;

	
	@Inject
	public EuclideanTravelTimeProvider(PositionUtil positionUtil) {
		this.positionUtil = positionUtil;
	}
	
	

	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
		long traveltime = MoveUtil.computeDuration(entity.getVelocity(), distance);
		return traveltime;
	}
	
}
