package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.*;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.storage.OnDemandPFVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.lang.*;
import java.util.*;

import org.slf4j.LoggerFactory;

/**
 * comparator for sorting Requests at the start of solver algorithm
 */
class SortRequestsByOriginTime implements Comparator<DefaultPlanComputationRequest>
{
    public int compare(DefaultPlanComputationRequest a, DefaultPlanComputationRequest b)
    {
        return a.getPickUpAction().getMaxTime() - b.getPickUpAction().getMaxTime();
    }
}

class SortActionsByMaxTime implements Comparator<PlanRequestAction>
{
    public int compare(PlanRequestAction a, PlanRequestAction b)
    {
        return a.getMaxTime() - b.getMaxTime();
    }
}

// custom structure to store a taxi schedule and it's duration
class ScheduleWithDuration
{
    public final List<PlanRequestAction> schedule;
    public final int duration;

    public ScheduleWithDuration(List<PlanRequestAction> schedule, int duration)
    {
        this.schedule = schedule;
        this.duration = duration;
    }
}

class TaxiStatus
{
    public int freeCapacity;
    public boolean personOnBoard;

    public TaxiStatus(int freeCapacity, boolean personOnBoard)
    {
        this.freeCapacity = freeCapacity;
        this.personOnBoard = personOnBoard;
    }
}


