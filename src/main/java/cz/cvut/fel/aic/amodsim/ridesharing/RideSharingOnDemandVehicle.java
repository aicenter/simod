/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
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
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle{

    private final VisioPositionUtil positionUtil;
	
	private DriverPlan currentPlan;
	
	private DriverPlanTask currentTask;


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
		LinkedList<DriverPlanTask> plan = new LinkedList<>();
		plan.add(new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, getPosition()));
		currentPlan = new DriverPlan(plan, 0);
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
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
        if(getPosition().id == currentTask.getLocation().id){
			pickupAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getLocation().id, vehicle);
			driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
		}
    }

    @Override
    protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        if(getPosition().id == currentTask.getLocation().id){
			dropOffAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getLocation().id, vehicle);
			driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
		}
    }

    @Override
    protected void driveToNearestStation() {
		state = OnDemandVehicleState.DRIVING_TO_STATION;
        targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());
		
		if(getPosition().equals(targetStation.getPosition())){
			finishDrivingToStation(currentTask.demandAgent);
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
					finishDrivingToStation(currentTask.demandAgent);
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
				parkedIn = null;
				leavingStationEvent();
			}
			if(currentTask.getTaskType() == DriverPlanTaskType.PICKUP){
				driveToDemandStartLocation();
			}
			else{
				driveToTargetLocation();
			}
		}
	}

	private void pickupAndContinue() {
		currentTask.demandAgent.tripStarted(this);
        vehicle.pickUp(currentTask.demandAgent);
		
		// statistics TODO demand tirp?
//		demandTrip = tripsUtil.createTrip(currentTask.getDemandAgent().getPosition().id,
//				currentTask.getLocation().id, vehicle);
		// demand trip length 0 - need to find out where the statistic is used, does it make sense with rebalancing?
        eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, 
                new PickupEventContent(timeProvider.getCurrentSimTime(), 
                        currentTask.demandAgent.getSimpleId(), getId(), 0));
		currentPlan.taskCompleted();
		driveToNextTask();
	}

	private void dropOffAndContinue() {
		currentTask.demandAgent.tripEnded();
        vehicle.dropOff(currentTask.demandAgent);
		
		// statistics
        eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentTask.demandAgent.getSimpleId(), getId()));
		
		currentPlan.taskCompleted();
		driveToNextTask();
	}

	@Override
	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentTask.demandAgent.getSimpleId(), getId()));
	}
	
	@Override
	public VehicleTrip getCurrentTripPlan() {
		return currentTrip;
	}

	boolean hasFreeCapacity() {
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
