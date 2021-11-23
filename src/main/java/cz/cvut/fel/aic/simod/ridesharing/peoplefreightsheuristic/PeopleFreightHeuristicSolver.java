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
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.lang.*;
import java.util.*;

import org.slf4j.LoggerFactory;

/**
 * comparator for sorting Requests at the start of solver algorithm
 */
class SortByPickupTime implements Comparator<PlanComputationRequestPeople>
{
    public int compare(PlanComputationRequestPeople a, PlanComputationRequestPeople b)
    {
        return a.getMaxPickupTime() - b.getMaxPickupTime();
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

    private int insertionHeuristicTime;

    private int debugFailTime;

    private double minCostIncrement;

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
//        maxDistance = (double) config.ridesharing.maxProlongationInSeconds
//                * agentpolisConfig.maxVehicleSpeedInMeters;
//        maxDistanceSquared = maxDistance * maxDistance;

        // the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
        // vehicle to be considered to serve the request
//        maxDelayTime = config.ridesharing.maxProlongationInSeconds * 1000;

        setEventHandeling();
    }



    @Override
    public Map<RideSharingOnDemandVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solve(List<PlanComputationRequestPeople> newRequestsPeople,
                                                                                                                  List<PlanComputationRequestPeople> waitingRequestsPeople,
                                                                                                                  List<PlanComputationRequestFreight> newRequestsFreight,
                                                                                                                  List<PlanComputationRequestFreight> waitingRequestsFreight)
    {
        // sortedRequestsList = sort pickup requests (V_fo U V_po) incrementally by time windows - maxPickupTime
        PlanComputationRequestPeople[] arrRequestsPeople = newRequestsPeople.toArray(new PlanComputationRequestPeople[0]);
        Arrays.sort(arrRequestsPeople, new SortByPickupTime());

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

}

