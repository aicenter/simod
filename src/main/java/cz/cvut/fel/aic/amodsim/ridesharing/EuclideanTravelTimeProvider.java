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
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class EuclideanTravelTimeProvider implements TravelTimeProvider{
	
	private final PositionUtil positionUtil;
	
	private final AmodsimConfig config;

	private final double travelSpeedEstimatePerSecond;
	
	private long callCount = 0;

	public long getCallCount() {
		return callCount;
	}
	
	
	
	
	@Inject
	public EuclideanTravelTimeProvider(PositionUtil positionUtil, AmodsimConfig config) {
		this.positionUtil = positionUtil;
		this.config = config;
		travelSpeedEstimatePerSecond = config.amodsim.ridesharing.maxSpeedEstimation / 3.6;
	}
	
	

	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		callCount++;
		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
		long traveltime = MoveUtil.computeDuration(travelSpeedEstimatePerSecond, distance);
		return traveltime;
	}

	@Override
	public double getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB) {
		return getTravelTime(null, positionA, positionB);
	}
	
}
