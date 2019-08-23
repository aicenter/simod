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
package cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.tongfei.progressbar.ProgressBar;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class InsertionHeuristicSolver extends DARPSolver implements EventHandler{
	
	private static final int INFO_PERIOD = 1000;

	private final PositionUtil positionUtil;
	
	private final AmodsimConfig config;
	
	private final double maxDistance;
	
	private final double maxDistanceSquared;
	
	private final int maxDelayTime;
	
	private final TimeProvider timeProvider;
	
	private final TypedSimulation eventProcessor;
	
	private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;
	
	
	
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
	
	
	
	
	
	@Inject
	public InsertionHeuristicSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider, 
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
			AmodsimConfig config, TimeProvider timeProvider, DefaultPlanComputationRequestFactory requestFactory,
			TypedSimulation eventProcessor, DroppedDemandsAnalyzer droppedDemandsAnalyzer) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.positionUtil = positionUtil;
		this.config = config;
		this.timeProvider = timeProvider;
		this.eventProcessor = eventProcessor;
		this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
		
		// max distance in meters between vehicle and request for the vehicle to be considered to serve the request
		maxDistance = (double) config.ridesharing.maxProlongationInSeconds 
				* config.vehicleSpeedInMeters;
		maxDistanceSquared = maxDistance * maxDistance;
		
		// the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
		// vehicle to be considered to serve the request
		maxDelayTime = config.ridesharing.maxProlongationInSeconds  * 1000;
		
		setEventHandeling();
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, 
			List<PlanComputationRequest> waitingRequests) {
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
		if(config.ridesharing.insertionHeuristic.recomputeWaitingRequests){
			for(AgentPolisEntity tVvehicle: vehicleStorage.getEntitiesForIteration()){
				RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
				DriverPlan currentPlan = vehicle.getCurrentPlan();
				Iterator<PlanAction> actionIterator = currentPlan.iterator();
				while(actionIterator.hasNext()){
					PlanAction action = actionIterator.next();
					if(action instanceof PlanRequestAction && !((PlanRequestAction) action).request.isOnboard()){
						actionIterator.remove();
					}
				}
				planMap.put(vehicle, currentPlan);
			}
		}
		
		if(requests.size() > 1){
			for(PlanComputationRequest request: ProgressBar.wrap(requests, "Processing new requests")){
				processRequest(request);
			}
		}
		else{
			for(PlanComputationRequest request: requests){
				processRequest(request);
			}
		}
		
		totalTime += System.nanoTime() - startTime;
		if(callCount % InsertionHeuristicSolver.INFO_PERIOD == 0){
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
			sb.append("Traveltime call count: ").append(((EuclideanTravelTimeProvider) travelTimeProvider).getCallCount()).append("\n");
			System.out.println(sb.toString());
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
	
	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request){
		canServeRequestCallCount++;
		
		// do not mess with rebalancing
		if(vehicle.getState() == OnDemandVehicleState.REBALANCING){
			return false;
		}
		
		// node identity
		if(vehicle.getPosition() == request.getFrom()){
			return true;
		}
		
		// euclidean distance check
		double dist_x = vehicle.getPosition().getLatitudeProjected() - request.getFrom().getLatitudeProjected();
		double dist_y = vehicle.getPosition().getLongitudeProjected() - request.getFrom().getLongitudeProjected();
		double distanceSquared = dist_x * dist_x + dist_y * dist_y;
		if(distanceSquared > maxDistanceSquared){
			return false;
		}
		
		// real feasibility check 
		boolean canServe = travelTimeProvider.getTravelTime(vehicle, request.getFrom()) 
				< maxDelayTime;
		
		return canServe;
	}

	private void computeOptimalPlan(RideSharingOnDemandVehicle vehicle, PlanComputationRequest planComputationRequest) {
		DriverPlan currentPlan = vehicle.getCurrentPlan();
		
		// if the plan was already changed
		if(planMap.containsKey(vehicle)){
			currentPlan = planMap.get(vehicle);
		}
		
		int freeCapacity = vehicle.getFreeCapacity();
		
		for(int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength(); pickupOptionIndex++){
			
			// continue if the vehicle is full
			if(freeCapacity == 0){
				continue;
			}
			
			for(int dropoffOptionIndex = pickupOptionIndex + 1; dropoffOptionIndex <= currentPlan.getLength() + 1; 
					dropoffOptionIndex++){
				DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex, 
						vehicle, planComputationRequest);
				if(potentialPlan != null){
					double costIncrement = potentialPlan.cost - currentPlan.cost;
					PlanData bestPlanData = new PlanData(vehicle, currentPlan, costIncrement);
					tryUpdateBestPlan(bestPlanData);
				}
			}
			
			// change free capacity for next index
			if(pickupOptionIndex < currentPlan.getLength()){
				if(currentPlan.plan.get(pickupOptionIndex) instanceof PlanActionPickup){
					freeCapacity--;
				}
				else{
					freeCapacity++;
				}
			}
		}
	}

	
	/**
	 * Returns list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
	 * @param currentPlan Current plan, starting with the current position action
	 * @param pickupOptionIndex Pick up index: 1 - current plan length
	 * @param dropoffOptionIndex Drop off index: 2 - current plan length + 1
	 * @param vehicle
	 * @param planComputationRequest
	 * @return list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
	 */
	private DriverPlan insertIntoPlan(final DriverPlan currentPlan, final int pickupOptionIndex, 
			final int dropoffOptionIndex, final RideSharingOnDemandVehicle vehicle, 
			final PlanComputationRequest planComputationRequest) {
		List<PlanAction> newPlanTasks = new LinkedList<>();
		
		
		// travel time of the new plan in milliseconds
		int newPlanTravelTime = 0;
		
		// discomfort of the new plan in milliseconds
		int newPlanDiscomfort = 0;
		
		PlanAction previousTask = null;
		
		// index of the lastly added action from the old plan (not considering current position action)
		int indexInOldPlan = -1;
		
		Iterator<PlanAction> oldPlanIterator = currentPlan.iterator();
		int freeCapacity = vehicle.getFreeCapacity();
		
		for(int newPlanIndex = 0; newPlanIndex <= currentPlan.getLength() + 1; newPlanIndex++){
			
			/* get new task */
			PlanAction newTask = null;
			if(newPlanIndex == pickupOptionIndex){
				newTask = planComputationRequest.getPickUpAction();
//						new PlanActionPickup(request.getDemandAgent(),  request.getDemandAgent().getPosition());
			}
			else if(newPlanIndex == dropoffOptionIndex){
				newTask = planComputationRequest.getDropOffAction();
//						= new DriverPlanTask(DriverPlanTaskType.DROPOFF, request.getDemandAgent(), 
//						request.getTargetLocation());
			}
			else{
				newTask = oldPlanIterator.next();
			}
			
			// travel time increment
			if(previousTask != null){
				if(previousTask instanceof PlanActionCurrentPosition){
					newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, newTask.getPosition());
				}
				else{
					newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, previousTask.getPosition(), 
							newTask.getPosition());
				}
			}
			long curentTaskTimeInSeconds = (timeProvider.getCurrentSimTime() + newPlanTravelTime) / 1000;
			
			
			/* check max time for all unfinished demands */
			
			int delta = 5;
			
			// check max time check for the new action
			if(newTask instanceof PlanRequestAction){
				int maxTime = ((PlanRequestAction) newTask).getMaxTime();
				if(maxTime < curentTaskTimeInSeconds + delta){
					return null;
				}
			}
			
			// check max time for actions in the current plan
			for(int index = indexInOldPlan + 1; index < currentPlan.getLength(); index++){
				PlanAction remainingAction = currentPlan.plan.get(index);
				if(!(remainingAction instanceof PlanActionCurrentPosition)){
					PlanRequestAction remainingRequestAction = (PlanRequestAction) remainingAction;
					if(remainingRequestAction.getMaxTime() < curentTaskTimeInSeconds + delta){
						return null;
					}
				}
			}
			
			// check max time for pick up action
			if(newPlanIndex <= pickupOptionIndex){
				if(planComputationRequest.getPickUpAction().getMaxTime() < curentTaskTimeInSeconds + delta){
					return null;
				}
			}
			
			// check max time for drop off action
			if(newPlanIndex <= dropoffOptionIndex){
				if(planComputationRequest.getDropOffAction().getMaxTime() < curentTaskTimeInSeconds + delta){
					return null;
				}
			}
			
			
			/* pickup and drop off handeling */
			if(newTask instanceof PlanActionDropoff){
				freeCapacity++;
				
				// discomfort increment
				PlanComputationRequest newRequest = ((PlanActionDropoff) newTask).getRequest();
				long taskExecutionTime = timeProvider.getCurrentSimTime() + newPlanTravelTime;
				newPlanDiscomfort += taskExecutionTime - newRequest.getOriginTime() * 1000 
						- newRequest.getMinTravelTime() * 1000;
			}
			else if(newTask instanceof PlanActionPickup){
				// capacity check
				if(freeCapacity == 0){
					return null;
				}
				freeCapacity--;
			}

			
			// index in old plan if the action was not new
			if(newPlanIndex != pickupOptionIndex && newPlanIndex != dropoffOptionIndex){
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
				requests.size()));
	}

	private void computeBestPlanForRequest(PlanComputationRequest request) {
		minCostIncrement = Double.MAX_VALUE;
		bestPlan = null;

		long iterationStartTime = System.nanoTime();

		vehicleStorage.stream().parallel().forEach((tVvehicle) -> processRequestVehicleCombination(request, tVvehicle));

		iterationTime += System.nanoTime() - iterationStartTime;
	}
	
	private void processRequestVehicleCombination(PlanComputationRequest request, AgentPolisEntity tVvehicle){
		RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;

		// fail fast
		if(canServeRequest(vehicle, request)){
			computeOptimalPlan(vehicle, request);
		}
	}
	
	private synchronized void tryUpdateBestPlan(PlanData newPlanData){
		vehiclePlanningAllCallCount++;
		
		if(newPlanData != null && newPlanData.increment < minCostIncrement){
			minCostIncrement = newPlanData.increment;
			bestPlan = newPlanData;
		}
	}

	private void processRequest(PlanComputationRequest request) {
		Benchmark benchmark = new Benchmark();
		benchmark.measureTime(() -> computeBestPlanForRequest(request));
		insertionHeuristicTime += benchmark.getDurationMsInt();

		if(bestPlan != null){
			planMap.put(bestPlan.vehicle, bestPlan.plan);
		}
		else{
			benchmark = new Benchmark();
			benchmark.measureTime(() ->	droppedDemandsAnalyzer.debugFail(request));
			debugFailTime += benchmark.getDurationMs();
		}
	}
	
	private class PlanData{
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
