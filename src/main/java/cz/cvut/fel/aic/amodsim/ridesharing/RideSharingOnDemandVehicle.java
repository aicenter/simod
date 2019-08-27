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
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.PlanActionCurrentPosition;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle{

	private final VisioPositionUtil positionUtil;
	
	private DriverPlan currentPlan;
	
	private PlanAction currentTask;


	public DriverPlan getCurrentPlan() {
		currentPlan.updateCurrentPosition(getPosition());
		return currentPlan;
	}
	
	
	
	
	@Inject
	public RideSharingOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, 
			TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
			PhysicalVehicleDriveFactory driveActivityFactory, VisioPositionUtil positionUtil, EventProcessor eventProcessor, 
			StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, AmodsimConfig config, 
			@Assisted String vehicleId, @Assisted SimulationNode startPosition) {
		super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral,
				driveActivityFactory, positionUtil, eventProcessor, timeProvider, rebalancingIdGenerator, config, 
				vehicleId, startPosition);
		this.positionUtil = positionUtil;
		
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
	
	public int getFreeCapacity(){
		return vehicle.getCapacity() - vehicle.getTransportedEntities().size();
	}
	
	public void replan(DriverPlan plan){
		currentPlan = plan;
		if(state != OnDemandVehicleState.WAITING){
			((PhysicalVehicleDrive) getCurrentTopLevelActivity()).end();
		}
		else{
			driveToNextTask();
		}
	}

	@Override
	protected void driveToDemandStartLocation() {
		// safety check that prevents request from being picked up twice because of the delayd pickup event
		if(((PlanActionPickup) currentTask).request.isOnboard()){
			driveToNextTask();
		}
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
		if(getPosition().id == currentTask.getPosition().id){
			pickupAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getPosition().id, vehicle);
			DemandAgent demandAgent = ((PlanActionPickup) currentTask).getRequest().getDemandAgent();
			driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
		}
	}

	@Override
	protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
		if(getPosition().id == currentTask.getPosition().id){
			dropOffAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getPosition().id, vehicle);
			driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
		}
	}

	@Override
	protected void driveToNearestStation() {
		state = OnDemandVehicleState.DRIVING_TO_STATION;
		targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());
		
		if(getPosition().equals(targetStation.getPosition())){
			finishDrivingToStation(((PlanRequestAction) currentTask).getRequest().getDemandAgent());
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
					targetStation.getPosition().getId(), vehicle);
			driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
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
					dropOffAndContinue();
					break;
				case DRIVING_TO_STATION:
					finishDrivingToStation(((PlanRequestAction) currentTask).getRequest().getDemandAgent());
					break;
				case REBALANCING:
					finishRebalancing();
					break;
			}
		}
	}

	private void driveToNextTask() {
		if(currentPlan.getLength() == 1){
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
			DemandAgent demandAgent = ((PlanActionPickup) currentTask).getRequest().getDemandAgent();
			if(demandAgent.isDropped()){
				throw new Exception(
					String.format("Demand agent %s cannot be picked up, he is already dropped!", demandAgent));
			}
			demandAgent.tripStarted(this);
			vehicle.pickUp(demandAgent);

			// statistics TODO demand tirp?
	//		demandTrip = tripsUtil.createTrip(currentTask.getDemandAgent().getPosition().id,
	//				currentTask.getLocation().id, vehicle);
			// demand trip length 0 - need to find out where the statistic is used, does it make sense with rebalancing?
			eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, 
					new PickupEventContent(timeProvider.getCurrentSimTime(), 
							demandAgent.getSimpleId(), getId(), 0));
			currentPlan.taskCompleted();
			driveToNextTask();

		} catch (Exception ex) {
			Logger.getLogger(RideSharingOnDemandVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void dropOffAndContinue() {
		DemandAgent demandAgent = ((PlanActionDropoff) currentTask).getRequest().getDemandAgent();
		demandAgent.tripEnded();
		vehicle.dropOff(demandAgent);
		
		// statistics
		eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null, 
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						demandAgent.getSimpleId(), getId()));
		currentPlan.taskCompleted();
		driveToNextTask();
	}

	@Override
	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, 
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						((PlanRequestAction) currentTask).getRequest().getDemandAgent().getSimpleId(), getId()));
	}
	
	@Override
	public VehicleTrip getCurrentTripPlan() {
		return currentTrip;
	}

	public boolean hasFreeCapacity() {
		return getFreeCapacity() > 0;
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
