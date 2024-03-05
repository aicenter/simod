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
package cz.cvut.fel.aic.simod.ridesharing.insertionheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.SimpleTransportVehicle;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.SimpleMoDVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author F.I.D.O.
 */
@Singleton
public class InsertionHeuristicSolver<T> extends DARPSolver implements EventHandler {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InsertionHeuristicSolver.class);

	private static final int INFO_PERIOD = 1000;

	private final PositionUtil positionUtil;

	private final SimodConfig config;

	private final double maxDistance;

	private final double maxDistanceSquared;

	private final int maxDelayTime;

	private final TimeProvider timeProvider;

	private final TypedSimulation eventProcessor;

	private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;

	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;


	private long callCount = 0;

	private long totalTime = 0;

	private long iterationTime = 0;

	private volatile long canServeRequestCallCount = 0;

	private long vehiclePlanningAllCallCount = 0;

	private Map<RideSharingOnDemandVehicle, DriverPlan> planMap;

	private int failFastTime;

	private int insertionHeuristicTime;

	private int debugFailTime;

	private double minCostIncrement;

	private PlanData bestPlan;

	OnDemandVehicle vehicleFromNearestStation;

//	protected int freeCapacity;

	/**
	 * Used vehicles per station in the current iteration of the solver
	 */
	private int[] usedVehiclesPerStation;

	private List<OnDemandVehicle> vehiclesForPlanning;


	@Inject
	public InsertionHeuristicSolver(
		TravelTimeProvider travelTimeProvider,
		PlanCostProvider travelCostProvider,
		OnDemandVehicleStorage vehicleStorage,
		PositionUtil positionUtil,
		SimodConfig config,
		TimeProvider timeProvider,
		DefaultPlanComputationRequestFactory requestFactory,
		TypedSimulation eventProcessor,
		DroppedDemandsAnalyzer droppedDemandsAnalyzer,
		OnDemandvehicleStationStorage onDemandvehicleStationStorage,
		AgentpolisConfig agentpolisConfig
	) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.positionUtil = positionUtil;
		this.config = config;
		this.timeProvider = timeProvider;
		this.eventProcessor = eventProcessor;
		this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;


		// max distance in meters between vehicle and request for the vehicle to be considered to serve the request
		maxDistance = (double) config.ridesharing.maxProlongationInSeconds
			* agentpolisConfig.maxVehicleSpeedInMeters;
		maxDistanceSquared = maxDistance * maxDistance;

		// the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
		// vehicle to be considered to serve the request
		maxDelayTime = config.ridesharing.maxProlongationInSeconds * 1000;

		setEventHandeling();
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(
		List<PlanComputationRequest> newRequests,
		List<PlanComputationRequest> waitingRequests
	) {
		callCount++;
		long startTime = System.nanoTime();

		planMap = new ConcurrentHashMap<>();

		// statistics
		failFastTime = 0;
		insertionHeuristicTime = 0;
		debugFailTime = 0;

		List<PlanComputationRequest> requests = config.ridesharing.insertionHeuristic.recomputeWaitingRequests
			? waitingRequests : newRequests;

		// discard current plans if the recomputing is on
		if (config.ridesharing.insertionHeuristic.recomputeWaitingRequests) {
			for (AgentPolisEntity tVvehicle : vehicleStorage.getEntitiesForIteration()) {
				RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
				DriverPlan currentPlan = vehicle.getCurrentPlan();
				Iterator<PlanAction> actionIterator = currentPlan.iterator();
				while (actionIterator.hasNext()) {
					PlanAction action = actionIterator.next();
					if (action instanceof PlanRequestAction && !((PlanRequestAction) action).request.isOnboard()) {
						actionIterator.remove();
					}
				}
				planMap.put(vehicle, currentPlan);
			}
		}

//		List<OnDemandVehicle> vehiclesForPlanning = getDrivingVehicles();
		usedVehiclesPerStation = new int[onDemandvehicleStationStorage.size()];

		getVehiclesForPlanning();

		if (requests.size() > 10) {
			for (PlanComputationRequest request : ProgressBar.wrap(requests, "Processing new requests")) {
				processRequest(request);
			}
		} else {
			for (PlanComputationRequest request : requests) {
				processRequest(request);
			}
		}

		totalTime += System.nanoTime() - startTime;
		if (callCount % InsertionHeuristicSolver.INFO_PERIOD == 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("Insetrion Heuristic Stats: \n");
			sb.append("Real time: ").append(System.nanoTime()).append(readableTime(System.nanoTime())).append("\n");
			sb.append("Simulation time: ").append(timeProvider.getCurrentSimTime())
				.append(readableTime(timeProvider.getCurrentSimTime() * 1000000)).append("\n");
			sb.append("Call count: ").append(callCount).append("\n");
			sb.append("Total time: ").append(totalTime).append(readableTime(totalTime)).append("\n");
			sb.append("Iteration time: ").append(iterationTime).append(readableTime(iterationTime)).append("\n");
			sb.append("Can serve call count: ").append(canServeRequestCallCount).append("\n");
			sb.append("Vehicle planning call count: ").append(vehiclePlanningAllCallCount).append("\n");
			sb.append("Traveltime call count: ").append((travelTimeProvider).getCallCount()).append("\n");
			LOGGER.info(sb.toString());
		}

		logRidesharingStats(newRequests);

		return planMap;
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

	private void computeOptimalPlan(RideSharingOnDemandVehicle vehicle, PlanComputationRequest planComputationRequest) {
		computeOptimalPlan(vehicle, vehicle.getCurrentPlan(), planComputationRequest);
	}

	/**
	 * Main method for computing the optimal plan for a given request-vehicle combination. It iterates over all possible
	 * pickup-dropoff combinations and tries to insert the request into the current plan. To actually try the insertion,
	 * the insertIntoPlan method is called.
	 * @param vehicle the vehicle to process
	 * @param currentPlan the current plan of the vehicle
	 * @param planComputationRequest the request to process
	 */
	private void computeOptimalPlan(
		RideSharingOnDemandVehicle vehicle,
		DriverPlan currentPlan,
		PlanComputationRequest planComputationRequest
	) {
		T counter = initFreeCapacityForRequest(vehicle, planComputationRequest);

		for (int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength(); pickupOptionIndex++) {

			// continue if the vehicle is full. The capacity is checked inside the insertIntoPlan method, so this is
			// just a cut-off
			if (!hasCapacityForRequest(planComputationRequest, counter)) {
				continue;
			}

			for (int dropoffOptionIndex = pickupOptionIndex + 1; dropoffOptionIndex <= currentPlan.getLength() + 1;
				 dropoffOptionIndex++) {
				DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex,
					vehicle, planComputationRequest
				);
				if (potentialPlan != null) {
					double costIncrement = potentialPlan.cost - currentPlan.cost;
					PlanData bestPlanData = new PlanData(vehicle, potentialPlan, costIncrement);
					tryUpdateBestPlan(bestPlanData);
				}
			}

			// change free capacity for next index
			adjustFreeCapacity(currentPlan, pickupOptionIndex, planComputationRequest, counter);
		}
	}

	protected boolean hasCapacityForRequest(PlanComputationRequest planComputationRequest, T counter) {
		int counterInt = (Integer) counter;
		return counterInt > 0;
	}

	protected T adjustFreeCapacity(
		DriverPlan currentPlan, int evaluatedIndex, PlanComputationRequest planComputationRequest, T counter
	) {
		int counterInt = (int) counter;
		if (evaluatedIndex < currentPlan.getLength()) { // no need to adjust for the last index

			if (currentPlan.plan.get(evaluatedIndex) instanceof PlanActionPickup) {
				counterInt--;
			} else {
				counterInt++;
			}
		}
		return (T) (Integer.valueOf(counterInt));
	}

	protected T initFreeCapacityForRequest(
		RideSharingOnDemandVehicle vehicle,
		PlanComputationRequest planComputationRequest
	) {
		SimpleMoDVehicle simpleVehicle = (SimpleMoDVehicle) vehicle.getVehicle();
		return (T) Integer.valueOf(simpleVehicle.getFreeCapacity());
	}


	/**
	 * Returns list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
	 *
	 * @param currentPlan            Current plan, starting with the current position action
	 * @param pickupOptionIndex      Pick up index: 1 - current plan length
	 * @param dropoffOptionIndex     Drop off index: 2 - current plan length + 1
	 * @param vehicle
	 * @param planComputationRequest
	 * @return list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
	 */
	private DriverPlan insertIntoPlan(
		final DriverPlan currentPlan,
		final int pickupOptionIndex,
		final int dropoffOptionIndex,
		final RideSharingOnDemandVehicle vehicle,
		final PlanComputationRequest planComputationRequest
	) {
		List<PlanAction> newPlanTasks = new LinkedList<>();

		// travel time of the new plan in milliseconds
		int newPlanTravelTime = 0;

		// discomfort of the new plan in milliseconds
		int newPlanDiscomfort = 0;

		// index of the lastly added action from the old plan
		int indexInOldPlan = 0;

		Iterator<PlanAction> oldPlanIterator = currentPlan.iterator();

		// process the current position action
		PlanAction previousTask = oldPlanIterator.next(); // current position action
		newPlanTasks.add(previousTask);

		// free capacity counter
		T counter = initFreeCapacityForRequest(vehicle, planComputationRequest);

		// we start computing the time current time (we cannot plan for the past :))
		long currentTaskTimeInSeconds = (timeProvider.getCurrentSimTime() + newPlanTravelTime) / 1000;

		for (int newPlanIndex = 1; newPlanIndex <= currentPlan.getLength() + 1; newPlanIndex++) {

			/* get new task */
			PlanRequestAction newTask = null;
			if (newPlanIndex == pickupOptionIndex) {
				newTask = planComputationRequest.getPickUpAction();
			} else if (newPlanIndex == dropoffOptionIndex) {
				newTask = planComputationRequest.getDropOffAction();
			} else {
				newTask = (PlanRequestAction) oldPlanIterator.next();
			}

			// current time adjustment according to the new task min time
			if(newTask instanceof PlanActionPickup){
				var minTime = ((PlanActionPickup) newTask).getMinTime();
				if (minTime > currentTaskTimeInSeconds) {
					currentTaskTimeInSeconds = minTime;
				}
			}

			// travel time increment
			int travelTime;
			if (previousTask instanceof PlanActionCurrentPosition) {
		 		travelTime = (int) travelTimeProvider.getTravelTime(vehicle, newTask.getPosition()) / 1000;
			} else {
				travelTime = (int) travelTimeProvider.getTravelTime(vehicle, previousTask.getPosition(),
					newTask.getPosition()
				) / 1000;
			}
			newPlanTravelTime += travelTime;
			currentTaskTimeInSeconds += travelTime;
//			LOGGER.debug("currentTaskTimeInSeconds: {}", currentTaskTimeInSeconds);

			/* check max time for all unfinished demands */

			// check max time check for the new action
			int maxTime = newTask.getMaxTime();
			if (maxTime < currentTaskTimeInSeconds) {
//                                    LOGGER.debug("currentTaskTimeInSeconds {} \n> maxTime {}",currentTaskTimeInSeconds, maxTime);
				return null;
			}

			// check max time for actions in the current plan
			for (int index = indexInOldPlan + 1; index < currentPlan.getLength(); index++) {
				PlanRequestAction remainingRequestAction = (PlanRequestAction) currentPlan.plan.get(index);
				if (remainingRequestAction.getMaxTime() < currentTaskTimeInSeconds) {
					return null;
				}
			}

			// check max time for pick up action
			if (newPlanIndex <= pickupOptionIndex) {
				if (planComputationRequest.getPickUpAction().getMaxTime() < currentTaskTimeInSeconds) {
					return null;
				}
			}

			// check max time for drop off action
			if (newPlanIndex <= dropoffOptionIndex) {
				if (planComputationRequest.getDropOffAction().getMaxTime() < currentTaskTimeInSeconds) {
					return null;
				}
			}

			/* pickup and drop off handeling */
			if (newTask instanceof PlanActionDropoff) {
				// discomfort increment
				PlanComputationRequest newRequest = newTask.getRequest();
				long taskExecutionTime = timeProvider.getCurrentSimTime() + newPlanTravelTime;
				newPlanDiscomfort += taskExecutionTime - newRequest.getMinTime() * 1000
					- newRequest.getMinTravelTime() * 1000;
			} else if (newTask instanceof PlanActionPickup) {
				// capacity check
				if (!hasCapacityForRequest(planComputationRequest, counter)) {
					return null;
				}
			}

			adjustFreeCapacity(currentPlan, newPlanIndex, planComputationRequest, counter);

			// index in old plan if the action was not new
			if (newPlanIndex != pickupOptionIndex && newPlanIndex != dropoffOptionIndex) {
				indexInOldPlan++;
			}

			newPlanTasks.add(newTask);
			previousTask = newTask;
		}

		// cost computation
		double newPlanCost = planCostProvider.calculatePlanCost(newPlanDiscomfort, newPlanTravelTime);

		return new DriverPlan(newPlanTasks, newPlanTravelTime, newPlanCost);
	}

	private String readableTime(long nanoTime) {
		long milisTotal = (long) nanoTime / 1000000;
		long millis = milisTotal % 1000;
		long second = (milisTotal / 1000) % 60;
		long minute = (milisTotal / (1000 * 60)) % 60;
		long hour = (milisTotal / (1000 * 60 * 60)) % 24;

		return String.format(" (%02d:%02d:%02d:%d)", hour, minute, second, millis);
	}

	private void logRidesharingStats(List<PlanComputationRequest> requests) {
		ridesharingStats.add(new RidesharingBatchStatsIH(failFastTime, insertionHeuristicTime, debugFailTime,
			requests.size()
		));
	}

	/**
	 * This method first prepares the list of vehicles for which the insertions will be computed and then calls the
	 * processRequestVehicleCombination method for each vehicle and request combination.
	 * @param request the request to process
	 */
	private void computeBestPlanForRequest(PlanComputationRequest request) {
		resetBestPlan();

		// in case of station system, add one vehicle from the nearest station
		if (config.stations.on) { //!onDemandvehicleStationStorage.isEmpty()){ //
			OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(
				request.getFrom(),
				OnDemandvehicleStationStorage.NearestType.TRAVELTIME_FROM
			);
			if (nearestStation != null) {
				int indexFromEnd = usedVehiclesPerStation[Integer.parseInt(nearestStation.getId())];
				int index = nearestStation.getParkedVehiclesCount() - 1 - indexFromEnd;

				if (index >= 0) {
					vehicleFromNearestStation = nearestStation.getVehicle(index);
					vehiclesForPlanning.add(vehicleFromNearestStation);
				} else {
					LOGGER.warn("Nearest station {} empty for request {}", nearestStation, request);
				}
			} else {
				LOGGER.warn("All stations empty for request {}", request);
			}
		}


		long iterationStartTime = System.nanoTime();

		vehiclesForPlanning.stream().parallel().forEach((tVvehicle) -> {
			processRequestVehicleCombination(request, tVvehicle);
		});

		iterationTime += System.nanoTime() - iterationStartTime;
	}

	/**
	 * Wrapper method for request-vehicle combination processing. It select the current vehicle plan and calls the
	 * tryToAddRequestToPlan method.
	 * @param request the request to process
	 * @param tVvehicle the vehicle to process
	 */
	private void processRequestVehicleCombination(PlanComputationRequest request, AgentPolisEntity tVvehicle) {
		RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;

		DriverPlan currentPlan = vehicle.getCurrentPlan();

		// if the plan was already changed
		if (planMap.containsKey(vehicle)) {
			currentPlan = planMap.get(vehicle);
		}

		tryToAddRequestToPlan(request, vehicle, currentPlan);
	}

	/**
	 * Given a request, a vehicle and its current plan, this method tries to fail fast and if the request can be served,
	 * it calls the computeOptimalPlan method.
	 * @param request the request to process
	 * @param vehicle the vehicle to process
	 * @param plan the plan to process
	 */
	public void tryToAddRequestToPlan(
		PlanComputationRequest request,
		RideSharingOnDemandVehicle vehicle,
		DriverPlan plan
	) {
		// fail fast
		if (canServeRequest(vehicle, request)) {
			computeOptimalPlan(vehicle, plan, request);
		}
	}

	private synchronized void tryUpdateBestPlan(PlanData newPlanData) {
		vehiclePlanningAllCallCount++;

		if (newPlanData != null && newPlanData.increment < minCostIncrement) {
			minCostIncrement = newPlanData.increment;
			bestPlan = newPlanData;
		}
	}

	/**
	 * Process a single request and update the best plan if a better plan is found.
	 * @param request the request to process
	 */
	private void processRequest(PlanComputationRequest request) {
		Benchmark benchmark = new Benchmark();
		vehicleFromNearestStation = null;
		benchmark.measureTime(() -> computeBestPlanForRequest(request));
		insertionHeuristicTime += benchmark.getDurationMsInt();

		if (bestPlan != null) {
			planMap.put(bestPlan.vehicle, bestPlan.plan);

			if (bestPlan.vehicle == vehicleFromNearestStation) {
				usedVehiclesPerStation[Integer.parseInt(vehicleFromNearestStation.getParkedIn().getId())]++;
			}
			// remove nearest vehicle if not used
			else {
				vehiclesForPlanning.remove(vehicleFromNearestStation);
			}
		} else {
			LOGGER.debug("Request {} cannot be served!", request);
			benchmark = new Benchmark();
			benchmark.measureTime(() -> droppedDemandsAnalyzer.debugFail(request, usedVehiclesPerStation));
			debugFailTime += benchmark.getDurationMs();
		}
	}

