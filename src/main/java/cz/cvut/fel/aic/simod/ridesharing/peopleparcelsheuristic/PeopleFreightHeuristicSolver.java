package cz.cvut.fel.aic.simod.ridesharing.peopleparcelsheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.*;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;


@Singleton
abstract class PeopleFreightHeuristicSolver extends DARPSolverPFShared implements EventHandler
{

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PeopleFreightHeuristicSolver.class);

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

    private Map<RideSharingOnDemandVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> planMap;

    private int failFastTime;

    private int insertionHeuristicTime;

    private int debugFailTime;

    private double minCostIncrement;

    private PlanData bestPlan;

    OnDemandVehicle vehicleFromNearestStation;

    private int[] usedVehiclesPerStation;

    private List<OnDemandVehicle> vehiclesForPlanning;


    @Inject
    public PeopleFreightHeuristicSolver(
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
            AgentpolisConfig agentpolisConfig)
    {
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

//    @Override
    public Map<RideSharingOnDemandVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solve(List<PlanComputationRequestPeople> newRequestsPeople,
                                                                                                                  List<PlanComputationRequestPeople> waitingRequestsPeople,
                                                                                                                  List<PlanComputationRequestFreight> newRequestsFreight,
                                                                                                                  List<PlanComputationRequestFreight> waitingRequestsFreight)
    {
        // sortedRequestsList = sort pickup requests (V_fo U V_po) incrementally by time windows

        // for i in sortedRequestList:
            // update all status of taxis K and parking places by time e_i
            // availableTaxis = findAvailableTaxis()
            // f_opt = best total benefit, if request i is served
            // k_opt = taxi to serve request i to get the best total benefit

            // if availableTaxis.size() > 0:
                // f_opt = -inf
                // for k_i in availableTaxis:
                    // f_k_i = new total benefit if taxi k serves request i
                    // if f_k_i > f_opt:
                        // f_opt = f_k_i
                        // k_opt = k_i
                // k_opt.route.add(request i)
                // update total benefit
            // else:
                // reject request i
        return null;
    }

//

    // sorts given requests by their time windows
//    private sortRequests()

    // finds best taxi to serve request i
//    public findBestTaxi(List<Taxi> availableTaxis, Request request_i)

    // finds nearest parking
//    public Node findBestParking(List<Node> parkings, first_stop, second_stop)

    // calculates new benefit
//    public calcNewBenefit()

    private boolean canServeRequest(RideSharingOnDemandVehicle vehicle)
    {
        // return true, if vehicle has no passenger onboard
        return true;
    }

    @Override
    public void handleEvent(Event event)
    {

    }

    @Override
    public EventProcessor getEventProcessor()
    {
        return eventProcessor;
    }


    private void setEventHandeling()
    {
        List<Enum> typesToHandle = new LinkedList<>();
        typesToHandle.add(OnDemandVehicleEvent.PICKUP);
        eventProcessor.addEventHandler(this, typesToHandle);
    }


//    private void computeOptimalPlan(RideSharingOnDemandVehicle vehicle, PlanComputationRequest planComputationRequest)
//    {
//        computeOptimalPlan(vehicle, vehicle.getCurrentPlan(), planComputationRequest);
//    }

//    private void computeOptimalPlan(RideSharingOnDemandVehicle vehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan currentPlan, PlanComputationRequest planComputationRequest)
//    {
//
//        int freeCapacity = vehicle.getFreeCapacity();
//
//        for (int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength(); pickupOptionIndex++)
//        {
//
//            // continue if the vehicle is full
//            if (freeCapacity == 0)
//            {
//                continue;
//            }
//
//            for (int dropoffOptionIndex = pickupOptionIndex + 1; dropoffOptionIndex <= currentPlan.getLength() + 1;
//                 dropoffOptionIndex++)
//            {
//                cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex,
//                        vehicle, planComputationRequest);
//                if (potentialPlan != null)
//                {
//                    double costIncrement = potentialPlan.cost - currentPlan.cost;
//                    PlanData bestPlanData = new PlanData(vehicle, potentialPlan, costIncrement);
//                    tryUpdateBestPlan(bestPlanData);
//                }
//            }
//
//            // change free capacity for next index
//            if (pickupOptionIndex < currentPlan.getLength())
//            {
//                if (currentPlan.plan.get(pickupOptionIndex) instanceof PlanActionPickup)
//                {
//                    freeCapacity--;
//                }
//                else
//                {
//                    freeCapacity++;
//                }
//            }
//        }
//    }


//    /**
//     * Returns list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
//     *
//     * @param currentPlan            Current plan, starting with the current position action
//     * @param pickupOptionIndex      Pick up index: 1 - current plan length
//     * @param dropoffOptionIndex     Drop off index: 2 - current plan length + 1
//     * @param vehicle
//     * @param planComputationRequest
//     * @return list of plan tasks with new request actions added at specified indexes or null if the plan is infeasible.
//     */
//    private cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan insertIntoPlan(final cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan currentPlan, final int pickupOptionIndex,
//                                      final int dropoffOptionIndex, final RideSharingOnDemandVehicle vehicle,
//                                      final PlanComputationRequest planComputationRequest)
//    {
//
//        List<PlanAction> newPlanTasks = new LinkedList<>();
//
//
//        // travel time of the new plan in milliseconds
//        int newPlanTravelTime = 0;
//
//        // discomfort of the new plan in milliseconds
//        int newPlanDiscomfort = 0;
//
//        PlanAction previousTask = null;
//
//        // index of the lastly added action from the old plan (not considering current position action)
//        int indexInOldPlan = -1;
//
//        Iterator<PlanAction> oldPlanIterator = currentPlan.iterator();
//        int freeCapacity = vehicle.getFreeCapacity();
//
//        for (int newPlanIndex = 0; newPlanIndex <= currentPlan.getLength() + 1; newPlanIndex++)
//        {
//
//            /* get new task */
//            PlanAction newTask = null;
//            if (newPlanIndex == pickupOptionIndex)
//            {
//                newTask = planComputationRequest.getPickUpAction();
////						new PlanActionPickup(request.getDemandAgent(),  request.getDemandAgent().getPosition());
//            }
//            else if (newPlanIndex == dropoffOptionIndex)
//            {
//                newTask = planComputationRequest.getDropOffAction();
////						= new DriverPlanTask(DriverPlanTaskType.DROPOFF, request.getDemandAgent(),
////						request.getTargetLocation());
//            }
//            else
//            {
//                newTask = oldPlanIterator.next();
//            }
//
//            // travel time increment
//            if (previousTask != null)
//            {
//                if (previousTask instanceof PlanActionCurrentPosition)
//                {
//                    newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, newTask.getPosition());
//                }
//                else
//                {
//                    newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, previousTask.getPosition(),
//                            newTask.getPosition());
//                }
//            }
//            long currentTaskTimeInSeconds = (timeProvider.getCurrentSimTime() + newPlanTravelTime) / 1000;
////			LOGGER.debug("currentTaskTimeInSeconds: {}", currentTaskTimeInSeconds);
//
//            /* check max time for all unfinished demands */
//
//            // check max time check for the new action
//            if (newTask instanceof PlanRequestAction)
//            {
//                int maxTime = ((PlanRequestAction) newTask).getMaxTime();
//                if (maxTime < currentTaskTimeInSeconds)
//                {
////                                    LOGGER.debug("currentTaskTimeInSeconds {} \n> maxTime {}",currentTaskTimeInSeconds, maxTime);
//                    return null;
//                }
//            }
//
//            // check max time for actions in the current plan
//            for (int index = indexInOldPlan + 1; index < currentPlan.getLength(); index++)
//            {
//                PlanAction remainingAction = currentPlan.plan.get(index);
//                if (!(remainingAction instanceof PlanActionCurrentPosition))
//                {
//                    PlanRequestAction remainingRequestAction = (PlanRequestAction) remainingAction;
//                    if (remainingRequestAction.getMaxTime() < currentTaskTimeInSeconds)
//                    {
//                        return null;
//                    }
//                }
//            }
//
//            // check max time for pick up action
//            if (newPlanIndex <= pickupOptionIndex)
//            {
//                if (planComputationRequest.getPickUpAction().getMaxTime() < currentTaskTimeInSeconds)
//                {
//                    return null;
//                }
//            }
//
//            // check max time for drop off action
//            if (newPlanIndex <= dropoffOptionIndex)
//            {
//                if (planComputationRequest.getDropOffAction().getMaxTime() < currentTaskTimeInSeconds)
//                {
//                    return null;
//                }
//            }
//
//
//            /* pickup and drop off handeling */
//            if (newTask instanceof PlanActionDropoff)
//            {
//                freeCapacity++;
//
//                // discomfort increment
//                PlanComputationRequest newRequest = ((PlanActionDropoff) newTask).getRequest();
//                long taskExecutionTime = timeProvider.getCurrentSimTime() + newPlanTravelTime;
//                newPlanDiscomfort += taskExecutionTime - newRequest.getOriginTime() * 1000
//                        - newRequest.getMinTravelTime() * 1000;
//            }
//            else if (newTask instanceof PlanActionPickup)
//            {
//                // capacity check
//                if (freeCapacity == 0)
//                {
//                    return null;
//                }
//                freeCapacity--;
//            }
//
//
//            // index in old plan if the action was not new
//            if (newPlanIndex != pickupOptionIndex && newPlanIndex != dropoffOptionIndex)
//            {
//                indexInOldPlan++;
//            }
//
//            newPlanTasks.add(newTask);
//            previousTask = newTask;
//        }
//
//        // cost computation
//        double newPlanCost = planCostProvider.calculatePlanCost(newPlanDiscomfort, newPlanTravelTime);
//
//        return new cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan(newPlanTasks, newPlanTravelTime, newPlanCost);
//    }

    private String readableTime(long nanoTime)
    {
        long milisTotal = (long) nanoTime / 1000000;
        long millis = milisTotal % 1000;
        long second = (milisTotal / 1000) % 60;
        long minute = (milisTotal / (1000 * 60)) % 60;
        long hour = (milisTotal / (1000 * 60 * 60)) % 24;

        return String.format(" (%02d:%02d:%02d:%d)", hour, minute, second, millis);
    }

    private void logRidesharingStats(List<PlanComputationRequest> requests)
    {
        ridesharingStats.add(new RidesharingBatchStatsIH(failFastTime, insertionHeuristicTime, debugFailTime,
                requests.size()));
    }

//    private void computeBestPlanForRequest(PlanComputationRequest request)
//    {
//        resetBestPlan();
//
//        // in case of station system, add one vehicle from the nearest station
//        if (config.stations.on)
//        { //!onDemandvehicleStationStorage.isEmpty()){ //
//            OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestStation(request.getFrom(),
//                    OnDemandvehicleStationStorage.NearestType.TRAVELTIME_FROM);
//            int indexFromEnd = usedVehiclesPerStation[Integer.parseInt(nearestStation.getId())];
//            int index = nearestStation.getParkedVehiclesCount() - 1 - indexFromEnd;
//
//            if (index >= 0)
//            {
//                vehicleFromNearestStation = nearestStation.getVehicle(index);
//                vehiclesForPlanning.add(vehicleFromNearestStation);
//            }
//            else
//            {
//                LOGGER.warn("Nearest station {} empty for request {}", nearestStation, request);
//            }
//        }
//
//
//        long iterationStartTime = System.nanoTime();
//
//        vehiclesForPlanning.stream().parallel().forEach((tVvehicle) -> {
//            processRequestVehicleCombination(request, tVvehicle);
//        });
//
//        iterationTime += System.nanoTime() - iterationStartTime;
//    }

/*
    private void processRequestVehicleCombination(PlanComputationRequest request, AgentPolisEntity tVvehicle)
    {
        RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;

        cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan currentPlan = vehicle.getCurrentPlan();

        // if the plan was already changed
        if (planMap.containsKey(vehicle))
        {
            currentPlan = planMap.get(vehicle);
        }

        tryToAddRequestToPlan(request, vehicle, currentPlan);
    }
*/

/*
    public void tryToAddRequestToPlan(PlanComputationRequest request, RideSharingOnDemandVehicle vehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan plan)
    {
        // fail fast
        if (canServeRequest(vehicle, request))
        {
            computeOptimalPlan(vehicle, plan, request);
        }
    }
*/

//    private synchronized void tryUpdateBestPlan(PlanData newPlanData)
//    {
//        vehiclePlanningAllCallCount++;
//
//        if (newPlanData != null && newPlanData.increment < minCostIncrement)
//        {
//            minCostIncrement = newPlanData.increment;
//            bestPlan = newPlanData;
//        }
//    }

//    private void processRequest(PlanComputationRequest request)
//    {
//        Benchmark benchmark = new Benchmark();
//        benchmark.measureTime(() -> computeBestPlanForRequest(request));
//        insertionHeuristicTime += benchmark.getDurationMsInt();
//
//        if (bestPlan != null)
//        {
//            planMap.put(bestPlan.vehicle, bestPlan.plan);
//
//            if (bestPlan.vehicle == vehicleFromNearestStation)
//            {
//                usedVehiclesPerStation[Integer.parseInt(vehicleFromNearestStation.getParkedIn().getId())]++;
//            }
//            // remove nearest vehicle if not used
//            else
//            {
//                vehiclesForPlanning.remove(vehicleFromNearestStation);
//            }
//        }
//        else
//        {
//            LOGGER.debug("Request {} cannot be served!", request);
//            benchmark = new Benchmark();
//            benchmark.measureTime(() -> droppedDemandsAnalyzer.debugFail(request, usedVehiclesPerStation));
//            debugFailTime += benchmark.getDurationMs();
//        }
//    }

//    private List<OnDemandVehicle> getDrivingVehicles() {
////		List<OnDemandVehicle> listForPlanning = new ArrayList<>();
////		for(OnDemandVehicle vehicle: vehicleStorage){
////			if(vehicle.getState() != OnDemandVehicleState.REBALANCING){
////				listForPlanning.add(vehicle);
////			}
////		}
////		return listForPlanning;
////	}

//    private List<OnDemandVehicle> getVehiclesForPlanning()
//    {
//        vehiclesForPlanning = new ArrayList<>();
//        for (OnDemandVehicle vehicle : vehicleStorage)
//        {
//            if (vehicle.getState() != OnDemandVehicleState.REBALANCING
//                    && (!config.stations.on || vehicle.getState() != OnDemandVehicleState.WAITING))
//            {
//                vehiclesForPlanning.add(vehicle);
//            }
//        }
//
//        return vehiclesForPlanning;
//    }

    public void resetBestPlan()
    {
        minCostIncrement = Double.MAX_VALUE;
        bestPlan = null;
    }

    public cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan getBestPlan()
    {
        if (bestPlan == null)
        {
            return null;
        }
        return bestPlan.plan;
    }

    private class PlanData
    {
        final cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan plan;

        final double increment;

        final RideSharingOnDemandVehicle vehicle;

        public PlanData(RideSharingOnDemandVehicle vehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan plan, double increment)
        {
            this.vehicle = vehicle;
            this.plan = plan;
            this.increment = increment;
        }
    }

}
