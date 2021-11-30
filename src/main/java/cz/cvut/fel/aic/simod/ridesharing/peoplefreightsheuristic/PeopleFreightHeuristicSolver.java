package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
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
import cz.cvut.fel.aic.simod.storage.PeopleFreightVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.lang.*;
import java.net.Inet4Address;
import java.util.*;

import jdk.internal.net.http.common.Pair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.LoggerFactory;

/**
 * comparator for sorting Requests at the start of solver algorithm
 */
class SortRequestsByMaxPickupTime implements Comparator<DefaultPlanComputationRequest>
{
    public int compare(DefaultPlanComputationRequest a, DefaultPlanComputationRequest b)
    {
        return a.getMaxPickupTime() - b.getMaxPickupTime();
    }
}

class SortActionsByMaxTime implements Comparator<PlanRequestAction>
{
    public int compare(PlanRequestAction a, PlanRequestAction b)
    {
        return a.request.getMaxPickupTime() - b.request.getMaxPickupTime();
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

    private Map<RideSharingOnDemandVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> planMap;

    private int failFastTime;

    private int debugFailTime;

    private double minCostIncrement;

    OnDemandVehicle vehicleFromNearestStation;

    private int[] usedVehiclesPerStation;

    long totalBenefit = 0;

    private List<PeopleFreightVehicle> vehiclesForPlanning;


    @Inject
    public PeopleFreightHeuristicSolver(
            TravelTimeProvider travelTimeProvider,
            PlanCostProvider travelCostProvider,
            PeopleFreightVehicleStorage vehicleStorage,
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
        vehiclesForPlanning = new ArrayList<>();
        List<OnDemandVehicle> oldTaxis = new ArrayList<>(vehicleStorage.getEntities());
        for (OnDemandVehicle oldTaxi : oldTaxis)
        {
            PeopleFreightVehicle newTaxi = (PeopleFreightVehicle) oldTaxi;
            vehiclesForPlanning.add(newTaxi);
        }

        setEventHandeling();
    }


    @Override
    public Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solve(List<PlanComputationRequestPeople> newRequestsPeople,
                                                                                                              List<PlanComputationRequestPeople> waitingRequestsPeople,
                                                                                                              List<PlanComputationRequestFreight> newRequestsFreight,
                                                                                                              List<PlanComputationRequestFreight> waitingRequestsFreight)
    {
        // TODO how to calculate plan discomfort
        int planDiscomfort = 0;

        // list of lists of requests for each taxi
        List<List<PlanRequestAction>> taxiSchedules = new ArrayList<>();
        // list of plan durations for each taxi
        List<Integer> planDurations = new ArrayList<>();

        // all requests have default minTime = 0
        List<DefaultPlanComputationRequest> newRequestsAll = new ArrayList<>();
        newRequestsAll.addAll(newRequestsPeople);
        newRequestsAll.addAll(newRequestsFreight);

        // sort pickup requests (V_fo U V_po) incrementally by time windows - maxPickupTime
        newRequestsPeople.sort(new SortRequestsByMaxPickupTime());

        for (int i = 0; i < newRequestsPeople.size(); i++)
        {
            DefaultPlanComputationRequest currentRequest = newRequestsPeople.get(i);
            // find available taxis
            List<PeopleFreightVehicle> availableTaxis = new ArrayList<>();
            for (int j = 0; j < vehiclesForPlanning.size(); j++)
            {
                if (isAvailable(vehiclesForPlanning.get(i)))
                {
                    // if request is of type package: check if vehicle has enough space for the package
                    if (currentRequest instanceof PlanComputationRequestFreight)
                    {
                        if ( ((PlanComputationRequestFreight) currentRequest).getWeight() <= vehiclesForPlanning.get(i).getMaxParcelsCapacity()
                                                                                           - vehiclesForPlanning.get(i).getCurrentParcelsWeight() )
                        {
                            availableTaxis.add(vehiclesForPlanning.get(i));
                        }
                    }
                    else
                    {
                        availableTaxis.add(vehiclesForPlanning.get(i));
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
                    Pair<List<PlanRequestAction>, Integer> possibleSchedule = trySchedule(taxiSchedules.get(vehiclesForPlanning.indexOf(currentTaxi)), currentRequest);
                    // if not feasible, continue to next taxi
                    if (possibleSchedule == null)
                    {
                        continue;
                    }

                    // benefit_k_i = new total benefit if taxi k serves request i
                    // TODO implement - calculate passenger's revenue
                    double passengerRevenue = 0;
                    double benefit_k_i = passengerRevenue - planCostProvider.calculatePlanCost(planDiscomfort, possibleSchedule.second);
                    if (benefit_k_i > bestBenefit)
                    {
                        bestBenefit = benefit_k_i;
                        bestTaxiIdx = vehiclesForPlanning.indexOf(currentTaxi);        // updating the idx of best taxi so far
                    }
                }
                // if suitable taxi was found
                if (bestTaxiIdx != -1)
                {
                    // Insert request i into route of taxi k∗
                    Pair<List<PlanRequestAction>, Integer> newSchedule = trySchedule(taxiSchedules.get(bestTaxiIdx), currentRequest);
                    taxiSchedules.set(i, newSchedule.first);
                    planDurations.set(i, newSchedule.second);

                    // update total benefit
                    totalBenefit += bestBenefit;
                }
            }
            // else: reject request i = DO nothing
        }


        Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> returnMap = new HashMap<>();

        for (int i = 0; i < taxiSchedules.size(); i++)
        {
            List<PlanAction> actionsList = new ArrayList<>(taxiSchedules.get(i));
            int planTime = planDurations.get(i);
            double planCost = planCostProvider.calculatePlanCost(planDiscomfort, planTime);

            DriverPlan newPlan = new DriverPlan(actionsList, planTime, planCost);
            returnMap.put(vehiclesForPlanning.get(i), newPlan);
        }

        return returnMap;
    }


    /**
     * returns sorted list of new taxi schedule (or null if the schedule is not feasible) and duration of this schedule
     */
    private Pair<List<PlanRequestAction>, Integer> trySchedule(List<PlanRequestAction> taxiSchedule, DefaultPlanComputationRequest newRequest)
    {
        taxiSchedule.add(newRequest.getPickUpAction());
        taxiSchedule.add(newRequest.getDropOffAction());
        taxiSchedule.sort(new SortActionsByMaxTime());

        // pairs of EarlyTime and LateTime
        List<List<Integer>> timeWindows = new ArrayList<>();

        // setup time windows
        for (PlanRequestAction planRequestAction : taxiSchedule)
        {
            timeWindows.add(new ArrayList<Integer>( Arrays.asList (0, planRequestAction.getMaxTime()) ));
        }

        // for every Node check the timeWindow and whether it's possible to get to the next node
        for (int i = 0; i < taxiSchedule.size() - 1; i++)   // size-1 ... check for the last Node of taxi is not needed
        {
            List<Integer> currentTimeWindow = timeWindows.get(i);
            int earlyTime = currentTimeWindow.get(0) + (int)(travelTimeProvider.getExpectedTravelTime(taxiSchedule.get(i).getPosition(), taxiSchedule.get(i + 1).getPosition()) / 1000);
            int lateTime = currentTimeWindow.get(1) + (int)(travelTimeProvider.getExpectedTravelTime(taxiSchedule.get(i).getPosition(), taxiSchedule.get(i + 1).getPosition()) / 1000);

            // if getting to the next Node after maxTime of the Node
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

        return new Pair<>(taxiSchedule, planTime);
    }


    // calculates new benefit
//    public double calcNewBenefit(taxi_k, request i)
        // check if there is feasible schedule for taxi k serving the request i

    private boolean isAvailable(PeopleFreightVehicle vehicle)
    {
        // return true, if vehicle has no passenger onboard
        return vehicle.isPassengerOnboard();
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

}

