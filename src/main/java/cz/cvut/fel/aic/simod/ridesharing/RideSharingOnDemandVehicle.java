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
package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.Wait;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.WaitActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.action.*;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.MoDVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;
import cz.cvut.fel.aic.simod.storage.MoDVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.visio.PlanLayerTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle {

	private static final Logger LOGGER = LoggerFactory.getLogger(RideSharingOnDemandVehicle.class.getName());

	private final VisioPositionUtil positionUtil;

	private final WaitActivityFactory waitActivityFactory;

	private final ZonedDateTime operationStart;

	private final ZonedDateTime operationEnd;

	private final TravelTimeProvider travelTimeProvider;


	private DriverPlan currentPlan;

	private PlanAction currentTask;

	private IdGenerator tripIdGenerator;


	public ZonedDateTime getOperationStart() {
		return operationStart;
	}

	public ZonedDateTime getOperationEnd() {
		return operationEnd;
	}

	public DriverPlan getCurrentPlan() {
		currentPlan.updateCurrentPosition(getPosition());
		return currentPlan;
	}


	@Inject
	public RideSharingOnDemandVehicle(
		MoDVehicleStorage vehicleStorage,
		TripsUtil tripsUtil,
		StationsDispatcher onDemandVehicleStationsCentral,
		PhysicalVehicleDriveFactory driveActivityFactory,
		VisioPositionUtil positionUtil,
		IdGenerator tripIdGenerator,
		EventProcessor eventProcessor,
		StandardTimeProvider timeProvider,
		IdGenerator rebalancingIdGenerator,
		SimodConfig config,
		IdGenerator idGenerator,
		AgentpolisConfig agentpolisConfig,
		WaitActivityFactory waitActivityFactory,
		TravelTimeProvider travelTimeProvider,
		String vehicleId,
		SimulationNode startPosition,
		MoDVehicle vehicle,
		ZonedDateTime operationStart,
		ZonedDateTime operationEnd,
		OnDemandVehicleState initialState
	) {
		super(
			vehicleStorage,
			tripsUtil,
			onDemandVehicleStationsCentral,
			driveActivityFactory,
			positionUtil,
			eventProcessor,
			timeProvider,
			rebalancingIdGenerator,
			config,
			idGenerator,
			agentpolisConfig,
			vehicleId,
			startPosition,
			vehicle
		);
		this.positionUtil = positionUtil;
		this.tripIdGenerator = tripIdGenerator;
		this.waitActivityFactory = waitActivityFactory;
		this.travelTimeProvider = travelTimeProvider;
		this.operationStart = operationStart;
		this.operationEnd = operationEnd;
		this.state = initialState;

//		empty plan
		LinkedList<PlanAction> plan = new LinkedList<>();
		plan.add(new PlanActionCurrentPosition(getPosition()));
		currentPlan = new DriverPlan(plan, 0, 0);

		if(config.vehicles.minPauseLength > 0){
			generatePauseActions();
		}
	}

	private void generatePauseActions() {

	}


	@Override
	public void handleEvent(Event event) {
		if(event.getType() instanceof OnDemandVehicleEvent && ((OnDemandVehicleEvent) event.getType()).equals(OnDemandVehicleEvent.START_OPERATING)) {
			startOperation();
		}
		else {
			LOGGER.error("Unexpected event: " + event.getType());
		}
	}


	public int getOnBoardCount() {
		return vehicle.getTransportedEntities().size();
	}

	public void replan(DriverPlan plan) {
		currentPlan = plan;

		// vehicle is outside the operation time
		if(state.equals(OnDemandVehicleState.NON_ACTIVE)) {
			var currentDatetime = timeProvider.getCurrentSimDateTime();

			// after the operation end time -> error
			if(currentDatetime.isAfter(operationEnd)) {
				LOGGER.error(
					"Vehicle {} is trying to start operation after the operation end time. Current time: {}, operation end time: {}",
					getId(),
					currentDatetime,
					operationEnd
				);
				return;
			}

			// before the operation start time -> wait till start operation event
			return;
		}


		// The vehicle now waits, we have to start moving.
		if (state == OnDemandVehicleState.WAITING && plan.getLength() > 1) {
			driveToNextTask();
		}
		// SPECIAL CASES - we don't have to do anything and let the current Drive action continue.

		else if (
			// The first action in the new plan is the same as the current action 
			(plan.getLength() > 1 && currentTask != null && currentTask.equals(plan.getNextTask()))
				// The new plan is empty and the vehicle is waiting or driving to station
				|| (plan.getLength() == 1 &&
				(state == OnDemandVehicleState.WAITING || state == OnDemandVehicleState.DRIVING_TO_STATION))) {

		}
		// We end the current Drive action and start the execution of the new plan.
		else {
			((PhysicalVehicleDrive) getCurrentTopLevelActivity()).end();
		}

	}

	@Override
	protected void driveToDemandStartLocation() {
		// safety check that prevents request from being picked up twice because of the delayed pickup event
		if (((PlanActionPickup) currentTask).request.isOnboard()) {
			currentPlan.taskCompleted();
			driveToNextTask();
		}
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
		if (getPosition().id == currentTask.getPosition().id) {
			pickupAndContinue();
		} else {
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
			DemandAgent demandAgent = ((PlanActionPickup) currentTask).getRequest().getDemandAgent();
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
		if (getPosition().id == currentTask.getPosition().id) {
			dropOffAndContinue();
		} else {
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void driveToNearestStation() {
		state = OnDemandVehicleState.DRIVING_TO_STATION;
		targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());

		if (getPosition().equals(targetStation.getPosition())) {
			finishDrivingToStation();
		} else {
			currentTrip = tripsUtil.createTrip(getPosition(), targetStation.getPosition(), vehicle);
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}


	@Override
	protected void onActivityFinish(Activity activity) {
		super.onActivityFinish(activity);
		if (activity instanceof Wait) {
			if(parkedIn == null) {
				pickupAndContinue();
			}
			else {
				driveToNextTask();
			}
		}
	}

	@Override
	public void finishedDriving(boolean wasStopped) {
		logTraveledDistance(wasStopped);

		if (wasStopped) {
			driveToNextTask();
		} else {
			switch (state) {
				case DRIVING_TO_START_LOCATION:
					pickupAndContinue();
					break;
				case DRIVING_TO_TARGET_LOCATION:
					dropOffAndContinue();
					break;
				case DRIVING_TO_STATION:
					finishDrivingToStation();
					break;
				case REBALANCING:
					finishRebalancing();
					break;
			}
		}
	}

	private void driveToNextTask() {
		// operation time check
		var currentDatetime = timeProvider.getCurrentSimDateTime();
		if (operationStart != null && currentDatetime.isBefore(operationStart)) {
			LOGGER.error(
				"Vehicle {} is trying to start operation before the operation start time. Current time: {}, operation start time: {}",
				getId(),
				currentDatetime,
				operationStart
			);
			return;
		}
		if (operationEnd != null && currentDatetime.isAfter(operationEnd)) {
			LOGGER.error(
				"Vehicle {} is trying to start operation after the operation end time. Current time: {}, operation end time: {}",
				getId(),
				currentDatetime,
				operationEnd
			);
			return;
		}

		if (currentPlan.getLength() == 1) {
			currentTask = null;
			if (state != OnDemandVehicleState.WAITING) {
				if (onDemandVehicleStationsCentral.stationsOn()) {
					driveToNearestStation();
				} else {
					park();
				}
			}
		} else {
			// check if we should not wait in station till the next task is available
			if(shouldWaitInStationForRequestMinTime()){
				return;
			}

			currentTask = currentPlan.getNextTask();

			// vehicle is waiting in the station
			if (parkedIn != null) {
				parkedIn.releaseVehicle(this);
				leavingStationEvent();
			}
			if (currentTask instanceof PlanActionPickup) {
				driveToDemandStartLocation();
			} else {
				driveToTargetLocation();
			}
		}
	}

	private boolean shouldWaitInStationForRequestMinTime() {
		// not in station -> no need to wait
		if(parkedIn == null) {
			return false;
		}

		var nextTask = currentPlan.getNextTask();
		long travelTimeToTask = travelTimeProvider.getTravelTime(
			this,
			getPosition(),
			nextTask.getPosition()
		);
		long minPickupTime = ((PlanActionPickup) nextTask).getMinTimeInSimulationTimeSeconds() * 1000L;
		if (travelTimeToTask + timeProvider.getCurrentSimTime() < minPickupTime) {
			waitActivityFactory.runActivity(
				this,
				minPickupTime - travelTimeToTask - timeProvider.getCurrentSimTime()
			);
			return true;
		}
		return false;
	}

	private void pickupAndContinue() {
		long minTime = ((PlanActionPickup) currentTask).getMinTimeInSimulationTimeSeconds() * 1000L;
		long currentTime = timeProvider.getCurrentSimTime();

		// we have to wait for the pickup time
		if (minTime > currentTime) {
			long waitTime = minTime - currentTime;
			waitActivityFactory.runActivity(this, waitTime);
			return;
		}

		try {
			DemandAgent demandAgent = ((PlanActionPickup) currentTask).getRequest().getDemandAgent();
			if (demandAgent.isDropped()) {
				long dropTime = demandAgent.getRequest().getMaxPickupTime();
				throw new Exception(
					String.format("Demand agent %s cannot be picked up, he is already dropped! Current simulation "
						+ "time: %s, dropp time: %s", demandAgent, currentTime, dropTime));
			}
			demandAgent.tripStarted(this);
			vehicle.pickUp(demandAgent);

			// statistics TODO demand tirp?
			//		demandTrip = tripsUtil.createTrip(currentTask.getDemandAgent().getPosition().id,
			//				currentTask.getLocation().id, vehicle);
			// demand trip length 0 - need to find out where the statistic is used, does it make sense with rebalancing?
			eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, new PickupEventContent(
				timeProvider.getCurrentSimTime(),
				demandAgent.getRequest().getId(),
				demandAgent.getSimpleId(),
				getId(),
				(int) Math.round(demandAgent.getMinDemandServiceDuration() / 1000)
			));
			currentPlan.taskCompleted();
			driveToNextTask();

		} catch (Exception ex) {
			LOGGER.error(null, ex);
		}
	}

	private void dropOffAndContinue() {
		DemandAgent demandAgent = ((PlanActionDropoff) currentTask).getRequest().getDemandAgent();
		demandAgent.tripEnded();
		vehicle.dropOff(demandAgent);

		// statistics
		eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null,
			new OnDemandVehicleEventContent(
				timeProvider.getCurrentSimTime(),
				demandAgent.getRequest().getId(),
				demandAgent.getSimpleId(),
				getId()
			)
		);
		currentPlan.taskCompleted();
		driveToNextTask();
	}

	@Override
	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, new OnDemandVehicleEventContent(
			timeProvider.getCurrentSimTime(),
			((PlanRequestAction) currentTask).getRequest().getId(),
			((PlanRequestAction) currentTask).getRequest().getId(),
			getId()
		));
	}

	@Override
	public VehicleTrip getCurrentTripPlan() {
		return currentTrip;
	}

	public boolean hasFreeCapacityFor(DemandAgent entity) {
		return vehicle.hasCapacityFor(entity);
	}

	public boolean hasFreeCapacityFor(PlanComputationRequest request) {
		return vehicle.hasCapacityFor(request);
	}

	public List<PlanLayerTrip> getPlanForRendering() {
		List<PlanLayerTrip> trips = new ArrayList<>(currentPlan.getLength());
		SimulationNode lastPosition = getPosition();
		for (PlanAction action : currentPlan) {
			if (action instanceof PlanRequestAction && lastPosition != action.getPosition()) {
				VehicleTrip<SimulationNode> newTrip
					= tripsUtil.createTrip(lastPosition, action.getPosition(), vehicle);
				trips.add(new PlanLayerTrip(
					tripIdGenerator.getId(),
					(PlanRequestAction) action,
					newTrip.getLocations()
				));
				lastPosition = action.getPosition();
			}
		}
		return trips;
	}

	private void logTraveledDistance(boolean wasStopped) {
		int length = wasStopped ? positionUtil.getTripLengthInMeters(currentTrip, getPosition())
			: positionUtil.getTripLengthInMeters(currentTrip);

		if (getOnBoardCount() > 0) {
			metersWithPassenger += length;
		} else {
			switch (state) {
				case DRIVING_TO_START_LOCATION:
					metersToStartLocation += length;
					break;
				case DRIVING_TO_STATION:
					metersToStation += length;
					break;
			}
		}

	}

	private void startOperation() {
		this.state = OnDemandVehicleState.WAITING;
		driveToNextTask();
	}

}
