/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;

/**
 *
 * @author F.I.D.O.
 */
public class TestPlanRequest implements PlanComputationRequest
{
	private boolean onboard;
	
	public final int minTravelTime;
	
	private final int originTime;
	
	private final int id;
	
	private final PlanActionPickup pickUpAction;
	
	private final PlanActionDropoff dropOffAction;
	
	
	@Override
	public int getMaxPickupTime() {
		return pickUpAction.getMaxTime();
	}

	@Override
	public int getMaxDropoffTime() {
		return dropOffAction.getMaxTime();
	}

	@Override
	public int getMinTravelTime() {
		return minTravelTime;
	}

	@Override
	public SimulationNode getFrom() {
		return pickUpAction.getPosition();
	}

	@Override
	public SimulationNode getTo() {
		return dropOffAction.getPosition();
	}

	
	public TestPlanRequest(int id, AmodsimConfig amodsimConfig, SimulationNode origin, 
			SimulationNode destination, int originTime, boolean onboard){
		
		minTravelTime = (int) Math.round(
				MathUtils.getTravelTimeProvider().getExpectedTravelTime(origin, destination) / 1000.0);
		int maxProlongation = amodsimConfig.ridesharing.maxProlongationInSeconds;
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;

		this.onboard = onboard;
		this.originTime = originTime;
		this.id = id;
		
		pickUpAction = new PlanActionPickup(this, origin, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(this, destination, maxDropOffTime);
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

	@Override
	public PlanActionPickup getPickUpAction() {
		return pickUpAction;
	}

	@Override
	public PlanActionDropoff getDropOffAction() {
		return dropOffAction;
	}

	@Override
	public DemandAgent getDemandAgent() {
		return null;
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setOnboard(boolean onboard) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	
}
