package cz.cvut.fel.aic.simod.ridesharing.transferinsertion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.greedyTASeT.TransferPlan;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class TransferInsertionSolver extends DARPSolver implements EventHandler {

    private final TypedSimulation eventProcessor;

    private final SimodConfig config;

    private final TimeProvider timeProvider;

    private final PositionUtil positionUtil;

    private final DroppedDemandsAnalyzer droppedDemandsAnalyzer;

    private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;


    private List<SimulationNode> transferPoints;

    protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;

    private Map<RideSharingOnDemandVehicle, DriverPlan> planMap;


    @Inject
    public TransferInsertionSolver(
            OnDemandVehicleStorage vehicleStorage,
            TravelTimeProvider travelTimeProvider,
            PlanCostProvider travelCostProvider,
            DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory,
            TypedSimulation eventProcessor,
            SimodConfig config,
            TimeProvider timeProvider,
            PositionUtil positionUtil,
            DroppedDemandsAnalyzer droppedDemandsAnalyzer,
            OnDemandvehicleStationStorage onDemandvehicleStationStorage,
            AgentpolisConfig agentpolisConfig) {

        super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
        this.eventProcessor = eventProcessor;
        this.config = config;
        this.timeProvider = timeProvider;
        this.positionUtil = positionUtil;
        this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
        this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
        this.requestFactory = requestFactory;

        setEventHandeling();
    }

    public void setTransferPoints(List<SimulationNode> transferPoints) {
        this.transferPoints = transferPoints;
    }

    @Override
    public EventProcessor getEventProcessor() {
        return eventProcessor;
    }

    @Override
    public void handleEvent(Event event) {

    }

    private void setEventHandeling() {
        List<Enum> typesToHandle = new LinkedList<>();

        typesToHandle.add(OnDemandVehicleEvent.PICKUP);
        eventProcessor.addEventHandler(this, typesToHandle);
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, List<PlanComputationRequest> waitingRequests) {

        // nacist taxiky
        List<RideSharingOnDemandVehicle> taxis = new ArrayList<>();
        for(AgentPolisEntity tVvehicle: vehicleStorage.getEntitiesForIteration()) {
            RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
            taxis.add(vehicle);
        }
        // nacist requesty
        List<PlanComputationRequest> requests = new ArrayList<>(newRequests);

        // ke kazdemu requestu priradit auta, ktera k nemu mohou dojet vcas
        int[] possiblePickupTaxisCounts = new int[requests.size()];
        int i = 0;
        Map<PlanComputationRequest, List<RideSharingOnDemandVehicle>> possiblePickupTaxisMap = new HashMap<>();
        for(PlanComputationRequest request : requests) {
            int counter = 0;
            List<RideSharingOnDemandVehicle> possiblePickupTaxisOneRequest = new ArrayList<>();
            for(RideSharingOnDemandVehicle t : taxis) {
                if (canPickupRequestInTime(t, request)) {
                    counter++;
                    possiblePickupTaxisOneRequest.add(t);
                }
            }
            possiblePickupTaxisCounts[i] = counter;
            possiblePickupTaxisMap.put(request, possiblePickupTaxisOneRequest);
            i++;
        }

        //sort R by the number of possible pickup taxis
        List<PlanComputationRequest> requestsCopy = new ArrayList<>(requests);
        requests.sort(Comparator.comparing(x -> possiblePickupTaxisCounts[requestsCopy.indexOf(x)]));


        planMap = new ConcurrentHashMap<>();

        for (PlanComputationRequest request : requests) {
            List<Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>> itinerariesPairs = new ArrayList<>();
            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            List<Long> delays = new ArrayList<>();
            List<Long> waitTimes = new ArrayList<>();

            long minimalTravelTime = travelTimeProvider.getExpectedTravelTime(request.getFrom(), request.getTo());
            for (RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                List<PlanAction> positnry = findItineraryBestInsertion(request.getPickUpAction(), request.getDropOffAction(), taxi);
                if (positnry != null) {
                    Long dropoffTime = getDropoffTimeForRequest(positnry, taxi, request);
                    if (dropoffTime == null) {
                        // chyba
                        continue;
                    }
                    delays.add(dropoffTime - timeProvider.getCurrentSimTime() - minimalTravelTime);
                    waitTimes.add((long)0);
                    List<List<PlanAction>> listItinerary = new ArrayList<>();
                    listItinerary.add(positnry);
                    List<RideSharingOnDemandVehicle> vehicles = new ArrayList<>();
                    vehicles.add(taxi);
                    itinerariesPairs.add(new Pair<>(listItinerary, vehicles));
                }

                for (SimulationNode station : transferPoints) {
                    if (timeProvider.getCurrentSimTime() + travelTimeProvider.getTravelTime(taxi, request.getFrom()) + travelTimeProvider.getExpectedTravelTime(request.getFrom(), station) >
                            request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                        continue;
                    }
                    // najit auta, do kterych je mozne prestoupit
                    for (int k = 0; k < taxis.size(); k++) {
                        if (taxi.equals(taxis.get(k))) {
                            continue;
                        } else if (station == request.getTo()) {
                            continue;
                        } else if (station == request.getFrom()) {
                            continue;
                        } else if (timeProvider.getCurrentSimTime() + travelTimeProvider.getTravelTime(taxis.get(k), station) >
                                request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                            continue;
                        } else {
                            int maxDropOffTime = request.getMaxDropoffTime() - (int) Math.round(travelTimeProvider.getExpectedTravelTime(station, request.getTo()) / 1000.0);

                            PlanActionDropoffTransfer dropoffActionTransfer = new PlanActionDropoffTransfer(request, station, maxDropOffTime);
                            PlanActionPickupTransfer pickupActionTransfer = new PlanActionPickupTransfer(request, station, maxDropOffTime);

                            // prvni usek = ceil
                            List<PlanAction> itnryp1 = findItineraryBestInsertionFirstSegment(request.getPickUpAction(), dropoffActionTransfer, taxi);
                            if (itnryp1 == null) {
                                continue;
                            }
                            Integer dropTime = getDropoffTimeForRequestFirstSegment(itnryp1, taxi, request);

                            // druhy usek = floor
                            List<PlanAction> itnryp2 = findTransferItineraryBestInsertionSecondSegment(pickupActionTransfer, request.getDropOffAction(), taxis.get(k), dropTime);
                            if (itnryp2 == null) {
                                continue;
                            }
                            int maxTransferTime = countMaxTimeTransferRound(itnryp1, taxi, itnryp2, taxis.get(k), request);
                            for (PlanAction action : itnryp1) {
                                if (action.equals(dropoffActionTransfer)) {
                                    ((PlanActionDropoffTransfer)action).setMaxTime(maxTransferTime);
                                }
                            }
                            Integer dropoffTime = getDropoffTimeForRequestFirstSegment(itnryp2, taxis.get(k), request);
                            if (dropoffTime == null) {
                                // chyba
                                continue;
                            }
                            delays.add(dropoffTime * 1000 - timeProvider.getCurrentSimTime() - minimalTravelTime);

                            long waitTime = getWaitTimeForRequest(itnryp2, request);
                            waitTimes.add(waitTime);

                            List<List<PlanAction>> listItinerary = new ArrayList<>();
                            listItinerary.add(itnryp1);
                            listItinerary.add(itnryp2);
                            List<RideSharingOnDemandVehicle> vehicles = new ArrayList<>();
                            vehicles.add(taxi);
                            vehicles.add(taxis.get(k));
                            itinerariesPairs.add(new Pair<>(listItinerary, vehicles));

                        }
                    }
                }
            }

            List<TransferPlan> transferPlans = new ArrayList<>();
            for (int j = 0; j < itinerariesPairs.size(); j++) {
                    TransferPlan t = new TransferPlan(waitTimes.get(j), delays.get(j), itinerariesPairs.get(j));
                    transferPlans.add(t);
            }
            transferPlans.sort(TransferPlan::compareByTransferTime);
            double beta = 0.2;
            int numOfTaken = (int) (transferPlans.size() * beta);
            if (numOfTaken == 0) {
                numOfTaken = 1;
            }
            List<TransferPlan> sublistTransferPlans = new ArrayList<>();
            for (int q = 0; q < numOfTaken; q++)
            {
                if(!transferPlans.isEmpty()) {
                    sublistTransferPlans.add(transferPlans.get(q));
                }
            }
            sublistTransferPlans.sort(TransferPlan::compareByDelay);

            if (!sublistTransferPlans.isEmpty()) {
                Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> key = sublistTransferPlans.get(0).pair;
                List<List<PlanAction>> plansForVehicles = key.getFirst();
                List<RideSharingOnDemandVehicle> vehicles = key.getSecond();
                for (int q = 0; q < vehicles.size(); q++) {
                    List<PlanAction> vehPlan = plansForVehicles.get(q);
                    DriverPlan dp = new DriverPlan(vehPlan, 0, 0);
                    planMap.put(vehicles.get(q), dp);
                }

            }
        }
        return planMap;
    }



    public List<PlanAction> findItineraryBestInsertionFirstSegment(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle) {
        DriverPlan currentVehiclePlan;
        if (planMap.containsKey(vehicle)) {
            currentVehiclePlan = planMap.get(vehicle);
        } else {
            currentVehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        List<List<PlanAction>> allPosibleActionsOrder = new ArrayList<>();
        List<Integer> planDurations = new ArrayList<>();

        for (int i = 1; i < currentVehiclePlan.plan.size()+1; i++) {
            for (int j = i+1; j < currentVehiclePlan.plan.size()+2; j++) {
                List<PlanAction> tempPlan = new ArrayList<>(currentVehiclePlan.plan);
                tempPlan.add(i, pickup);
                tempPlan.add(j, dropoff);
                Pair<Boolean, Integer> p = checkValidItineraryAndCountPlanDurationFirstPart(tempPlan, vehicle);
                if (p.getFirst()) {
                    if (checkCapacityNotExceeded(tempPlan, vehicle)) {
                        allPosibleActionsOrder.add(tempPlan);
                        planDurations.add(p.getSecond());
                    }
                }
            }
        }

        if (allPosibleActionsOrder.isEmpty()) {
            return null;
        }

        int minIndex = planDurations.indexOf(Collections.min(planDurations));
        List<PlanAction> selectedPlan = allPosibleActionsOrder.get(minIndex);

        return selectedPlan;
    }

    public List<PlanAction> findTransferItineraryBestInsertionSecondSegment(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle, int arrivalToStationForVeh1) {
        DriverPlan currentVehiclePlan;
        if (planMap.containsKey(vehicle)) {
            currentVehiclePlan = planMap.get(vehicle);
        } else {
            currentVehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        List<List<PlanAction>> allPosibleActionsOrder = new ArrayList<>();
        List<Integer> planDurations = new ArrayList<>();

        for (int i = 1; i < currentVehiclePlan.plan.size()+1; i++) {
            for (int j = i+1; j < currentVehiclePlan.plan.size()+2; j++) {
                List<PlanAction> tempPlan = new ArrayList<>(currentVehiclePlan.plan);
                tempPlan.add(i, pickup);
                tempPlan.add(j, dropoff);
                Integer arrivalTime = getArrivalTimeToStationIntFloor(tempPlan, vehicle, ((PlanRequestAction)pickup).request);
                assert arrivalTime != null;
                int waitTime = arrivalToStationForVeh1 - arrivalTime;
                if (waitTime > 0) {
                    PlanActionWait waitAction = new PlanActionWait(((PlanRequestAction)pickup).request, pickup.getPosition(), ((PlanRequestAction) pickup).getMaxTime(), waitTime * 1000);
                    tempPlan.add(i, waitAction);
                }
                Pair<Boolean, Integer> p = checkValidItineraryAndCountPlanDurationSecondPart(tempPlan, vehicle);
                if (p.getFirst()) {
                    if (checkCapacityNotExceeded(tempPlan, vehicle)) {
                        allPosibleActionsOrder.add(tempPlan);
                        planDurations.add(p.getSecond());
                    }
                }
            }
        }

        if (allPosibleActionsOrder.isEmpty()) {
            return null;
        }

        int minIndex = planDurations.indexOf(Collections.min(planDurations));
        List<PlanAction> selectedPlan = allPosibleActionsOrder.get(minIndex);

        return selectedPlan;
    }

    public List<PlanAction> findItineraryBestInsertion(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle) {
        DriverPlan currentVehiclePlan;
        if (planMap.containsKey(vehicle)) {
            currentVehiclePlan = planMap.get(vehicle);
        } else {
            currentVehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        List<List<PlanAction>> allPosibleActionsOrder = new ArrayList<>();
        List<Long> planDurations = new ArrayList<>();

        for (int i = 1; i < currentVehiclePlan.plan.size()+1; i++) {
            for (int j = i+1; j < currentVehiclePlan.plan.size()+2; j++) {
                List<PlanAction> tempPlan = new ArrayList<>(currentVehiclePlan.plan);
                tempPlan.add(i, pickup);
                tempPlan.add(j, dropoff);
                Pair<Boolean, Long> p = checkValidItineraryAndCountPlanDuration(tempPlan, vehicle);
                if (p.getFirst()) {
                    if (checkCapacityNotExceeded(tempPlan, vehicle)) {
                        allPosibleActionsOrder.add(tempPlan);
                        planDurations.add(p.getSecond());
                    }
                }
            }
        }

        if (allPosibleActionsOrder.isEmpty()) {
            return null;
        }

        int minIndex = planDurations.indexOf(Collections.min(planDurations));
        List<PlanAction> selectedPlan = allPosibleActionsOrder.get(minIndex);

        return selectedPlan;
    }

    public List<PlanAction> findTransferItineraryBestInsertion(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle, Long arrivalToStationForVeh1) {
        DriverPlan currentVehiclePlan;
        if (planMap.containsKey(vehicle)) {
            currentVehiclePlan = planMap.get(vehicle);
        } else {
            currentVehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        List<List<PlanAction>> allPosibleActionsOrder = new ArrayList<>();
        List<Long> planDurations = new ArrayList<>();

        for (int i = 1; i < currentVehiclePlan.plan.size()+1; i++) {
            for (int j = i+1; j < currentVehiclePlan.plan.size()+2; j++) {
                List<PlanAction> tempPlan = new ArrayList<>(currentVehiclePlan.plan);
                tempPlan.add(i, pickup);
                tempPlan.add(j, dropoff);
                Long arrivalTime = getArrivalTimeToStation(tempPlan, vehicle, ((PlanRequestAction)pickup).request);
                assert arrivalTime != null;
                long waitTime = arrivalToStationForVeh1 - arrivalTime;
                if (waitTime >= 0) {
                    waitTime += 3000;
                    PlanActionWait waitAction = new PlanActionWait(((PlanRequestAction)pickup).request, pickup.getPosition(), ((PlanRequestAction) pickup).getMaxTime(), waitTime);
                    tempPlan.add(i, waitAction);
                }
                else if (waitTime < 0 && waitTime > -3000) {
                    waitTime -= 3000;
                    PlanActionWait waitAction = new PlanActionWait(((PlanRequestAction)pickup).request, pickup.getPosition(), ((PlanRequestAction) pickup).getMaxTime(), -waitTime);
                    tempPlan.add(i, waitAction);
                }
                Pair<Boolean, Long> p = checkValidItineraryAndCountPlanDuration(tempPlan, vehicle);
                if (p.getFirst()) {
                    if (checkCapacityNotExceeded(tempPlan, vehicle)) {
                        allPosibleActionsOrder.add(tempPlan);
                        planDurations.add(p.getSecond());
                    }
                }
            }
        }

        if (allPosibleActionsOrder.isEmpty()) {
            return null;
        }

        int minIndex = planDurations.indexOf(Collections.min(planDurations));
        List<PlanAction> selectedPlan = allPosibleActionsOrder.get(minIndex);

        return selectedPlan;
    }

    public long getWaitTimeForRequest(List<PlanAction> itinerary, PlanComputationRequest request) {
        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionWait) {
                if (((PlanActionWait) action).request == request) {
                    return ((PlanActionWait) action).getWaitTime();
                }
            }
        }
        return -1;
    }

    public int countMaxTimeTransfer(List<PlanAction> itnryp1, RideSharingOnDemandVehicle veh1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh2, PlanComputationRequest request) {
        boolean isWaitInItnryp2 = false;
        for (PlanAction action : itnryp2) {
            if (action instanceof PlanActionWait) {
                if (((PlanActionWait) action).request == request) {
                    isWaitInItnryp2 = true;
                }
            }
        }
        if (isWaitInItnryp2) {
            Long dropTime = getDropoffTimeForRequest(itnryp1, veh1, request);
            int time = (int) Math.round(dropTime / 1000.0);
            return time;
        } else {
            Long dropTime = getArrivalTimeToStation(itnryp2, veh2, request);
            int time = (int) Math.round(dropTime / 1000.0);
            return time - 2;
        }
    }

    public int countMaxTimeTransferRound(List<PlanAction> itnryp1, RideSharingOnDemandVehicle veh1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh2, PlanComputationRequest request) {
        boolean isWaitInItnryp2 = false;
        for (PlanAction action : itnryp2) {
            if (action instanceof PlanActionWait) {
                if (((PlanActionWait) action).request == request) {
                    isWaitInItnryp2 = true;
                }
            }
        }
        if (isWaitInItnryp2) {
            Integer dropTime = getDropoffTimeForRequestFirstSegment(itnryp1, veh1, request);
            return dropTime;
        } else {
            Integer dropTime = getArrivalTimeToStationIntFloor(itnryp2, veh2, request);
            return dropTime - 1;
        }
    }

    public boolean canPickupRequestInTime(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        SimulationNode previousDestination = vehicle.getPosition();

        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time += timeToFinishEdge;

        long timeToNewPickup = travelTimeProvider.getExpectedTravelTime(previousDestination, request.getFrom());
        time += timeToNewPickup;

        if (!(time > request.getMaxPickupTime() * 1000)) {
            return true;
        }
        return false;

    }

    private boolean checkCapacityNotExceeded(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        int freeCapacity = vehicle.getFreeCapacity();
        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                freeCapacity -= 1;
                if (freeCapacity < 0) {
                    return false;
                }
            }
            else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                freeCapacity += 1;
            }
        }
        return true;
    }

    private Pair<Boolean, Long> checkValidItineraryAndCountPlanDuration(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        SimulationNode previousDestination;
        boolean ret = true;
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        previousDestination = vehicle.getPosition();

        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time += timeToFinishEdge;

        for (PlanAction action : itinerary) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxPickupTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pickupTransfer.getMaxTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < dropoffTransfer.getMaxTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxDropoffTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    SimulationNode dest = action.getPosition();
                    PlanActionWait wait = (PlanActionWait) action;
                    if (!(wait.isWaitingStarted())) {
                        time += wait.getWaitTime();
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = action.getPosition();
                    } else {
                        long substract;
                        long waitTime;
                        if (wait.isWaitingPaused()) {
                            substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                            waitTime = wait.getWaitTime() - substract;
                        } else {
                            waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());
                        }
                        time = time + waitTime;
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = action.getPosition();
                    }
                }
            }
        }
        Pair<Boolean, Long> p = new Pair<>(ret, time);
        return p;
    }

    private Pair<Boolean, Integer> checkValidItineraryAndCountPlanDurationFirstPart(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        SimulationNode previousDestination;
        boolean ret = true;
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        previousDestination = vehicle.getPosition();
        int timeRounded = (int) Math.ceil(timeProvider.getCurrentSimTime() / 1000.0);

        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time += timeToFinishEdge;
        timeRounded += (int) Math.ceil(timeToFinishEdge / 1000.0);

        for (PlanAction action : itinerary) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pcq.getMaxPickupTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pickupTransfer.getMaxTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < dropoffTransfer.getMaxTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pcq.getMaxDropoffTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    SimulationNode dest = action.getPosition();
                    PlanActionWait wait = (PlanActionWait) action;
                    if (!(wait.isWaitingStarted())) {
                        time += wait.getWaitTime();
                        timeRounded += (int) Math.ceil(wait.getWaitTime() / 1000.0);
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                        previousDestination = action.getPosition();
                    } else {
                        long substract;
                        long waitTime;
                        if (wait.isWaitingPaused()) {
                            substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                            waitTime = wait.getWaitTime() - substract;
                        } else {
                            waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());
                        }
                        time = time + waitTime;
                        timeRounded += (int) Math.ceil(wait.getWaitTime() / 1000.0);
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        timeRounded += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                        previousDestination = action.getPosition();
                    }
                }
            }
        }
        Pair<Boolean, Integer> p = new Pair<>(ret, timeRounded);
        return p;
    }

    private Pair<Boolean, Integer> checkValidItineraryAndCountPlanDurationSecondPart(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        SimulationNode previousDestination;
        boolean ret = true;
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        previousDestination = vehicle.getPosition();
        int timeRounded = (int) Math.floor(timeProvider.getCurrentSimTime() / 1000.0);

        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time += timeToFinishEdge;
        timeRounded += (int) Math.floor(timeToFinishEdge / 1000.0);

        for (PlanAction action : itinerary) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pcq.getMaxPickupTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pickupTransfer.getMaxTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < dropoffTransfer.getMaxTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (!(timeRounded < pcq.getMaxDropoffTime())) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    SimulationNode dest = action.getPosition();
                    PlanActionWait wait = (PlanActionWait) action;
                    if (!(wait.isWaitingStarted())) {
                        time += wait.getWaitTime();
                        timeRounded += (int) Math.floor(wait.getWaitTime() / 1000.0);
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                        previousDestination = action.getPosition();
                    } else {
                        long substract;
                        long waitTime;
                        if (wait.isWaitingPaused()) {
                            substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                            waitTime = wait.getWaitTime() - substract;
                        } else {
                            waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());
                        }
                        time = time + waitTime;
                        timeRounded += (int) Math.floor(wait.getWaitTime() / 1000.0);
                        time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        timeRounded += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                        previousDestination = action.getPosition();
                    }
                }
            }
        }
        Pair<Boolean, Integer> p = new Pair<>(ret, timeRounded);
        return p;
    }

    private Integer getDropoffTimeForRequestFirstSegment(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        int timeInt = (int) Math.ceil(timeProvider.getCurrentSimTime() / 1000.0);
        SimulationNode previousDestination = vehicle.getPosition();
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time = time + timeToFinishEdge;
        timeInt += (int) Math.ceil(timeToFinishEdge / 1000.0);

        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                timeInt += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition()) / 1000.0);
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                timeInt += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition()) / 1000.0);
                if (((PlanRequestAction) action).getRequest() == request) {
                    return timeInt;
                }
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionWait) {
                SimulationNode dest = action.getPosition();
                PlanActionWait wait = (PlanActionWait) action;
                if (!(wait.isWaitingStarted())) {
                    time = time + wait.getWaitTime();
                    timeInt += (int) Math.ceil(wait.getWaitTime());
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeInt += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition()) / 1000.0);
                    previousDestination = action.getPosition();
                } else {
                    long substract;
                    long waitTime;
                    if (wait.isWaitingPaused()) {
                        substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                        waitTime = wait.getWaitTime() - substract;
                    } else {
                        waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());
                    }
                    time = time + waitTime;
                    timeInt += (int) Math.ceil(wait.getWaitTime());
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeInt += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition()) / 1000.0);
                    previousDestination = action.getPosition();
                }
            }
        }
        return null;
    }


    private Long getDropoffTimeForRequest(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        SimulationNode previousDestination = vehicle.getPosition();
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time = time + timeToFinishEdge;

        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                if (((PlanRequestAction) action).getRequest() == request) {
                    return time;
                }
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionWait) {
                SimulationNode dest = action.getPosition();
                PlanActionWait wait = (PlanActionWait) action;
                if (!(wait.isWaitingStarted())) {
                    time = time + wait.getWaitTime();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = action.getPosition();
                } else {
                    long substract;
                    long waitTime;
                    if (wait.isWaitingPaused()) {
                        substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                        waitTime = wait.getWaitTime() - substract;
                    } else {
                        waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());
                    }
                    time = time + waitTime;
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = action.getPosition();
                }
            }
        }
        return null;
    }

    private Long getArrivalTimeToStation(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        SimulationNode previousDestination = vehicle.getPosition();
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time = time + timeToFinishEdge;

        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                if (((PlanRequestAction) action).getRequest() == request) {
                    return time;
                }
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionWait) {
                PlanActionWait wait = (PlanActionWait) action;
                SimulationNode dest = action.getPosition();
                if (!(wait.isWaitingStarted())) {
                    time = time + wait.getWaitTime();
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = action.getPosition();
                } else {
                    long substract;
                    long waitTime;
                    if (wait.isWaitingPaused()) {
                        substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                        waitTime = wait.getWaitTime() - substract;
                    } else {
                        waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());

                    }
                    time = time + waitTime;
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = action.getPosition();
                }
            }

        }
        return null;
    }

    private Integer getArrivalTimeToStationIntFloor(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        long time = timeProvider.getCurrentSimTime();
        int timeInt = (int) Math.floor(timeProvider.getCurrentSimTime() / 1000.0);
        long timeToFinishEdge = 0;
        SimulationNode previousDestination = vehicle.getPosition();
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, stopLoc);
                previousDestination = stopLoc;
            }
            else if (vehicle.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge += travelTimeProvider.getTravelTime(vehicle, currLoc);
                    }
                    if (currLoc == vehicle.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) vehicle.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time = time + timeToFinishEdge;
        timeInt += (int) Math.floor(timeToFinishEdge / 1000.0);

        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                timeInt += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition())/1000.0);
                if (((PlanRequestAction) action).getRequest() == request) {
                    return timeInt;
                }
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                timeInt += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition())/1000.0);
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionWait) {
                PlanActionWait wait = (PlanActionWait) action;
                SimulationNode dest = action.getPosition();
                if (!(wait.isWaitingStarted())) {
                    time = time + wait.getWaitTime();
                    timeInt += (int) Math.floor(wait.getWaitTime() / 1000.0);
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeInt += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition())/1000.0);
                    previousDestination = action.getPosition();
                } else {
                    long substract;
                    long waitTime;
                    if (wait.isWaitingPaused()) {
                        substract = wait.getWaitingPausedAt() - wait.getWaitingStartedAt();
                        waitTime = wait.getWaitTime() - substract;
                    } else {
                        waitTime = wait.getWaitTime() - (timeProvider.getCurrentSimTime() - wait.getWaitingStartedAt());

                    }
                    time = time + waitTime;
                    timeInt += (int) Math.floor(waitTime / 1000.0);
                    time += travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    timeInt += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition())/1000.0);
                    previousDestination = action.getPosition();
                }
            }

        }
        return null;
    }
}
