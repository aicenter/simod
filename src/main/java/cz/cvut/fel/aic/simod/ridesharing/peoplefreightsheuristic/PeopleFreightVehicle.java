package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandEntityType;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PeopleFreightVehicle extends RideSharingOnDemandVehicle {

	private boolean passengerOnboard;

	@Inject
	public PeopleFreightVehicle(
			PhysicalTransportVehicleStorage vehicleStorage,
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
			String vehicleId,
			SimulationNode startPosition,
			int maxParcelsCapacity,
			@Assisted PhysicalPFVehicle physVehicle) {
		super(
				vehicleStorage,
				tripsUtil,
				onDemandVehicleStationsCentral,
				driveActivityFactory,
				positionUtil,
				tripIdGenerator,
				eventProcessor,
				timeProvider,
				rebalancingIdGenerator,
				config,
				idGenerator,
				agentpolisConfig,
				vehicleId,
				startPosition,
				physVehicle);


//		vehicle.setDriver(this);
		this.passengerOnboard = false;
	}


	public int getMaxPackagesCapacity() {
		return ((PhysicalPFVehicle) vehicle).getPackagesCapacity();
	}

	public int getCurrentPackagesWeight() {
		return ((PhysicalPFVehicle) vehicle).getCurrentPackagesWeight();
	}

	public int getFreePackagesCapacity() {
		return ((PhysicalPFVehicle) vehicle).getPackagesCapacity() - ((PhysicalPFVehicle) vehicle).getCurrentPackagesWeight();
	}

	public boolean isPassengerOnboard() {
		return passengerOnboard;
	}

	public void setPassengerOnboard(boolean passengerOnboard) {
		this.passengerOnboard = passengerOnboard;
	}


	@Override
	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null,
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
						(((PlanRequestAction) currentTask).getRequest()).getDemandEntity().getSimpleId(), getId()));
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
		}
		else {
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
//			DemandAgent demandAgent = ((PlanActionPickup) currentTask).getRequest().getDemandAgent();
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
		if (getPosition().id == currentTask.getPosition().id) {
			dropOffAndContinue();
		}
		else {
			currentTrip = tripsUtil.createTrip(getPosition(), currentTask.getPosition(), vehicle);
			driveFactory.runActivity(this, vehicle, currentTrip);
		}
	}

	@Override
	protected void pickupAndContinue() {
		try {
			DefaultPFPlanCompRequest request = (DefaultPFPlanCompRequest) ((PlanActionPickup) currentTask).getRequest();
			TransportableDemandEntity demandEntity = request.getDemandEntity();
			if (passengerOnboard) {
				throw new Exception(
						String.format("Demand entity %s cannot be picked up, because passenger is onboard! Current simulation "
								+ "time: %s", demandEntity, timeProvider.getCurrentSimTime()));
			}

			if (demandEntity.isDropped()) {
				long currentTime = timeProvider.getCurrentSimTime();
				long droppTime = demandEntity.getDemandTime() + config.ridesharing.maxProlongationInSeconds * 1000;
				throw new Exception(
						String.format("Demand entity %s cannot be picked up, it is already dropped! Current simulation "
								+ "time: %s, drop time: %s", demandEntity, currentTime, droppTime));
			}
			demandEntity.tripStarted(this);
			vehicle.pickUp(demandEntity);

//			if (request instanceof PlanComputationRequestFreight) {
//				int newParcelsWeight = getCurrentParcelsWeight() + ((PlanComputationRequestFreight) request).getWeight();
//				setCurrentParcelsWeight(newParcelsWeight);
//			}

			DemandEntityType demandType;
			if (request instanceof PlanComputationRequestPeople) {
				passengerOnboard = true;
				demandType = DemandEntityType.AGENT;
			}
			else {
				demandType = DemandEntityType.PACKAGE;
			}


			eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null,
					new PeopleFreightVehicleEventContent(timeProvider.getCurrentSimTime(),
							demandEntity.getSimpleId(), getId(),
							(int) Math.round(demandEntity.getMinDemandServiceDuration() / 1000), demandType));
			currentPlan.taskCompleted();
			driveToNextTask();
		}
		catch (Exception ex) {
			Logger.getLogger(PhysicalPFVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	protected void dropOffAndContinue() {
		try {
			DefaultPFPlanCompRequest request = (DefaultPFPlanCompRequest) ((PlanActionDropoff) currentTask).getRequest();
			TransportableDemandEntity demandEntity = request.getDemandEntity();

			if (passengerOnboard && request instanceof PlanComputationRequestFreight) {
				throw new Exception(
						String.format("Demand entity %s cannot be dropped off, because passenger is onboard! Current simulation "
								+ "time: %s", demandEntity, timeProvider.getCurrentSimTime()));
				//TODO time=575, vehicle 1-5, package 44
			}
			demandEntity.tripEnded();
			vehicle.dropOff(demandEntity);

			//		if (request instanceof PlanComputationRequestFreight) {
			//			int newParcelsWeight = getCurrentParcelsWeight() - ((PlanComputationRequestFreight) request).getWeight();
			//			setCurrentParcelsWeight(newParcelsWeight);
			//		}
			if (request instanceof PlanComputationRequestPeople) {
				passengerOnboard = false;
			}

			eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null,
					new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
							demandEntity.getSimpleId(), getId()));
			currentPlan.taskCompleted();
			driveToNextTask();
		}
		catch (Exception ex) {
			Logger.getLogger(PhysicalPFVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