@Singleton
public class PeopleFreightHeuristicSolver extends DARPSolverPFShared implements EventHandler
{

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PeopleFreightHeuristicSolver.class);

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

    private volatile long canServeRequestCallCount = 0;

    private long vehiclePlanningAllCallCount = 0;

    private int failFastTime;

    private int debugFailTime;

    private double minCostIncrement;

    OnDemandVehicle vehicleFromNearestStation;

    private int[] usedVehiclesPerStation;

    long totalBenefit = 0;

    private final List<PeopleFreightVehicle> allVehicles;

    private List<List<PlanRequestAction>> taxiSchedules;

    private List<TaxiStatus> taxiStatuses;

    @Inject
    public PeopleFreightHeuristicSolver(
            TravelTimeProvider travelTimeProvider,
            PlanCostProvider travelCostProvider,
            OnDemandPFVehicleStorage vehicleStorage,
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

        // casting given vehicles to actual PeopleFreightVehicles
        allVehicles = new ArrayList<>();
        for (AgentPolisEntity taxiEntity : vehicleStorage.getEntitiesForIteration())
        {
            PeopleFreightVehicle newTaxi = (PeopleFreightVehicle) taxiEntity;
            allVehicles.add(newTaxi);
        }
        taxiSchedules = new ArrayList<>();

        // initializing taxi statuses
        taxiStatuses = new ArrayList<>();
        for (int i = 0; i < allVehicles.size(); i++)
        {
            taxiStatuses.add(new TaxiStatus(allVehicles.get(i).getMaxParcelsCapacity(), false));
        }
    }


    /**
     *
     * @param newRequestsPeople         - people requests
     * @param waitingRequestsPeople     - null
     * @param newRequestsFreight        - freight requests
     * @param waitingRequestsFreight    - null
     * @return list of plans for all taxis
     */
    @Override
    public Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solve(List<PlanComputationRequestPeople> newRequestsPeople,
                                                                                                            List<PlanComputationRequestPeople> waitingRequestsPeople,
                                                                                                            List<PlanComputationRequestFreight> newRequestsFreight,
                                                                                                            List<PlanComputationRequestFreight> waitingRequestsFreight)
    {
        int curAlgorithmTime = 0;

        // maybe in future: how to calculate plan discomfort
        int planDiscomfort = 0;

        // init of empty lists of requests for each taxi
        for (int i = 0; i < allVehicles.size(); i++)
        {
            taxiSchedules.add(new ArrayList<>());
        }

        // list of plan durations for each taxi
        List<Integer> planDurations = new ArrayList<>(Arrays.asList(new Integer[allVehicles.size()]));
        Collections.fill(planDurations, 0);

        // all requests have default minTime = 0
        List<DefaultPlanComputationRequest> newRequestsAll = new ArrayList<>();
        if (newRequestsPeople != null)
        {
            newRequestsAll.addAll(newRequestsPeople);
        }
        if (newRequestsFreight != null)
        {
            newRequestsAll.addAll(newRequestsFreight);
        }
//        System.out.println("all requests:" + newRequestsAll.toString());

        // sort requests incrementally by time windows - maxPickupTime
        newRequestsAll.sort(new SortRequestsByOriginTime());

        List<PeopleFreightVehicle> availableTaxis = new ArrayList<>();

        for (int i = 0; i < newRequestsAll.size(); i++)
        {
            DefaultPlanComputationRequest currentRequest = newRequestsAll.get(i);

            // update algorithmTime
            curAlgorithmTime = currentRequest.getPickUpAction().getMaxTime();
            // update status of each vehicle
            for (int j = 0; j < allVehicles.size(); j++)
            {
                taxiStatuses.set(j, calcTaxiStatus(j, curAlgorithmTime));
            }
            // select vehicles available at the moment
            for (int j = 0; j < allVehicles.size(); j++)
            {
                if (taxiStatuses.get(j).personOnBoard == false)
                {
                    // if request is of type package: check if vehicle has enough space for the package
                    if (currentRequest instanceof PlanComputationRequestFreight)
                    {
                        if ( ((PlanComputationRequestFreight) currentRequest).getWeight() <= taxiStatuses.get(j).freeCapacity)
                        {
                            availableTaxis.add(allVehicles.get(j));
                        }
                    }
                    else
                    {
                        availableTaxis.add(allVehicles.get(j));
                    }
                }
            }

            double bestBenefit;         // f_i* - best total benefit, if request i is served
            int bestTaxiIdx = -1;        // k* - taxi to serve request i to get the best total benefit

            if (availableTaxis.size() > 0)
            {
                bestBenefit = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < availableTaxis.size(); k++)
                {
                    PeopleFreightVehicle currentTaxi = availableTaxis.get(k);
                    // check if schedule is feasible
                    ScheduleWithDuration possibleSchedule = trySchedule(allVehicles.indexOf(currentTaxi), currentRequest);
                    // if not feasible, continue to next taxi
                    if (possibleSchedule == null)
                    {
                        continue;
                    }

                    // benefit_k_i = new total benefit if taxi k serves request i
                    // TODO implement - calculate passenger's revenue ???
                    double passengerRevenue = 0;
                    double benefit_k_i = passengerRevenue - planCostProvider.calculatePlanCost(planDiscomfort, possibleSchedule.duration);
                    if (benefit_k_i > bestBenefit)
                    {
                        bestBenefit = benefit_k_i;
                        bestTaxiIdx = allVehicles.indexOf(currentTaxi);        // updating the idx of best taxi so far
                    }
                }
                // if suitable taxi was found
                if (bestTaxiIdx != -1)
                {
                    // Insert request i into route of taxi k∗
                    ScheduleWithDuration newSchedule = trySchedule(bestTaxiIdx, currentRequest);
                    taxiSchedules.set(bestTaxiIdx, newSchedule.schedule);  // NullPointerException won't happen, because this block happens only if some taxi was found
                    planDurations.set(bestTaxiIdx, newSchedule.duration);

                    // update total benefit
                    totalBenefit += bestBenefit;
                }
            }
            // else: reject request i => DO nothing
        }


        Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> returnMap = new HashMap<>();

        for (int i = 0; i < taxiSchedules.size(); i++)
        {
            List<PlanAction> actionsList = new ArrayList<>(taxiSchedules.get(i));
            int planTime = planDurations.get(i);
            double planCost = planCostProvider.calculatePlanCost(planDiscomfort, planTime);

            DriverPlan newPlan = new DriverPlan(actionsList, planTime, planCost);
            returnMap.put(allVehicles.get(i), newPlan);
        }

        return returnMap;
    }

    /**
     * calculates status of taxi 'taxiIdx' at given time
     */
    private TaxiStatus calcTaxiStatus (int taxiIdx, int curAlgTime)
    {
        List<PlanRequestAction> possibleTaxiSchedule = new ArrayList<>(taxiSchedules.get(taxiIdx));
        possibleTaxiSchedule.sort(new SortActionsByMaxTime());      // TODO: probably no need to sort it

        if (possibleTaxiSchedule.size() == 0)
        {
            return taxiStatuses.get(taxiIdx);
        }

        boolean personOnBoard = false;
        int curFreightWeight = 0;
        int taxiMaxCapacity = allVehicles.get(taxiIdx).getMaxParcelsCapacity();
        int tempTime = 0;
        for (int i = 0; tempTime <= curAlgTime; i++)
        {
            PlanAction action = possibleTaxiSchedule.get(i);
            if (action instanceof PlanActionPickup)
            {
                PlanActionPickup pickAction = (PlanActionPickup) action;
                // update the temporary time
                tempTime = pickAction.getMaxTime();
                PlanComputationRequest pickRequest = pickAction.request;
                if (pickRequest instanceof PlanComputationRequestFreight)
                {
                    // adding the package onBoard
                    curFreightWeight += ((PlanComputationRequestFreight) pickRequest).getWeight();
                }
                else
                {
                    // adding the person onBoard
                    personOnBoard = true;
                }
            }
            else if (action instanceof PlanActionDropoff)
            {
                // update the temporary time
                tempTime = ((PlanActionDropoff) action).getMaxTime();
                // if person is onBoard  && this is the dropoff action of person
                if (personOnBoard && ((PlanActionDropoff) action).request instanceof PlanComputationRequestPeople)
                {
                    // remove the person from the taxi
                    personOnBoard = false;
                }
                // if person is not onBoard, then remove package "weight" from the taxi
                else if (!personOnBoard)
                {
                    curFreightWeight -= ((PlanComputationRequestFreight) ((PlanActionDropoff) action).request).getWeight();
                }
            }
        }
        return new TaxiStatus(taxiMaxCapacity - curFreightWeight, personOnBoard);
    }

    /**
     * returns sorted list of new taxi schedule (or null if the schedule is not feasible) and duration of this schedule
     */
    private ScheduleWithDuration trySchedule(int taxiIndex, DefaultPlanComputationRequest newRequest)
    {
        List<PlanRequestAction> possibleTaxiSchedule = new ArrayList<>(taxiSchedules.get(taxiIndex));
        possibleTaxiSchedule.add(newRequest.getPickUpAction());
        possibleTaxiSchedule.add(newRequest.getDropOffAction());
        possibleTaxiSchedule.sort(new SortActionsByMaxTime());

        // pairs of EarlyTime and LateTime
        List<List<Integer>> timeWindows = new ArrayList<>();

        // setup time windows
        for (PlanRequestAction planRequestAction : possibleTaxiSchedule)
        {
            timeWindows.add(new ArrayList<Integer>(Arrays.asList(0, planRequestAction.getMaxTime())));
        }

        // for every Node: check if taxi is capable of carrying the passenger or package and whether it's possible to get to the next node
        boolean personOnBoard = false;
        String personOnBoardId = "";
        int curFreightWeight = 0;
        int taxiMaxCapacity = allVehicles.get(taxiIndex).getMaxParcelsCapacity();
        for (int i = 0; i < possibleTaxiSchedule.size() - 1; i++)   // size-1 ... the last Node of taxi has no following Node to be checked
        {
            // check for sufficient capacity
            PlanAction action = possibleTaxiSchedule.get(i);
            if (action instanceof PlanActionPickup)
            {
                // if person is on board, reject
                if (personOnBoard)
                {
                    return null;
                }
                PlanActionPickup pickAction = (PlanActionPickup) action;
                PlanComputationRequest pickRequest = pickAction.request;
                // checking sufficient freight capacity
                if (pickRequest instanceof PlanComputationRequestFreight)
                {
                    // if not sufficient freight capacity, reject
                    if (((PlanComputationRequestFreight) pickRequest).getWeight() + curFreightWeight > taxiMaxCapacity)
                    {
                        return null;
                    }
                    // adding the package onBoard
                    curFreightWeight += ((PlanComputationRequestFreight) pickRequest).getWeight();
                }
                else
                {
                    // adding the person onBoard
                    personOnBoard = true;
                    personOnBoardId = pickRequest.getDemandAgent().getId();
                }
            }
            else if (action instanceof PlanActionDropoff)
            {
                // if person is onBoard  &&  this is the dropoff action of the person onBoard, accept
                if (personOnBoard && ((PlanActionDropoff) action).request.getDemandAgent().getId().equals(personOnBoardId))
                {
                    // remove the person from the taxi
                    personOnBoard = false;
                    personOnBoardId = "";
                }
                // if person is not onBoard, then remove package "weight" from the taxi
                else if (!personOnBoard)
                {
                    curFreightWeight -= ((PlanComputationRequestFreight) ((PlanActionDropoff) action).request).getWeight();
                }
                // else reject
                else
                {
                    return null;
                }
            }


            List<Integer> currentTimeWindow = timeWindows.get(i);
            int earlyTime = currentTimeWindow.get(0) + (int) (travelTimeProvider.getExpectedTravelTime(possibleTaxiSchedule.get(i).getPosition(), possibleTaxiSchedule.get(i + 1).getPosition()) / 1000);
            int lateTime = currentTimeWindow.get(1) + (int) (travelTimeProvider.getExpectedTravelTime(possibleTaxiSchedule.get(i).getPosition(), possibleTaxiSchedule.get(i + 1).getPosition()) / 1000);

            // if taxi is getting to the next Node after maxTime of the Node
            if (earlyTime > currentTimeWindow.get(1))
            {
                // not feasible -> terminate
                return null;
            }
            timeWindows.get(i + 1).set(0, Math.toIntExact(Math.max(earlyTime, timeWindows.get(i + 1).get(0))));
            timeWindows.get(i + 1).set(1, Math.toIntExact(Math.min(lateTime, timeWindows.get(i + 1).get(1))));
        }
        // planTime = earlyTime of last time window - earlyTime of first time window
        int planTime = timeWindows.get(timeWindows.size() - 1).get(0) - timeWindows.get(0).get(0);

        return new ScheduleWithDuration(possibleTaxiSchedule, planTime);
    }


    // calculates new benefit
//    public double calcNewBenefit(taxi_k, request i)
    // check if there is feasible schedule for taxi k serving the request i


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

}