//	private List<OnDemandVehicle> getDrivingVehicles() {
//		List<OnDemandVehicle> listForPlanning = new ArrayList<>();
//		for(OnDemandVehicle vehicle: vehicleStorage){
//			if(vehicle.getState() != OnDemandVehicleState.REBALANCING){
//				listForPlanning.add(vehicle);
//			}
//		}
//		return listForPlanning;
//	}

	private List<OnDemandVehicle> getVehiclesForPlanning() {
		vehiclesForPlanning = new ArrayList<>();
		for (OnDemandVehicle vehicle : vehicleStorage) {
			if (vehicle.getState() != OnDemandVehicleState.REBALANCING
				&& (!config.stations.on || vehicle.getState() != OnDemandVehicleState.WAITING)) {
				vehiclesForPlanning.add(vehicle);
			}
		}

		return vehiclesForPlanning;
	}

	public void resetBestPlan() {
		minCostIncrement = Double.MAX_VALUE;
		bestPlan = null;
	}

	public DriverPlan getBestPlan() {
		if (bestPlan == null) {
			return null;
		}
		return bestPlan.plan;
	}

	private class PlanData {
		final DriverPlan plan;

		final double increment;

		final RideSharingOnDemandVehicle vehicle;

		public PlanData(RideSharingOnDemandVehicle vehicle, DriverPlan plan, double increment) {
			this.vehicle = vehicle;
			this.plan = plan;
			this.increment = increment;
		}
	}

}
