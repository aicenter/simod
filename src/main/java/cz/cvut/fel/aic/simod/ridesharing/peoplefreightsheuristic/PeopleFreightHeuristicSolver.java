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
import cz.cvut.fel.aic.simod.storage.PeopleFreightVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
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

    private int debugFailTime;

    private double minCostIncrement;

    OnDemandVehicle vehicleFromNearestStation;

    private int[] usedVehiclesPerStation;


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
        List<OnDemandVehicle> oldTaxis = new ArrayList<>();
        oldTaxis.addAll(vehicleStorage.getEntities());
        for (int i = 0; i < oldTaxis.size(); i++)
        {
            OnDemandVehicle oldTaxi = oldTaxis.get(i);
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
        // TODO put together people and freight requests - how???
        List<? extends PlanComputationRequestPeople> newReqPpl = newRequestsPeople;
        List<? extends DefaultPlanComputationRequest> newRequestAll = newReqPpl; //new ArrayList<>();
//        newRequestAll.addAll(newReqPpl);


        // sort pickup requests (V_fo U V_po) incrementally by time windows - maxPickupTime
        Collections.sort(newRequestsPeople, new SortByPickupTime());

        for (int i = 0; i < newRequestsPeople.size(); i++)
        {
            DefaultPlanComputationRequest currentRequest = newRequestsPeople.get(i);
            // update all status of taxis K (and parking places) by time e_i
            // find available taxis
            List<PeopleFreightVehicle> availableTaxis = new ArrayList<>();
            for (int j = 0; j < vehiclesForPlanning.size(); j++)
            {
                if (isAvailable(vehiclesForPlanning.get(i)))
                {
                    availableTaxis.add(vehiclesForPlanning.get(i));
                }
            }

            double bestBenefit;         // f_opt - best total benefit, if request i is served
            int bestTaxiIdx = 0;        // k_opt - taxi to serve request i to get the best total benefit

            if (availableTaxis.size() > 0)
            {
                bestBenefit = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < availableTaxis.size(); k++)
                {
                    // benefit_k_i = new total benefit if taxi k serves request i
                    double benefit_k_i = 0;
//                    benefit_k_i = calcNewBenefit();  //TODO TravelCostProvider
                    if (benefit_k_i > bestBenefit)
                    {
                        bestBenefit = benefit_k_i;
                        bestTaxiIdx = k;            // updating the idx of best taxi so far
                    }
                }
                // k_opt.route.add(request i)
                //TODO add both request origin and destination to DrivePlan
                PlanAction newActionFrom = makeActionFrom(newReqPpl.get(i));
                PlanAction newActionTo = makeActionTo(newReqPpl.get(i));

                vehiclesForPlanning.get(bestTaxiIdx).getCurrentPlan().plan.add(newActionFrom);
                vehiclesForPlanning.get(bestTaxiIdx).getCurrentPlan().plan.add(newActionTo);
                // update total benefit
            }
            else
            {
                // reject request i
            }
        }
        //TODO create hashmap with vehicles and their plans
        Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> retMap = new HashMap<>();
        return retMap;
    }


    private PlanAction makeActionFrom(DefaultPlanComputationRequest request)
    {
        return new PlanAction(request.getFrom());
    }

    private PlanAction makeActionTo(DefaultPlanComputationRequest request)
    {
        return new PlanAction(request.getTo());
    }

    // TODO check if there is feasible schedule for taxi k serving the request i = Algorithm 3
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

