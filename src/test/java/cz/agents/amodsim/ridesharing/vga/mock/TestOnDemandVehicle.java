/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
