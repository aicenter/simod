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
package cz.cvut.fel.aic.simod;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;
import cz.cvut.fel.aic.simod.action.PlanActionDropoff;
import cz.cvut.fel.aic.simod.action.PlanActionPickup;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Random;


public class DefaultPlanComputationRequest implements PlanComputationRequest {

	private final TimeProvider timeProvider;

	/**
	 * Request index in the trips.csv file.
	 */
	public final int id;

	public final ZonedDateTime announcementTime;

	/**
	 * Request minimum pickup time in seconds.
	 */
	private final ZonedDateTime minTime;

	private final SlotType requiredSlotType;

	private DemandAgent demandAgent;

	private final PlanActionPickup pickUpAction;

	private final PlanActionDropoff dropOffAction;

	/**
	 * Min travel time in seconds
	 */
	private final int minTravelTime;

	private final int requiredVehicleId;


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

	@Override
	public void setDemandAgent(DemandAgent demandAgent) {
		this.demandAgent = demandAgent;
	}

	public SlotType getRequiredSlotType() {
		return requiredSlotType;
	}

	@Inject
	private DefaultPlanComputationRequest(
		TravelTimeProvider travelTimeProvider,
		TimeProvider timeProvider,
		@Assisted("id") int id,
		SimodConfig config,
		@Assisted("origin") SimulationNode origin,
		@Assisted("destination") SimulationNode destination,
		@Assisted("announcementTime") ZonedDateTime announcementTime,
		@Assisted("desiredPickupTime") ZonedDateTime desiredPickupTime,
		@Assisted SlotType requiredSlotType,
		@Assisted @Nullable DemandAgent demandAgent,
		@Assisted("requiredVehicleId") int requiredVehicleId
	) {
		this.timeProvider = timeProvider;
		this.id = id;
		this.announcementTime = announcementTime;
		this.requiredSlotType = requiredSlotType;
		this.requiredVehicleId = requiredVehicleId;

		hash = 0;

//		originTime = (int) Math.round(demandAgent.getDemandTime() / 1000.0);
		if(config.enableNegativeDelay){
			ZonedDateTime minTimeIncludingNegativeDelay = desiredPickupTime.minusSeconds(config.maxPickupDelay);
			if(timeProvider.getCurrentSimDateTime().isBefore(minTimeIncludingNegativeDelay)){
				minTime = minTimeIncludingNegativeDelay;
			}
			else{
				minTime = timeProvider.getCurrentSimDateTime();
			}
		}
		else{
			minTime = desiredPickupTime;
		}

		minTravelTime = (int) Math.round(
			travelTimeProvider.getExpectedTravelTime(origin, destination) / 1000.0);

		int maxProlongation;
		if (config.maxTravelTimeDelay.mode.equals("absolute")) {
			maxProlongation = config.maxTravelTimeDelay.seconds;
		} else {
			maxProlongation = (int) Math.round(
				config.maxTravelTimeDelay.relative * minTravelTime);
		}

		int maxPickUpDelay;
		if(config.maxPickupDelay >= 0){
			maxPickUpDelay = config.maxPickupDelay;
		}
		else{
			maxPickUpDelay = maxProlongation;
		}

		ZonedDateTime maxPickUpTime = desiredPickupTime.plusSeconds(maxPickUpDelay);
		ZonedDateTime maxDropOffTime = maxPickUpTime.plusSeconds(minTravelTime + maxProlongation);

		this.demandAgent = demandAgent;
		onboard = false;

		pickUpAction = new PlanActionPickup(timeProvider, this, origin, minTime, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(timeProvider, this, destination, maxDropOffTime);
	}



	@Override
	public int getMinSimulationTimeSeconds() {
		return (int) (timeProvider.getSimTimeFromDateTime(minTime) / 1000);
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
		if (hash == 0) {
			int p = 1_200_007;
			Random rand = new Random();
			int a = rand.nextInt(p) + 1;
			int b = rand.nextInt(p);
			hash = (int) (((long) a * getId() + b) % p) % 1_200_000;
		}
		return hash;
	}


	@Override
	public String toString() {
		return String.format("%s - from: %s to: %s", getId(), getFrom(), getTo());
	}

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

	@Override
	public int getRequiredVehicleId() {
		return requiredVehicleId;
	}


	public interface DefaultPlanComputationRequestFactory {
		public DefaultPlanComputationRequest create(
			@Assisted("id") int id,
			@Assisted("origin") SimulationNode origin,
			@Assisted("destination") SimulationNode destination,
			@Assisted("announcementTime") ZonedDateTime announcementTime,
			@Assisted("desiredPickupTime") ZonedDateTime minPickupTime,
			SlotType requiredSlotType,
			DemandAgent demandAgent,
			@Assisted("requiredVehicleId") int requiredVehicleId
		);
	}

}
