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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.DelayData;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;

/**
 *
 * @author david
 */
public class TestOnDemandVehicle implements MovingEntity{
	
	private final AmodsimConfig config;
	
	private final SimulationNode targetNode;
	
	private final DelayData delayData;
	
	private final SimulationNode position;

	public TestOnDemandVehicle(AmodsimConfig config, SimulationNode targetNode, DelayData delayData, SimulationNode position) {
		this.config = config;
		this.targetNode = targetNode;
		this.delayData = delayData;
		this.position = position;
	}
	
	

	@Override
	public double getVelocity() {
		return (double) config.vehicleSpeedInMeters;
	}

	@Override
	public SimulationNode getTargetNode() {
		return targetNode;
	}

	@Override
	public DelayData getDelayData() {
		return delayData;
	}

	@Override
	public SimulationNode getPosition() {
		return position;
	}
	
}
