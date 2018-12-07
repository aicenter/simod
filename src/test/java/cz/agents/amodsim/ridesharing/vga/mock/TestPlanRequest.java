/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;

/**
 *
 * @author F.I.D.O.
 */
public class TestPlanRequest implements PlanComputationRequest
{
	private boolean onboard;
	
	public final SimulationNode from;
	
	public final SimulationNode to;
	
	public final int minTravelTime;
	
	public final int maxPickUpTime;
	
	public final int maxDropOffTime;
	
	private final int originTime;
	
	private final int id;
	
	
	@Override
	public int getMaxPickupTime() {
		return maxPickUpTime;
	}

	@Override
	public int getMaxDropoffTime() {
		return maxDropOffTime;
	}

	@Override
	public int getMinTravelTime() {
		return minTravelTime;
	}

	@Override
	public SimulationNode getFrom() {
		return from;
	}

	@Override
	public SimulationNode getTo() {
		return to;
	}

	
	public TestPlanRequest(int id, AmodsimConfig amodsimConfig, SimulationNode origin, 
			SimulationNode destination, int originTime, boolean onboard){
		
		from = origin;
		to = destination;
		
        minTravelTime = (int) Math.round(
				MathUtils.getTravelTimeProvider().getExpectedTravelTime(origin, destination) / 1000.0);
        int maxProlongation = (int) Math.round(
				amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort * minTravelTime);
		
		maxPickUpTime = originTime + maxProlongation;
		maxDropOffTime = originTime + minTravelTime + maxProlongation;

		this.onboard = onboard;
		this.originTime = originTime;
		this.id = id;
    }

	@Override
	public int getOriginTime() {
		return originTime;
	}

	@Override
	public boolean isOnboard() {
		return onboard;
	}

	@Override
	public String toString() {
		return String.format("Demand %s", id);
	}

	
}
