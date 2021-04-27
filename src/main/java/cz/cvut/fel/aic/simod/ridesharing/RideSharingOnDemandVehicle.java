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
package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.ParcelAgent;
import cz.cvut.fel.aic.simod.entity.SimulationAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionCurrentPosition;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.simod.visio.PlanLayerTrip;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle{
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RideSharingOnDemandVehicle.class);

	private final VisioPositionUtil positionUtil;
	
	private DriverPlan currentPlan;
	
	private PlanAction currentTask;

	private IdGenerator tripIdGenerator;

	public DriverPlan getCurrentPlan() {
		currentPlan.updateCurrentPosition(getPosition());
		return currentPlan;
	}
	
	
	
	
	@Inject
	public RideSharingOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, 
			TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
			PhysicalVehicleDriveFactory driveActivityFactory, VisioPositionUtil positionUtil,IdGenerator tripIdGenerator,
			EventProcessor eventProcessor,StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, SimodConfig config, 
			@Assisted String vehicleId, @Assisted SimulationNode startPosition) {
		super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral,
				driveActivityFactory, positionUtil, eventProcessor, timeProvider, rebalancingIdGenerator, config, 
				vehicleId, startPosition);
		this.positionUtil = positionUtil;
		this.tripIdGenerator = tripIdGenerator;
		
