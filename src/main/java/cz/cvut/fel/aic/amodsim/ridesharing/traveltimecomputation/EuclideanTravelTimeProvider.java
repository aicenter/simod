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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class EuclideanTravelTimeProvider extends TravelTimeProvider{
	
	private final PositionUtil positionUtil;
	
	private final AmodsimConfig config;

	private final int travelSpeedEstimateCmPerSecond;
	
	
	
	
	
	@Inject
	public EuclideanTravelTimeProvider(TimeProvider timeProvider, PositionUtil positionUtil, AmodsimConfig config) {
		super(timeProvider);
		this.positionUtil = positionUtil;
		this.config = config;
		travelSpeedEstimateCmPerSecond = config.vehicleSpeedInMeters * 100;
	}
	
	

	@Override
	public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		callCount++;
		int distance = (int) Math.round(
				positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB)) * 100);
		long traveltime = MoveUtil.computeDuration(travelSpeedEstimateCmPerSecond, distance);
		return traveltime;
	}	
}
