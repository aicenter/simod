/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.simod.config.SimodConfig;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class EuclideanTravelTimeProvider extends TravelTimeProvider{
	
	private final PositionUtil positionUtil;
	
	private final SimodConfig config;

	private final int travelSpeedEstimateCmPerSecond;
	
	
	
	
	
	@Inject
	public EuclideanTravelTimeProvider(TimeProvider timeProvider, PositionUtil positionUtil, SimodConfig config) {
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
