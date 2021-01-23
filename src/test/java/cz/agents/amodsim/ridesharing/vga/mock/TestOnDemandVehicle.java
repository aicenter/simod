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
package cz.agents.amodsim.ridesharing.vga.mock;

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
	public int getVelocity() {
		return config.vehicleSpeedInMeters;
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
