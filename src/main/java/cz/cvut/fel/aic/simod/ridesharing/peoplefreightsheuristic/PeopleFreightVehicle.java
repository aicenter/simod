package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.TransportableEntityManagement;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PFPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PeopleFreightVehicle<T extends TransportableEntity> extends RideSharingOnDemandVehicle<T, PhysicalPFVehicle<T>>
{
	public final int vehiclePassengerCapacity = 1;

	private static final int LENGTH = 4;

	private boolean passengerOnboard;

	private final int maxParcelsCapacity;

	private int currentParcelsWeight;


	public PeopleFreightVehicle(
			PhysicalTransportVehicleStorage<PhysicalPFVehicle<T>> vehicleStorage,
			TripsUtil tripsUtil,
			StationsDispatcher onDemandVehicleStationsCentral,
			PhysicalVehicleDriveFactory driveActivityFactory,	// TODO: upravit factory pro tvoreni ??
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
			@Assisted PhysicalPFVehicle<T> vClass)
	{
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
				vClass);


		this.maxParcelsCapacity = maxParcelsCapacity;
		this.currentParcelsWeight = 0;
		this.passengerOnboard = false;
	}


	public int getMaxParcelsCapacity()
	{
		return maxParcelsCapacity;
	}

	public int getCurrentParcelsWeight()
	{
		return currentParcelsWeight;
	}

	public boolean isPassengerOnboard()
	{
		return passengerOnboard;
	}

	public void setPassengerOnboard(boolean passengerOnboard)
	{
		this.passengerOnboard = passengerOnboard;
	}

	public void setCurrentParcelsWeight(int curWeight)
	{
		this.currentParcelsWeight = curWeight;
	}

	private void pickupAndContinue() {
		try {
			PFPlanComputationRequest request = (PFPlanComputationRequest) ((PlanActionPickup) currentTask).getRequest();
			TransportableEntityManagement demandEntity = request.getDemandEntity();
			if (demandEntity.isDropped()) {
				long currentTime = timeProvider.getCurrentSimTime();
				long droppTime = demandEntity.getDemandTime() + config.ridesharing.maxProlongationInSeconds * 1000;
				throw new Exception(
						String.format("Demand agent %s cannot be picked up, he is already dropped! Current simulation "
								+ "time: %s, drop time: %s", demandEntity, currentTime, droppTime));
			}
			demandEntity.tripStarted(this);
			vehicle.pickUp((TransportableEntity) demandEntity);

			eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null,
					new PickupEventContent(timeProvider.getCurrentSimTime(),
							demandEntity.getSimpleId(), getId(),
							(int) Math.round(demandEntity.getMinDemandServiceDuration() / 1000)));
			currentPlan.taskCompleted();
			driveToNextTask();
		}
		catch (Exception ex) {
			Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void dropOffAndContinue() {
		PFPlanComputationRequest request = (PFPlanComputationRequest) ((PlanActionPickup) currentTask).getRequest();
		TransportableEntityManagementdemandEntity = request.getDemandEntity();
		demandEntity.tripEnded();
		vehicle.dropOff(demandEntity);

		eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null,
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(),
						demandEntity.getSimpleId(), getId()));
		currentPlan.taskCompleted();
		driveToNextTask();
	}
}
