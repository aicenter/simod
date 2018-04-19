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
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.amodsim.entity.vehicle.Demand;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle{
    
    private final LinkedList<Node> targetNodes;
    
    private final LinkedList<Demand> demands;
    
    private final LinkedList<Demand> pickedDemands;
    
    private final PositionUtil positionUtil;
	
	private DriverPlan currentPlan;
	
	private DriverPlanTask currentTask;
	
	private PhysicalVehicleDrive<RideSharingOnDemandVehicle> currentDriveActivity;

	
	
	public DriverPlan getCurrentPlan() {
		currentPlan.updateCurrentPosition(getPosition());
		return currentPlan;
	}
	
	
    
    
    @Inject
    public RideSharingOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, 
            TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            StandardDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor, 
            StandardTimeProvider timeProvider, @Named("precomputedPaths") boolean precomputedPaths, 
            IdGenerator rebalancingIdGenerator, AmodsimConfig config, @Assisted String vehicleId, 
            @Assisted SimulationNode startPosition) {
        super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral,
                driveActivityFactory, positionUtil, eventProcessor, timeProvider, precomputedPaths, 
                rebalancingIdGenerator, config, vehicleId, startPosition);
        this.positionUtil = positionUtil;
        targetNodes = new LinkedList<>();
        demands = new LinkedList<>();
        pickedDemands = new LinkedList<>();
		currentPlan = new DriverPlan(new LinkedList<>());
    }

	@Override
	public void handleEvent(Event event) {
		
	}
        
    public boolean hasFreeCapacity(){
        return targetNodes.size() < vehicle.getCapacity();
    }
	
	public int getOnBoardCount(){
		return vehicle.getTransportedEntities().size();
	}
	
	public void replan(DriverPlan plan){
		currentPlan = plan;
		if(currentDriveActivity != null){
			currentDriveActivity.end();
		}
		driveToNextTask();
	}

    @Override
    protected void driveToDemandStartLocation() {
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
        if(getPosition().id == currentTask.getLocation().id){
			pickupAndContinue();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getLocation().id, vehicle);
            metersToStartLocation += positionUtil.getTripLengthInMeters(currentTrip);
			currentDriveActivity = driveFactory.create(this, vehicle, vehicleTripToTrip(currentTrip));
			currentDriveActivity.run();
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
			metersWithPassenger += positionUtil.getTripLengthInMeters(currentTrip);
			currentDriveActivity = driveFactory.create(this, vehicle, vehicleTripToTrip(currentTrip));
			currentDriveActivity.run();
		}
    }

    @Override
    protected void driveToNearestStation() {
        targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());
		
		if(getPosition().equals(targetStation.getPosition())){
			waitInStation();
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
					targetStation.getPosition().getId(), vehicle);
            metersToStation += positionUtil.getTripLengthInMeters(currentTrip);
			currentDriveActivity = driveFactory.create(this, vehicle, vehicleTripToTrip(currentTrip));
			currentDriveActivity.run();
		}
    }

    @Override
    public void finishedDriving() {
        switch(state){
            case DRIVING_TO_START_LOCATION:
				pickupAndContinue();
            case DRIVING_TO_TARGET_LOCATION:
				dropOffAndContinue();
            case DRIVING_TO_STATION:
				waitInStation();
            case REBALANCING:
                waitInStation();
                break;
        }
    }

	private void driveToNextTask() {
		if(currentPlan.getLength() == 1){
			if(state != OnDemandVehicleState.WAITING){
				driveToNearestStation();
			}
		}
		else{
			if(state == OnDemandVehicleState.WAITING){
				leavingStationEvent();
			}
			currentTask = currentPlan.getCurrentTask();
			if(currentTask.getTaskType() == DriverPlanTaskType.PICKUP){
				driveToDemandStartLocation();
			}
			else{
				driveToTargetLocation();
			}
		}
	}

	private void pickupAndContinue() {
		currentTask.demandAgent.tripStarted();
        vehicle.pickUp(currentTask.demandAgent);
		
		// statistics
		demandTrip = tripsUtil.createTrip(currentTask.getDemandAgent().getPosition().id,
				currentTask.getLocation().id, vehicle);
        eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, 
                new PickupEventContent(timeProvider.getCurrentSimTime(), 
                        currentTask.demandAgent.getSimpleId(), 
                        positionUtil.getTripLengthInMeters(demandTrip)));

		
	}

	private void dropOffAndContinue() {
		currentTask.demandAgent.tripEnded();
        vehicle.dropOff(currentTask.demandAgent);
		
		// statistics
        eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentTask.demandAgent.getSimpleId()));

		driveToNextTask();
	}
    
    
    
    
    
}
