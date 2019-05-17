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
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	
	private List<DefaultPlanComputationRequest> planComputationRequests;
	
	private final Map<Integer,DefaultPlanComputationRequest> requestsMapByDemandAgents;
	
	
	
	private long callCount = 0;
	
	private long totalTime = 0;
	
	private long iterationTime = 0;
	
	private long canServeRequestCallCount = 0;
	
	private long vehiclePlanningAllCallCount = 0;
	
	private Map<RideSharingOnDemandVehicle, DriverPlan> planMap;
	
	private int failFastTime;
	
	private int insertionHeuristicTime;
	
	private int debugFailTime;
	
	
	
	
	
	@Inject
	public InsertionHeuristicSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider, 
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
			AmodsimConfig config, TimeProvider timeProvider, DefaultPlanComputationRequestFactory requestFactory,
			TypedSimulation eventProcessor) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.positionUtil = positionUtil;
		this.config = config;
		this.timeProvider = timeProvider;
		this.eventProcessor = eventProcessor;
		
		planComputationRequests = new ArrayList<>();
		requestsMapByDemandAgents = new HashMap<>();
		
		// max distance in meters between vehicle and request for the vehicle to be considered to serve the request
		maxDistance = (double) config.ridesharing.maxProlongationInSeconds 
				* config.ridesharing.maxDirectSpeedEstimationKmh / 3600 * 1000;
		maxDistanceSquared = maxDistance * maxDistance;
		
		// the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
		// vehicle to be considered to serve the request
		maxDelayTime = config.ridesharing.maxProlongationInSeconds  * 1000;
		
		setEventHandeling();
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(final List<OnDemandRequest> requests) {
		callCount++;
		long startTime = System.nanoTime();
		
		planMap = new HashMap<>();
		
		// statistics
		failFastTime = 0;
		insertionHeuristicTime = 0;
		debugFailTime = 0;
		
		// dicsard requests from previous batch if recomputing is off
		if(!config.ridesharing.insertionHeuristic.recomputeWaitingRequests){
			planComputationRequests = new ArrayList<>();
		}
		
		for(OnDemandRequest request: requests){
			// plan request creation
			DefaultPlanComputationRequest planComputationRequest = requestFactory.create(0, request.getPosition(), 
				request.getTargetLocation(), request.getDemandAgent());
			planComputationRequests.add(planComputationRequest);
			
			if(config.ridesharing.insertionHeuristic.recomputeWaitingRequests){
				requestsMapByDemandAgents.put(request.getDemandAgent().getSimpleId(), planComputationRequest);
			}
		}
		
		// discard current plans
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
		
		if(planComputationRequests.size() > 1){
			for(DefaultPlanComputationRequest request: ProgressBar.wrap(planComputationRequests, "Processing new requests")){
				processRequest(request);
			}
		}
		else{
			for(DefaultPlanComputationRequest request: planComputationRequests){
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
		
		logRidesharingStats(requests);
		
		return planMap;
	}
	
	@Override
	public void handleEvent(Event event) {
		if(config.ridesharing.insertionHeuristic.recomputeWaitingRequests){
			OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
			DefaultPlanComputationRequest request = requestsMapByDemandAgents.get(eventContent.getDemandId());
			request.setOnboard(true);
			if(!planComputationRequests.remove(request)){
				try {
					throw new Exception("Request picked up twice");
				} catch (Exception ex) {
					Logger.getLogger(InsertionHeuristicSolver.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
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
	
	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, DefaultPlanComputationRequest request){
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
		// TODO compute from interpolated position
		boolean canServe = travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getFrom()) 
				< maxDelayTime;
		
		return canServe;
	}

	private PlanData getOptimalPlan(RideSharingOnDemandVehicle vehicle, PlanComputationRequest planComputationRequest) {
		
		double minCostIncrement = Double.MAX_VALUE;
		DriverPlan bestPlan = null;
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
					if(costIncrement < minCostIncrement){
						minCostIncrement = costIncrement;
						bestPlan = potentialPlan;
					}
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
		return new PlanData(vehicle, bestPlan, minCostIncrement);
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
				newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, previousTask.getPosition(), 
							newTask.getPosition());
			}
			
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
			
			/* check max time for all unfinished demands */
			long curentTaskTimeInSeconds = (timeProvider.getCurrentSimTime() + newPlanTravelTime) / 1000;
			
			// check max time for actions in the current plan
			for(int index = indexInOldPlan + 1; index < currentPlan.getLength(); index++){
				PlanAction remainingAction = currentPlan.plan.get(index);
				if(!(remainingAction instanceof PlanActionCurrentPosition)){
					PlanRequestAction remainingRequestAction = (PlanRequestAction) remainingAction;
					if(remainingRequestAction.getMaxTime() < curentTaskTimeInSeconds){
						return null;
					}
				}
			}
			
			// check max time for pick up action
			if(newPlanIndex <= pickupOptionIndex){
				if(planComputationRequest.getPickUpAction().getMaxTime() < curentTaskTimeInSeconds){
					return null;
				}
			}
			
			// check max time for drop off action
			if(newPlanIndex <= dropoffOptionIndex){
				if(planComputationRequest.getDropOffAction().getMaxTime() < curentTaskTimeInSeconds){
					return null;
				}
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

	private void debugFail(DefaultPlanComputationRequest request) {
		boolean freeVehicle = false;
		double bestCartesianDistance = Double.MAX_VALUE;
		double bestTravelTimne = Double.MAX_VALUE;
		
		for(OnDemandVehicle tVvehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
			if(vehicle.hasFreeCapacity()){
				freeVehicle = true;
				
				// cartesian distance check
				double distance = positionUtil.getPosition(vehicle.getPosition())
						.distance(positionUtil.getPosition(request.getFrom()));

				if(distance < bestCartesianDistance){
					bestCartesianDistance = distance;
				}
				
				
				if(distance < maxDistance){
						// real feasibility check 
					// TODO compute from interpolated position
					double travelTime = 
							travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getFrom());


					if(travelTime < bestTravelTimne){
						bestTravelTimne = travelTime;
					}
				}
			}
		}	
		String requestId = request.getDemandAgent().getId();
		if(!freeVehicle){
			System.out.println("Request " + requestId + ": Cannot serve request - No free vehicle");
		}
		else if(bestCartesianDistance > maxDistance){
			System.out.println("Request " + requestId + ": Cannot serve request - Too big distance: " + bestCartesianDistance + "m (max distance: " + maxDistance + ")");
		}
		else if(bestTravelTimne > maxDelayTime){
			System.out.println("Request " + requestId + ": Cannot serve request - Too big traveltime to startLoaction: " + bestTravelTimne);
		}
		else{
			System.out.println("Request " + requestId + ": Cannot serve request - Some other problem - all nearby vehicle plans infeasible?");
		}
	}

	private String readableTime(long nanoTime) {
		long milisTotal = (long) nanoTime / 1000000;
		long millis = milisTotal % 1000;
		long second = (milisTotal / 1000) % 60;
		long minute = (milisTotal / (1000 * 60)) % 60;
		long hour = (milisTotal / (1000 * 60 * 60)) % 24;
		
		return String.format(" (%02d:%02d:%02d:%d)", hour, minute, second, millis);
	}

	private void logRidesharingStats(List<OnDemandRequest> requests) {
		ridesharingStats.add(new RidesharingBatchStatsIH(failFastTime, insertionHeuristicTime, debugFailTime, 
				requests.size()));
	}

	private PlanData computeBestPlanForRequest(DefaultPlanComputationRequest request) {
		double minCostIncrement = Double.MAX_VALUE;
		PlanData bestPlan = null;
		RideSharingOnDemandVehicle servingVehicle = null;

		long iterationStartTime = System.nanoTime();

		

		for(AgentPolisEntity tVvehicle: vehicleStorage.getEntitiesForIteration()){

			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;

			// fail fast
			if(canServeRequest(vehicle, request)){
				vehiclePlanningAllCallCount++;

				PlanData newPlanData = getOptimalPlan(vehicle, request);
				if(newPlanData != null && newPlanData.increment < minCostIncrement){
					minCostIncrement = newPlanData.increment;
					bestPlan = newPlanData;
					servingVehicle = vehicle;
				}
			}
		}

		iterationTime += System.nanoTime() - iterationStartTime;
		
		return bestPlan;
	}

	private void processRequest(DefaultPlanComputationRequest request) {
		PlanData bestPlanData = Benchmark.measureTime(() -> computeBestPlanForRequest(request));
		insertionHeuristicTime += Benchmark.getDurationMsInt();

		if(bestPlanData != null){
			planMap.put(bestPlanData.vehicle, bestPlanData.plan);
		}
		else{
			Benchmark.measureTime(() ->	debugFail(request));
			debugFailTime += Benchmark.getDurationMs();
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
