/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod.visual.ridesharing.vga.mock;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.action.PlanActionDropoff;
import cz.cvut.fel.aic.simod.action.PlanActionPickup;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

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
		return pickUpAction.getMaxTimeInSimulationTimeSeconds();
	}

	@Override
	public int getMaxDropoffTime() {
		return dropOffAction.getMaxTimeInSimulationTimeSeconds();
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

	
	public TestPlanRequest(int id, SimodConfig SimodConfig, SimulationNode origin, 
			SimulationNode destination, int originTime, boolean onboard, TravelTimeProvider travelTimeProvider){
		
		minTravelTime = (int) Math.round(
				travelTimeProvider.getExpectedTravelTime(origin, destination) / 1000.0);
		int maxProlongation = SimodConfig.ridesharing.maxProlongationInSeconds;
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;

		this.onboard = onboard;
		this.originTime = originTime;
		this.id = id;
		
		pickUpAction = new PlanActionPickup(this, origin, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(this, destination, maxDropOffTime);
	}

	@Override
	public int getMinSimulationTimeSeconds() {
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
