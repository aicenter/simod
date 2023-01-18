package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.*;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.lang.*;
import java.util.*;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.LoggerFactory;



@Singleton
public class PFSolverMultiInsertion extends DARPSolverPFShared implements EventHandler {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PFSolverMultiInsertion.class);

	private static final int INFO_PERIOD = 1000;

	private final PositionUtil positionUtil;

	private final SimodConfig config;

	private final double maxDistance ;

	private final double maxDistanceSquared;

	private final int maxDelayTime;

	private final TimeProvider timeProvider;

	private final TypedSimulation eventProcessor;

	private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;

	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;


	private long callCount = 0;

	private long totalTime = 0;

	private long iterationTime = 0;

	private long peopleFreightHeuristicTime;

	private volatile long canServeRequestCallCount = 0;

	private long vehiclePlanningAllCallCount = 0;

	private int failFastTime;

	private int debugFailTime;

	private double minCostIncrement;

	OnDemandVehicle vehicleFromNearestStation;

	private int[] usedVehiclesPerStation;

	long totalBenefit = 0;

	private List<PeopleFreightVehicle> allVehicles;

	private List<List<PlanAction>> taxiSchedules;

	List<SimulationNode> taxiCurrentPositions;

	List<PlanActionCurrentPosition> taxiCurrentPositionsActions;

	ScheduleWithDuration bestPlan;

	int bestPlanTaxiIndex;

	@Inject
	public PFSolverMultiInsertion(
			TravelTimeProvider travelTimeProvider,
			PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage,
			PositionUtil positionUtil,
			SimodConfig config,
			TimeProvider timeProvider,
			DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory,
			TypedSimulation eventProcessor,
			DroppedDemandsAnalyzer droppedDemandsAnalyzer,
			OnDemandvehicleStationStorage onDemandvehicleStationStorage,
			AgentpolisConfig agentpolisConfig) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.positionUtil = positionUtil;
		this.config = config;
		this.timeProvider = timeProvider;
		this.eventProcessor = eventProcessor;
		this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.bestPlan = null;


        // max distance in meters between vehicle and request for the vehicle to be considered to serve the request
        maxDistance = (double) config.ridesharing.maxProlongationInSeconds
                * agentpolisConfig.maxVehicleSpeedInMeters;
        maxDistanceSquared = maxDistance * maxDistance;

        // the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
        // vehicle to be considered to serve the request
        maxDelayTime = config.ridesharing.maxProlongationInSeconds * 1000;

		setEventHandeling();
	}


	/**
	 * @param newRequestsPeople      - people requests
	 * @param waitingRequestsPeople  - not used
	 * @param newRequestsFreight     - freight requests
	 * @param waitingRequestsFreight - not used
	 * @return list of plans for all vehicles
	 */
	@Override
	public Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solve(
			List<PlanComputationRequest> newRequestsPeople,
			List<PlanComputationRequest> waitingRequestsPeople,
			List<PlanComputationRequest> newRequestsFreight,
			List<PlanComputationRequest> waitingRequestsFreight) {


// --------------------------- INIT ---------------------------------------
		allVehicles = new ArrayList<>();
		taxiSchedules = new ArrayList<>();
		taxiCurrentPositions = new ArrayList<>();
		taxiCurrentPositionsActions = new ArrayList<>();

		// statistics
		peopleFreightHeuristicTime = 0;

		// initializing the vehicles and plans
		for (AgentPolisEntity taxiEntity : vehicleStorage.getEntitiesForIteration()) {
			PeopleFreightVehicle newTaxi = (PeopleFreightVehicle) taxiEntity;
			allVehicles.add(newTaxi);
			List<PlanAction> newTaxiCurrentPlan = newTaxi.getCurrentPlanNoUpdate().plan;

			taxiCurrentPositionsActions.add((PlanActionCurrentPosition) newTaxiCurrentPlan.remove(0)); // separate the PlanActionCurrentPosition from the plan
			taxiCurrentPositions.add(newTaxi.getPosition());
			taxiSchedules.add(new ArrayList<>(newTaxiCurrentPlan));
		}
// --------------------------- END INIT ------------------------------------

		// maybe in future: calculate plan discomfort
		int planDiscomfort = 0;


		// list of plan durations for each taxi
		List<Integer> planDurations = new ArrayList<>(Arrays.asList(new Integer[allVehicles.size()]));
		Collections.fill(planDurations, 0);


		List<PlanComputationRequest> newRequestsAll = new ArrayList<>();
		if (newRequestsPeople != null) {
			newRequestsAll.addAll(newRequestsPeople);
		}
		if (newRequestsFreight != null) {
			newRequestsAll.addAll(newRequestsFreight);
		}


		// sort requests incrementally by time windows (their maxPickupTime)
		newRequestsAll.sort(new SortRequestsByOriginTime());

		List<PeopleFreightVehicle> availableTaxis;

		for (int i = 0; i < newRequestsAll.size(); i++) {
			DefaultPFPlanCompRequest currentRequest = (DefaultPFPlanCompRequest) newRequestsAll.get(i);
			// reset the bestPlan
			bestPlan = new ScheduleWithDuration(null, 0, Double.MAX_VALUE);

			bestPlanTaxiIndex = -1;

			// select vehicles available at the moment
			availableTaxis = new ArrayList<>();
			for (int j = 0; j < allVehicles.size(); j++) {
				// first check if vehicle is in a range of maxDistance
				if (!(canServeRequest(allVehicles.get(j), currentRequest))) {
					continue;
				}

				// if currentRequest is of type package: check if vehicle has enough space for the package
				if (currentRequest instanceof PlanComputationRequestFreight) {
					if (((PlanComputationRequestFreight) currentRequest).getWeight() <= allVehicles.get(j).getFreePackagesCapacity()) {
						availableTaxis.add(allVehicles.get(j));
					}
				}
				// otherwise: check if vehicle has enough passenger capacity
				else if (allVehicles.get(j).getFreeCapacity() > 0) {
					availableTaxis.add(allVehicles.get(j));
				}
			}

			if (availableTaxis.size() > 0) {
				for (PeopleFreightVehicle currentTaxi : availableTaxis) {
					// find best schedule and measure the computation time
					Benchmark benchmark = new Benchmark();
					benchmark.measureTime(() -> findBestScheduleInsertion(allVehicles.indexOf(currentTaxi), currentRequest));
					peopleFreightHeuristicTime += benchmark.getDurationMsInt();
				}

				// set the bestPlan to
				if (bestPlanTaxiIndex != -1 ) {
					taxiSchedules.set(bestPlanTaxiIndex, bestPlan.schedule);
				}
			}

		}


		Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> returnMap = new HashMap<>();

		for (int i = 0; i < taxiSchedules.size(); i++) {
			List<PlanAction> actionsList = new ArrayList<>(taxiSchedules.get(i));
			actionsList.add(0, taxiCurrentPositionsActions.get(i));
			int planTime = planDurations.get(i);
			double planCost = planCostProvider.calculatePlanCost(planDiscomfort, planTime);

			DriverPlan newPlan = new DriverPlan(actionsList, planTime, planCost);
			returnMap.put(allVehicles.get(i), newPlan);
		}

		logRidesharingStats(newRequestsAll);

		return returnMap;
	}


	// Inserts the newRequests' actions on specified indexes of plan of taxi[taxiIndex]
	// Returns feasible plan or null
	private ScheduleWithDuration insertIntoPlan(int taxiIndex, PlanComputationRequest newRequest, int pickupIndex, int dropoffIndex) {
		int planDiscomfort = 0;
		List<PlanAction> newTaxiSchedule = new ArrayList<>(taxiSchedules.get(taxiIndex));

		newTaxiSchedule.add(pickupIndex, newRequest.getPickUpAction());
		newTaxiSchedule.add(dropoffIndex, newRequest.getDropOffAction());

		// pairs of EarlyTime and LateTime
		List<TimeWindow> timeWindows = new ArrayList<>();

		long currentTime = timeProvider.getCurrentSimTime();
		SimulationNode firstDemandPosition = newTaxiSchedule.get(0).getPosition();

		long firstTravelTime = travelTimeProvider.getTravelTime(allVehicles.get(taxiIndex), firstDemandPosition);
		long firstActionMaxTime = ((PlanRequestAction) newTaxiSchedule.get(0)).request.getMaxPickupTime() * 1000L;
		long timeAtFirstAction = currentTime + firstTravelTime;

		// if the arrival time to the first action is higher than it's maxTime -> Terminate (not feasible)
		if (timeAtFirstAction >= firstActionMaxTime) {
			return null;
		}

		// setup time windows
		for (PlanAction planAction : newTaxiSchedule) {
			timeWindows.add(new TimeWindow(timeAtFirstAction, ((PlanRequestAction) planAction).getMaxTime() * 1000L));
		}

		int freePassengersCapacity = allVehicles.get(taxiIndex).getFreeCapacity();
		int freeFreightCapacity = allVehicles.get(taxiIndex).getFreePackagesCapacity();

		for (int i = 0; i < newTaxiSchedule.size() - 1; i++) {
			PlanAction action = newTaxiSchedule.get(i);
			if (action instanceof PlanActionPickup) {
				PlanActionPickup pickAction = (PlanActionPickup) action;
				PlanComputationRequest pickRequest = pickAction.request;
				// if Package
				if (pickRequest instanceof PlanComputationRequestFreight) {
					// if not sufficient freight capacity, reject
					if (freeFreightCapacity - ((PlanComputationRequestFreight) pickRequest).getWeight() < 0) {
						return null;
					}
					// adding the package onBoard
					freeFreightCapacity -= ((PlanComputationRequestFreight) pickRequest).getWeight();
				}
				// if Agent
				else {
					// if not sufficient passenger capacity, reject
					if (freePassengersCapacity == 0) {
						return null;
					}
					// else add the person onBoard
					freePassengersCapacity -= 1;
				}
			}
			else if (action instanceof PlanActionDropoff) {
				PlanActionDropoff dropAction = (PlanActionDropoff) action;
				PlanComputationRequest dropRequest = dropAction.request;

				// if Agent
				if (dropRequest instanceof PlanComputationRequestPeople) {
					// remove the person from the taxi
					freePassengersCapacity += 1;
				}
				// if Package
				else {
					freeFreightCapacity += ((PlanComputationRequestFreight) dropRequest).getWeight();
				}
			}
			else {
				throw new NotImplementedException("PeopleFreightHeuristicSolver.trySchedule(): Action is neither pickup nor dropoff");
			}


			TimeWindow currentTimeWindow = timeWindows.get(i);
			long travelTime = travelTimeProvider.getExpectedTravelTime(newTaxiSchedule.get(i).getPosition(),
					newTaxiSchedule.get(i + 1).getPosition());
			long earlyTime = currentTimeWindow.earlyTime + travelTime;
			long lateTime = currentTimeWindow.lateTime + travelTime;

			// if taxi is getting to the next Node after maxTime of the Node
			if (earlyTime > timeWindows.get(i + 1).lateTime) {
				// not feasible -> terminate
				return null;
			}
			timeWindows.set(i + 1, new TimeWindow(Math.max(earlyTime, timeWindows.get(i + 1).earlyTime),
					Math.min(lateTime, timeWindows.get(i + 1).lateTime)));
		}

		long planDuration = timeWindows.get(timeWindows.size() - 1).earlyTime - timeWindows.get(0).earlyTime;
		double planCost = planCostProvider.calculatePlanCost(planDiscomfort, (int) planDuration / 1000);

		return new ScheduleWithDuration(newTaxiSchedule, planDuration, planCost);
	}

	// Tries to insert the new request into all possible positions in the plan of taxi[taxiIndex]
	// Updates the bestPlan, if better one was found
	private void findBestScheduleInsertion(int taxiIndex, PlanComputationRequest newRequest) {
		ScheduleWithDuration localBestSchedule = null;
		double bestLocalPlanCost = Double.MAX_VALUE;

		List<PlanAction> currentSchedule = taxiSchedules.get(taxiIndex);

		int freePassengerCapacity = allVehicles.get(taxiIndex).getFreeCapacity();
		int freePackagesCapacity = allVehicles.get(taxiIndex).getFreePackagesCapacity();
		
		for (int pickupOptionIndex = 0; pickupOptionIndex <= currentSchedule.size(); pickupOptionIndex++) {

			if (newRequest instanceof PlanComputationRequestPeople && freePassengerCapacity == 0 ||
				newRequest instanceof PlanComputationRequestFreight && freePackagesCapacity < ((PlanComputationRequestFreight) newRequest).getWeight()) {
				continue;
			}

			for (int dropoffOptionIndex = pickupOptionIndex + 1;
				 dropoffOptionIndex <= currentSchedule.size() + 1; dropoffOptionIndex++)
			{
				// try to put the new request to specified positions in plan
				ScheduleWithDuration potentialSchedule = insertIntoPlan(taxiIndex, newRequest, pickupOptionIndex, dropoffOptionIndex);
				if (potentialSchedule != null) {
					if (potentialSchedule.planCost < bestLocalPlanCost) {
						bestLocalPlanCost = potentialSchedule.planCost;
						localBestSchedule = potentialSchedule;
					}
				}
			}

			// update free capacity for next index
			if (pickupOptionIndex < currentSchedule.size())
			{
				// PickUp
				if (currentSchedule.get(pickupOptionIndex) instanceof PlanActionPickup)
				{
					PlanComputationRequest request = ((PlanActionPickup) currentSchedule.get(pickupOptionIndex)).request;
					if (request instanceof PlanComputationRequestPeople) {
						freePassengerCapacity--;
					}
					else {
						freePackagesCapacity -= ((PlanComputationRequestFreight) request).getWeight();
					}
				}
				// Dropoff
				else
				{
					PlanComputationRequest request = ((PlanRequestAction) currentSchedule.get(pickupOptionIndex)).request;
					if (request instanceof PlanComputationRequestPeople) {
						freePassengerCapacity++;
					}
					else {
						freePackagesCapacity += ((PlanComputationRequestFreight) request).getWeight();
					}
				}
			}
		}

		// update the bestSchedule, if better one was found
		if (bestLocalPlanCost < bestPlan.planCost) {
			bestPlan = localBestSchedule;
			bestPlanTaxiIndex = taxiIndex;
		}
	}

	private void logRidesharingStats(List<PlanComputationRequest> requests)
	{
		ridesharingStats.add(new RidesharingBatchStatsPFH(requests.size(), peopleFreightHeuristicTime, 0, 0));
	}

	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
		canServeRequestCallCount++;

		// do not mess with rebalancing
		if (vehicle.getState() == OnDemandVehicleState.REBALANCING) {
			return false;
		}

		// node identity
		if (vehicle.getPosition() == request.getFrom()) {
			return true;
		}

		// euclidean distance check
		double dist_x = vehicle.getPosition().getLatitudeProjected() - request.getFrom().getLatitudeProjected();
		double dist_y = vehicle.getPosition().getLongitudeProjected() - request.getFrom().getLongitudeProjected();
		double distanceSquared = dist_x * dist_x + dist_y * dist_y;
		if (distanceSquared > maxDistanceSquared) {
			return false;
		}

		// real feasibility check
		boolean canServe = travelTimeProvider.getTravelTime(vehicle, request.getFrom())
				< maxDelayTime;


		return canServe;
	}

	@Override
	public void handleEvent(Event event) {

	}

	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}


	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		eventProcessor.addEventHandler(this, typesToHandle);
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests,
															 List<PlanComputationRequest> waitingRequests) {
		throw new NotImplementedException("Not implemented in People Freight Heuristic Solver class.");
	}

}
