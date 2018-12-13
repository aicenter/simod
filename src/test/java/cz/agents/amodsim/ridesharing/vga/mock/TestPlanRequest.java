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
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanPickup;

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
	
	private final VGAVehiclePlanPickup pickUpAction;
	
	private final VGAVehiclePlanDropoff dropOffAction;
	
	
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
        int maxProlongation = (int) Math.round(
				amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort * minTravelTime);
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;

		this.onboard = onboard;
		this.originTime = originTime;
		this.id = id;
		
		pickUpAction = new VGAVehiclePlanPickup(this, origin, maxPickUpTime);
		dropOffAction = new VGAVehiclePlanDropoff(this, destination, maxDropOffTime);
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
	public VGAVehiclePlanPickup getPickUpAction() {
		return pickUpAction;
	}

	@Override
	public VGAVehiclePlanDropoff getDropOffAction() {
		return dropOffAction;
	}

	
}
