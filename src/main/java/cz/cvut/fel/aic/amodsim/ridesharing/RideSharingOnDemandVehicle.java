/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RideSharingOnDemandVehicle.class);

    private static int tooLongTripsCount = 0;

    private final PositionUtil positionUtil;
    private DriverPlan currentPlan;
    private DriverPlanTask currentTask;

    double metersFromLastRecharge;
    private final long chargingTime;
    private final double maxDrivingRange;
    private final double maxRideTime;
        
    
    public DriverPlan getCurrentPlan() {
        currentPlan.updateCurrentPosition(getPosition());
        return currentPlan;
    }

    @Inject public RideSharingOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral,
            PhysicalVehicleDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor, StandardTimeProvider timeProvider,
            @Named("precomputedPaths") boolean precomputedPaths, IdGenerator rebalancingIdGenerator, AmodsimConfig config, @Assisted String vehicleId,
            @Assisted SimulationNode startPosition) {
        super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, precomputedPaths, rebalancingIdGenerator,
                config, vehicleId, startPosition);
        this.positionUtil = positionUtil;
        this.chargingTime = config.amodsim.ridesharing.chargingTime * 60 * 1000;
        this.maxDrivingRange = config.amodsim.ridesharing.drivingRange * 1000;
        this.maxRideTime = config.amodsim.ridesharing.maxRideTime * 60 * 1000;
        
        //	empty plan
        LinkedList<DriverPlanTask> plan = new LinkedList<>();
        plan.add(new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, getPosition()));
        currentPlan = new DriverPlan(plan, 0);
        
    }

    @Override public void handleEvent(Event event) {
        if (event.getType().equals(OnDemandVehicleEvent.CHARGING_COMPLETED)) {
            //LOGGER.info(vehicle.getId() + ": Charged");
            metersFromLastRecharge = 0;
            waitInStation();
        }
    }

    public int getOnBoardCount() {
        return vehicle.getTransportedEntities().size();
    }

    public int getFreeCapacity() {
        return vehicle.getCapacity() - vehicle.getTransportedEntities().size();
    }

    public void replan(DriverPlan plan) {
        currentPlan = plan;
        if (state != OnDemandVehicleState.WAITING) {
            ((PhysicalVehicleDrive) getCurrentTopLevelActivity()).end();
        } else {
            driveToNextTask();
        }
    }

    @Override protected void driveToDemandStartLocation() {
        state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
        if (getPosition().id == currentTask.getLocation().id) {
            pickupAndContinue();
        } else {
            currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getLocation().id, vehicle);
            driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
        }
    }

    @Override protected void driveToTargetLocation() {
        state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        if (getPosition().id == currentTask.getLocation().id) {
            dropOffAndContinue();
        } else {
            currentTrip = tripsUtil.createTrip(getPosition().id, currentTask.getLocation().id, vehicle);
            driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
        }
    }

    @Override protected void driveToNearestStation() {
        state = OnDemandVehicleState.DRIVING_TO_STATION;
        targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());

        if (getPosition().equals(targetStation.getPosition())) {
            finishDrivingToStation(currentTask.demandAgent);
        } else {
            currentTrip = tripsUtil.createTrip(getPosition().id, targetStation.getPosition().getId(), vehicle);
            driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
        }
    }

    protected void driveToNearestStationToCharge() {
        state = OnDemandVehicleState.CHARGING;
        targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());

        if (getPosition().equals(targetStation.getPosition())) {
            charge();
        } else {
            currentTrip = tripsUtil.createTrip(getPosition().id, targetStation.getPosition().getId(), vehicle);
            driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
        }
    }

    @Override public void finishedDriving(boolean wasStopped) {
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
                finishDrivingToStation(currentTask.demandAgent);
                break;
            case REBALANCING:
                finishRebalancing();
                break;
            case CHARGING:
                charge();
                break;
            }
        }
    }

    private void charge() {
        //LOGGER.info(vehicle.getId()+": Arrived to station for charging after "+ metersFromLastRecharge/1000 +" km");
        if(metersFromLastRecharge > maxDrivingRange){
            LOGGER.error(vehicle.getId()+": max driving range exceeded " + metersFromLastRecharge);
        }
        if(getOnBoardCount() > 0){
            LOGGER.error("Arrived to station with "+getOnBoardCount()+" passenger(s)");
        }
        getEventProcessor().addEvent(OnDemandVehicleEvent.CHARGING_COMPLETED, this, null, null, chargingTime);
        metersFromLastRecharge = 0;
        // add new event, so that after 2 hours vehicle state is changed from
        //charging to waiting.
    }

    private void driveToNextTask() {
        if (currentPlan.getLength() == 1) {
            if (state != OnDemandVehicleState.WAITING) {
                if (metersFromLastRecharge >= maxDrivingRange * 0.75) {
                    driveToNearestStationToCharge();
                } else {
                    driveToNearestStation();
                }
            }
        } else {
            currentTask = currentPlan.getNextTask();
            if (state == OnDemandVehicleState.WAITING) {
                parkedIn.releaseVehicle(this);
                leavingStationEvent();
            }
            if (currentTask.getTaskType() == DriverPlanTaskType.PICKUP) {
                driveToDemandStartLocation();
            } else {
                driveToTargetLocation();
            }
        }
    }

    private void pickupAndContinue() {
        currentTask.demandAgent.tripStarted(this);
        if (getPosition().id != currentTask.getLocation().id) {
            LOGGER.error(vehicle.getId() + ", wrong start location");
        }
        vehicle.pickUp(currentTask.demandAgent);
        eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, new PickupEventContent(timeProvider.getCurrentSimTime(), currentTask.demandAgent.getSimpleId(), 0));
        currentPlan.taskCompleted();
        driveToNextTask();
    }

    private void dropOffAndContinue() {
        double tripDuration = currentTask.getDemandAgent().getCurrentServiceDuration();
        currentTask.demandAgent.tripEnded();
        if (getPosition().id != currentTask.getLocation().id) {
            LOGGER.error(vehicle.getId() + ", wrong target location");
        }
        if(tripDuration > maxRideTime){
            LOGGER.error(vehicle.getId() + " max ride time exceeded: " + tripDuration+"; count "+(++tooLongTripsCount));
        }
        vehicle.dropOff(currentTask.demandAgent);

        // statistics
        eventProcessor
                .addEvent(OnDemandVehicleEvent.DROP_OFF, null, null,
                    new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), currentTask.demandAgent.getSimpleId()));

        currentPlan.taskCompleted();
        driveToNextTask();
    }

    @Override protected void leavingStationEvent() {
        eventProcessor
                .addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), currentTask.demandAgent.getSimpleId()));
    }

    @Override public VehicleTrip getCurrentTripPlan() {
        return currentTrip;
    }

    boolean hasFreeCapacity() {
        return getFreeCapacity() > 0;
    }

    private void logTraveledDistance(boolean wasStopped) {
        int length = wasStopped ? positionUtil.getTripLengthInMeters(currentTrip, getPosition()) : positionUtil.getTripLengthInMeters(currentTrip);
        //System.out.println("Log trave distance: length "+length);
        metersFromLastRecharge += length;

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
}