//		empty plan
		LinkedList<PlanAction> plan = new LinkedList<>();
		plan.add(new PlanActionCurrentPosition(getPosition()));
		currentPlan = new DriverPlan(plan, 0, 0);
	}

	@Override
	public void handleEvent(Event event) {
		
	}
	
	public int getOnBoardCount(){
		return vehicle.getTransportedEntities().size();
	}

	// need to distinguish between parcels and passengers
	public int getFreeCapacity(){
		return vehicle.getCapacity() - vehicle.getTransportedEntities().size();
	}

	public int getFreeTrunkCapacity() {
		return vehicle.getTrunkCapacity() - vehicle.getTransportedTrunkEntities().size();
	}
	
	public void replan(DriverPlan plan){
		currentPlan = plan;
		// The vehicle now waits, we have to start moving.
		if(state == OnDemandVehicleState.WAITING && plan.getLength() > 1){
			driveToNextTask();
		}
		// SPECIAL CASES - we don't have to do anything and let the current Drive action continue.
		
		else if(
			// The first action in the new plan is the same as the current action 
			(plan.getLength() > 1 && currentTask != null && currentTask.equals(plan.getNextTask()))
			// The new plan is empty and the vehicle is waiting or driving to station
			|| (plan.getLength() == 1 && (state == OnDemandVehicleState.WAITING || state == OnDemandVehicleState.DRIVING_TO_STATION))){

		}
		// We end the current Drive action and start the execution of the new plan.
		else{
			((PhysicalVehicleDrive) getCurrentTopLevelActivity()).end();
		}
		
	}

	@Override
	protected void driveToDemandStartLocation() {
		// safety check that prevents request from being picked up twice because of the delayed pickup event
		if(((PlanActionPickup) currentTask).request.isOnboard()){
			currentPlan.taskCompleted();
			driveToNextTask();
		}
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
		if(getPosition().id == currentTask.getPosition().id){
			pickupAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
			// ???
//			DemandAgent demandAgent = (DemandAgent) ((PlanActionPickup) currentTask).getRequest().getSimulationAgent();
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
		if(getPosition().id == currentTask.getPosition().id){
			LOGGER.info("driveToTargetLocation: Vehicle id: " + getVehicleId() + ", current position: " + getPosition().id + ", task position: " + currentTask.getPosition().id);
			dropOffAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void driveToNearestStation() {
		state = OnDemandVehicleState.DRIVING_TO_STATION;
		targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());
		
		if(getPosition().equals(targetStation.getPosition())){
			finishDrivingToStation();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition(), targetStation.getPosition(), vehicle);
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	public void finishedDriving(boolean wasStopped) {
		logTraveledDistance(wasStopped);
		
		if(wasStopped){
			driveToNextTask();
		}
		else{
			switch(state){
				case DRIVING_TO_START_LOCATION:
					pickupAndContinue();
					break;
				case DRIVING_TO_TARGET_LOCATION:
					LOGGER.info("finishedDriving (wasStopped=" + wasStopped + "): Vehicle id: " + getVehicleId() + ", current position: " + getPosition().id + ", task position: " + currentTask.getPosition().id);
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
		if(currentPlan.getLength() == 1){
			currentTask = null;
			if(state != OnDemandVehicleState.WAITING){
				if(onDemandVehicleStationsCentral.stationsOn()){
					driveToNearestStation();
				}
				else{
					park();
				}
			}
		}
		else{
			currentTask = currentPlan.getNextTask();
			if(parkedIn != null){
				parkedIn.releaseVehicle(this);
				leavingStationEvent();
			}
			if(currentTask instanceof PlanActionPickup){
				driveToDemandStartLocation();
			}
			else{
				driveToTargetLocation();
			}
		}
	}

	private void pickupAndContinue() {
		try {
			SimulationAgent simulationAgent = ((PlanActionPickup) currentTask).getRequest().getSimulationAgent();
			if (simulationAgent.isDropped()) {
				long currentTime = timeProvider.getCurrentSimTime();
				long droppTime = simulationAgent.getDemandTime() + config.ridesharing.maxProlongationInSeconds * 1000;
				throw new Exception(
					String.format("Demand agent %s cannot be picked up, he is already dropped! Current simulation "
							+ "time: %s, dropp time: %s", simulationAgent, currentTime, droppTime));
			}
			simulationAgent.tripStarted(this);
			// TODO maybe fix with Strategy pattern
			EntityType entityType = simulationAgent.getType();
			OnDemandVehicleEvent eventType;
			if (entityType == DemandSimulationEntityType.DEMAND) {
				vehicle.pickUp((DemandAgent) simulationAgent);
				eventType = OnDemandVehicleEvent.DEMAND_PICKUP;
			} else {
				LOGGER.info("Vehicle " + getVehicleId() + " picking up parcel " + simulationAgent.getSimpleId() + " at position: " + getPosition().id);
				vehicle.pickUp((ParcelAgent) simulationAgent);
				eventType = OnDemandVehicleEvent.PARCEL_PICKUP;
			}
			// statistics TODO demand tirp?
	//		demandTrip = tripsUtil.createTrip(currentTask.getDemandAgent().getPosition().id,
	//				currentTask.getLocation().id, vehicle);
			// demand trip length 0 - need to find out where the statistic is used, does it make sense with rebalancing?
			eventProcessor.addEvent(eventType, null, null,
					new PickupEventContent(timeProvider.getCurrentSimTime(),
							simulationAgent.getSimpleId(), getId(), 0));
			currentPlan.taskCompleted();
			driveToNextTask();

		} catch (Exception ex) {
			Logger.getLogger(RideSharingOnDemandVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void dropOffAndContinue() {
		SimulationAgent simulationAgent = ((PlanActionDropoff) currentTask).getRequest().getSimulationAgent();
		LOGGER.info(simulationAgent.getType() + " " + simulationAgent.getSimpleId() + ", state: " + simulationAgent.getState() + ", agent vehicle id: " +
				simulationAgent.getOnDemandVehicle().getVehicleId() + ", vehicle id: " + getVehicleId() + ", from: " +
				((PlanActionDropoff) currentTask).getRequest().getFrom().id + ", to: " +
				((PlanActionDropoff) currentTask).getRequest().getTo().id + " current position: " + getPosition().id +
				" remaining orders: " + getOnBoardCount());
		simulationAgent.tripEnded();
		// TODO maybe fix with Strategy pattern
		EntityType entityType = simulationAgent.getType();
		OnDemandVehicleEvent eventType;
		if (entityType == DemandSimulationEntityType.DEMAND) {
			vehicle.dropOff((DemandAgent) simulationAgent);
			eventType = OnDemandVehicleEvent.DEMAND_DROP_OFF;
		} else {
			vehicle.dropOff((ParcelAgent) simulationAgent);
			eventType = OnDemandVehicleEvent.PARCEL_DROP_OFF;
		}
		
		// statistics
		eventProcessor.addEvent(eventType, null, null,
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
						simulationAgent.getSimpleId(), getId()));
		currentPlan.taskCompleted();
		driveToNextTask();
	}

	@Override
	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, 
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						((PlanRequestAction) currentTask).getRequest().getSimulationAgent().getSimpleId(), getId()));
	}
	
	@Override
	public VehicleTrip getCurrentTripPlan() {
		return currentTrip;
	}

	public boolean hasFreeCapacity() {
		return getFreeCapacity() > 0;
	}

	public boolean hasFreeTrunkCapacity() {
		return getFreeTrunkCapacity() > 0;
	}
	
	public List<PlanLayerTrip> getPlanForRendering(){
		List<PlanLayerTrip> trips = new ArrayList<>(currentPlan.getLength());
		SimulationNode lastPosition = getPosition();
		for(PlanAction action: currentPlan){
			if(action instanceof PlanRequestAction && lastPosition != action.getPosition()){
				VehicleTrip<SimulationNode> newTrip
						= tripsUtil.createTrip(lastPosition, action.getPosition(), vehicle);
				trips.add(new PlanLayerTrip(tripIdGenerator.getId(),(PlanRequestAction) action, newTrip.getLocations()));
				lastPosition = action.getPosition();
			}
		}
		return trips;
	}

	private void logTraveledDistance(boolean wasStopped) {
		int length = wasStopped ? positionUtil.getTripLengthInMeters(currentTrip, getPosition())
				: positionUtil.getTripLengthInMeters(currentTrip);
		
		if(getOnBoardCount() > 0){
			metersWithPassenger += length;
		}
		else{
			switch(state){
				case DRIVING_TO_START_LOCATION:
					metersToStartLocation += length;
					break;
				case DRIVING_TO_STATION:
					metersToStation += length;
					break;
			}
		}
		
	}

}
