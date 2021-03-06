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
package cz.cvut.fel.aic.simod.ridesharing.model;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import java.util.Random;


public class DefaultPlanComputationRequest implements PlanComputationRequest{
	
	public final int id;
	
	/**
	 * Request origin time in seconds.
	 */
	private final int originTime;

	private final DemandAgent demandAgent;
	
	private final PlanActionPickup pickUpAction;
	
	private final PlanActionDropoff dropOffAction;
	
	/**
	 * Min travel time in seconds
	 */
	private final int minTravelTime;
	

	private boolean onboard;
	
	private int hash;

	
	
	@Override
	public int getId() {
		return id;
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
	public boolean isOnboard() {
		return onboard;
	}

	public void setOnboard(boolean onboard) {
		this.onboard = onboard;
	}
	
	@Override
	public DemandAgent getDemandAgent() { 
		return demandAgent; 
	}
	


	@Inject
	private DefaultPlanComputationRequest(TravelTimeProvider travelTimeProvider, @Assisted int id, 
			SimodConfig SimodConfig, @Assisted("origin") SimulationNode origin, 
			@Assisted("destination") SimulationNode destination, @Assisted DemandAgent demandAgent){
		this.id = id;
		
		hash = 0;
		
		originTime = (int) Math.round(demandAgent.getDemandTime() / 1000.0);
		minTravelTime = (int) Math.round(
				travelTimeProvider.getExpectedTravelTime(origin, destination) / 1000.0);
		
		int maxProlongation;
		if(SimodConfig.ridesharing.discomfortConstraint.equals("absolute")){
			maxProlongation = SimodConfig.ridesharing.maxProlongationInSeconds;
		}
		else{
			maxProlongation = (int) Math.round(
				SimodConfig.ridesharing.maximumRelativeDiscomfort * minTravelTime);
		}		
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;
		
		this.demandAgent = demandAgent;
		onboard = false;
		
		pickUpAction = new PlanActionPickup(this, origin, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(this, destination, maxDropOffTime);
	}

	@Override
	public int getOriginTime() { 
		return originTime; 
	}

	

//	@Override
//	public boolean equals(Object obj) {
//		if(!(obj instanceof DefaultPlanComputationRequest)) return false;
//		return demandAgent.toString().equals(((DefaultPlanComputationRequest) obj).demandAgent.toString());
//	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

//	@Override
//	public int hashCode() {
//		return demandAgent.getSimpleId();
//	}
	@Override
	public int hashCode() {
		if(hash == 0){
			int p = 1_200_007;
			Random rand = new Random();
			int a = rand.nextInt(p) + 1;
			int b = rand.nextInt(p);
			hash = (int) (((long) a * demandAgent.getSimpleId() + b) % p) % 1_200_000 ;
		}
		return hash;
	}
	

	@Override
	public String toString() {
		return String.format("%s - from: %s to: %s", demandAgent, getFrom(), getTo());
	}

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


	
	
	
	public interface DefaultPlanComputationRequestFactory {
		public DefaultPlanComputationRequest create(int id, @Assisted("origin") SimulationNode origin, 
				@Assisted("destination") SimulationNode destination, DemandAgent demandAgent);
	}

}
