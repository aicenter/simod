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
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.*;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.lang.*;
import java.util.*;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.LoggerFactory;

/**
 * comparator for sorting Requests
 */
//class SortRequestsByOriginTime implements Comparator<PlanComputationRequest> {
//	public int compare(PlanComputationRequest a, PlanComputationRequest b) {
//		return a.getOriginTime() - b.getOriginTime();
//	}
//}
//
//class SortActionsByMaxTime implements Comparator<PlanAction> {
//	public int compare(PlanAction a, PlanAction b) {
//		return ((PlanRequestAction) a).getMaxTime() - ((PlanRequestAction) b).getMaxTime();
//	}
//}
//
//// structure to store a taxi schedule and it's duration
//class ScheduleWithDuration {
//	protected final List<PlanAction> schedule;
//	protected final long duration;
//
//	public ScheduleWithDuration(List<PlanAction> schedule, long duration) {
//		this.schedule = schedule;
//		this.duration = duration;
//	}
//}
//
//class TimeWindow {
//	protected long earlyTime;
//	protected long lateTime;
//
//	public TimeWindow(long earlyTime, long lateTime) {
//		this.earlyTime = earlyTime;
//		this.lateTime = lateTime;
//	}
//}


@Singleton
public class PFSolverBase extends DARPSolverPFShared implements EventHandler {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PFSolverBase.class);

	private static final int INFO_PERIOD = 1000;

	private final PositionUtil positionUtil;

	private final SimodConfig config;

	private final double maxDistance = 10;

	private final double maxDistanceSquared = 100;

	private final int maxDelayTime = 1000;

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

	ScheduleWithDuration bestSchedule;

	@Inject
	public PFSolverBase(
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
		this.bestSchedule = null;

		setEventHandeling();


/*
        // max distance in meters between vehicle and request for the vehicle to be considered to serve the request
        maxDistance = (double) config.ridesharing.maxProlongationInSeconds
                * agentpolisConfig.maxVehicleSpeedInMeters;
        maxDistanceSquared = maxDistance * maxDistance;
        // the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
        // vehicle to be considered to serve the request
        maxDelayTime = config.ridesharing.maxProlongationInSeconds * 1000;
*/


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


		// sort requests incrementally by time windows - maxPickupTime
		newRequestsAll.sort(new SortRequestsByOriginTime());

		List<PeopleFreightVehicle> availableTaxis;

		for (int i = 0; i < newRequestsAll.size(); i++) {
			DefaultPFPlanCompRequest currentRequest = (DefaultPFPlanCompRequest) newRequestsAll.get(i);

			// select vehicles available at the moment
			availableTaxis = new ArrayList<>();
			for (int j = 0; j < allVehicles.size(); j++) {
				// if taxi j is not carrying person at the moment
				if (!allVehicles.get(j).isPassengerOnboard()) {

					// if currentRequest is of type package: check if vehicle has enough space for the package
					if (currentRequest instanceof PlanComputationRequestFreight) {
						if (((PlanComputationRequestFreight) currentRequest).getWeight() <= allVehicles.get(j).getFreePackagesCapacity()) {
							availableTaxis.add(allVehicles.get(j));
						}
					}
					// otherwise add it right away
					else {
						availableTaxis.add(allVehicles.get(j));
					}

//					availableTaxis.add(allVehicles.get(j));
				}
			}

			double bestBenefit;         // f_i* - best total benefit, if request i is served
			int bestTaxiIdx = -1;        // k* - taxi to serve request i to get the best total benefit

			if (availableTaxis.size() > 0) {
				bestBenefit = Double.NEGATIVE_INFINITY;
				for (int k = 0; k < availableTaxis.size(); k++) {
					PeopleFreightVehicle currentTaxi = availableTaxis.get(k);
					// check if schedule is feasible

					Benchmark benchmark = new Benchmark();
					benchmark.measureTime(() -> trySchedule(allVehicles.indexOf(currentTaxi), currentRequest));
					peopleFreightHeuristicTime += benchmark.getDurationMsInt();

//					trySchedule(allVehicles.indexOf(currentTaxi), currentRequest);

					// if not feasible, continue to next taxi
					if (bestSchedule == null) {
						continue;
					}

					// benefit_k_i = new total benefit if taxi k serves request i
					// TODO implement - calculate passenger's revenue ???
					double passengerRevenue = 0;
					double benefit_k_i = passengerRevenue - planCostProvider.calculatePlanCost(planDiscomfort, (int)(bestSchedule.duration / 1000));
					if (benefit_k_i > bestBenefit) {
						bestBenefit = benefit_k_i;
						bestTaxiIdx = allVehicles.indexOf(currentTaxi);        // updating the idx of best taxi so far
					}
				}
				// if suitable taxi was found
				if (bestTaxiIdx != -1) {
					// Insert request i into route of taxi k∗
					trySchedule(bestTaxiIdx, currentRequest);
					taxiSchedules.set(bestTaxiIdx, bestSchedule.schedule);  // NullPointerException won't happen, because this block happens only if at least 1 taxi was found
					planDurations.set(bestTaxiIdx, (int) (bestSchedule.duration / 1000));

					// update total benefit
					totalBenefit += bestBenefit;
				}
			}
			// else: reject request i => DO nothing
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


	/**
	 * returns sorted list of new taxi schedule (or null if the schedule is not feasible) and duration of this schedule
	 */
	private void trySchedule(int taxiIndex, PlanComputationRequest newRequest) {

		int planDiscomfort = 0;
		bestSchedule = null;
		List<PlanAction> possibleTaxiSchedule = new ArrayList<>(taxiSchedules.get(taxiIndex));

		possibleTaxiSchedule.add(newRequest.getPickUpAction());
		possibleTaxiSchedule.add(newRequest.getDropOffAction());
		possibleTaxiSchedule.sort(new SortActionsByMaxTime());

		// pairs of EarlyTime and LateTime
		List<TimeWindow> timeWindows = new ArrayList<>();

		long currentTime = timeProvider.getCurrentSimTime();
		SimulationNode firstDemandPosition = possibleTaxiSchedule.get(0).getPosition();

		long firstTravelTime = travelTimeProvider.getTravelTime(allVehicles.get(taxiIndex), firstDemandPosition);
		long firstActionMaxTime = ((PlanRequestAction) possibleTaxiSchedule.get(0)).request.getMaxPickupTime() * 1000L;
		long timeAtFirstAction = currentTime + firstTravelTime;

		// if the arrival time to the first action is higher than it's maxTime -> Terminate (not feasible)
		if (timeAtFirstAction >= firstActionMaxTime) {
			return;
		}

		// setup time windows
		for (PlanAction planAction : possibleTaxiSchedule) {
			timeWindows.add(new TimeWindow(timeAtFirstAction, ((PlanRequestAction) planAction).getMaxTime() * 1000L));
		}

		// for every Node: check if taxi is capable of carrying the passenger or package and whether it's possible to get to the next node
		boolean personOnBoard = allVehicles.get(taxiIndex).isPassengerOnboard();
		String personOnBoardId = "";
		if (personOnBoard) {
			personOnBoardId = ((DemandAgent) allVehicles.get(taxiIndex).getVehicle().getTransportedEntities().get(0)).getId();
		}
		int curFreightWeight = allVehicles.get(taxiIndex).getCurrentPackagesWeight();
		final int taxiMaxCapacity = allVehicles.get(taxiIndex).getMaxPackagesCapacity();
		for (int i = 0; i < possibleTaxiSchedule.size() - 1; i++)   // size-1 ... the last Node of taxi has no following Node to be checked
		{
			// check for sufficient person capacity
			PlanAction action = possibleTaxiSchedule.get(i);
			if (action instanceof PlanActionPickup) {
				// if person is on board, reject
				if (personOnBoard) {
					return;
				}
				PlanActionPickup pickAction = (PlanActionPickup) action;
				PlanComputationRequest pickRequest = pickAction.request;
				// if Package
				// checking sufficient freight capacity
				if (pickRequest instanceof PlanComputationRequestFreight) {
					// if not sufficient freight capacity, reject
					if (((PlanComputationRequestFreight) pickRequest).getWeight() + curFreightWeight > taxiMaxCapacity) {
						return;
					}
					// adding the package onBoard
					curFreightWeight += ((PlanComputationRequestFreight) pickRequest).getWeight();
				}
				// if Agent
				else {
					// adding the person onBoard
					personOnBoard = true;
					personOnBoardId = pickRequest.getDemandEntity().getId();
				}
			}
			else if (action instanceof PlanActionDropoff) {
				PlanActionDropoff dropAction = (PlanActionDropoff) action;
				PlanComputationRequest dropRequest = dropAction.request;

				// if Agent
				if (dropRequest instanceof PlanComputationRequestPeople) {
					if (personOnBoard && (((PlanActionDropoff) action).request).getDemandEntity().getId().equals(personOnBoardId)) {
						// remove the person from the taxi
						personOnBoard = false;
						personOnBoardId = "";
					}
					else {
						return;
					}
				}
				// if Package
				else {
					if (personOnBoard) {
						return;
					}
					else {
						curFreightWeight -= ((PlanComputationRequestFreight) dropRequest).getWeight();
					}
				}
			}
			else {
				throw new NotImplementedException("PeopleFreightHeuristicSolver.trySchedule(): Action is neither pickup nor dropoff");
			}


			TimeWindow currentTimeWindow = timeWindows.get(i);
			long travelTime = travelTimeProvider.getExpectedTravelTime(possibleTaxiSchedule.get(i).getPosition(),
					possibleTaxiSchedule.get(i + 1).getPosition());
			long earlyTime = currentTimeWindow.earlyTime + travelTime;
			long lateTime = currentTimeWindow.lateTime + travelTime;

			// if taxi is getting to the next Node after maxTime of the Node
			if (earlyTime > timeWindows.get(i + 1).lateTime) {
				// not feasible -> terminate
				return;
			}
			timeWindows.set(i + 1, new TimeWindow(Math.max(earlyTime, timeWindows.get(i + 1).earlyTime),
					Math.min(lateTime, timeWindows.get(i + 1).lateTime)) );
		}
		// planDuration = earlyTime of last time window - earlyTime of first time window
		long planDuration = timeWindows.get(timeWindows.size() - 1).earlyTime - timeWindows.get(0).earlyTime;
		double planCost = planCostProvider.calculatePlanCost(planDiscomfort, (int) planDuration / 1000);

		bestSchedule = new ScheduleWithDuration(possibleTaxiSchedule, planDuration, planCost);
	}


	private void logRidesharingStats(List<PlanComputationRequest> requests)
	{
		ridesharingStats.add(new RidesharingBatchStatsPFH(requests.size(), peopleFreightHeuristicTime, 0, 0));
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
