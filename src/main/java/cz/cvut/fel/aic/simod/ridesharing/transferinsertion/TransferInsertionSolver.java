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

import java.awt.image.AreaAveragingScaleFilter;
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
        //order to descending order
        Collections.reverse(requests);

        planMap = new ConcurrentHashMap<>();

        // zkusim najit plan bez prestupu
        for (PlanComputationRequest request : requests) {
            List<Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>> itinerariesPairs = new ArrayList<>();
            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            List<Long> delays = new ArrayList<>();
            List<Long> waitTimes = new ArrayList<>();

            long minimalTravelTime = travelTimeProvider.getExpectedTravelTime(request.getFrom(), request.getTo());
            for (RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                List<PlanAction> positnry = findItineraryBestInsertion(request.getPickUpAction(), request.getDropOffAction(), taxi);
                if (positnry != null) {
                    // pridat plan do nejakeho seznamu vsech validnich planu
                    Long dropoffTime = getDropoffTimeForRequest(positnry, taxi, request);
                    if (dropoffTime == null) {
                        // chyba
                        continue;
                    }
                    delays.add(dropoffTime - timeProvider.getCurrentSimTime() - minimalTravelTime);
                    waitTimes.add((long) Long.MAX_VALUE);
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

                            // todo: mozna u dropoff akce upravit max time o dve sekundy driv?
                            PlanActionDropoffTransfer dropoffActionTransfer = new PlanActionDropoffTransfer(request, station, maxDropOffTime);
                            PlanActionPickupTransfer pickupActionTransfer = new PlanActionPickupTransfer(request, station, maxDropOffTime);

                            //vytvorim plan pro prvni usek cesty
                            List<PlanAction> itnryp1 = findItineraryBestInsertion(request.getPickUpAction(), dropoffActionTransfer, taxi);
                            if (itnryp1 == null) {
                                continue;
                            }
                            Long dropTime = getDropoffTimeForRequest(itnryp1, taxi, request);

                            //vytvorim plan pro druhy usek cesty
                            List<PlanAction> itnryp2 = findTransferItineraryBestInsertion(pickupActionTransfer, request.getDropOffAction(), taxis.get(k), dropTime);
                            if (itnryp2 == null) {
                                continue;
                            }
                            int maxTransferTime = countMaxTimeTransfer(itnryp1, taxi, itnryp2, taxis.get(k), request);

                            for (PlanAction action : itnryp1) {
                                if (action.equals(dropoffActionTransfer)) {
                                    ((PlanActionDropoffTransfer)action).setMaxTime(maxTransferTime);
                                }
                            }

                            Long dropoffTime = getDropoffTimeForRequest(itnryp2, taxis.get(k), request);
                            if (dropoffTime == null) {
                                // chyba
                                continue;
                            }
                            delays.add(dropoffTime - timeProvider.getCurrentSimTime() - minimalTravelTime);

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

            // vyberu nejlepsi a dam ho do mapy
            // ted budu chtit plany seradit podle delay
            // vyberu z nich treba 20 %
            // ty dale seradim podle waitTime
            // budu se snazit vybrat takove, co maji wait time 0 a vetsi (uplatnuji prestup),
            // pokud zadne takove nebudou, vezmu i ty s wait Time -1 (to jsou ty bez prestupu)

            List<TransferPlan> transferPlans = new ArrayList<>();
            for (int j = 0; j < itinerariesPairs.size(); j++) {
                TransferPlan t = new TransferPlan(waitTimes.get(j), delays.get(j), itinerariesPairs.get(j));
                transferPlans.add(t);
            }
            transferPlans.sort(TransferPlan::compareByDelay);
            //vezmu hornich beta procent
            double beta = 0.2;
            int numOfTaken = (int) (delays.size() * beta);
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
            sublistTransferPlans.sort(TransferPlan::compareByTransferTime);


            if (!sublistTransferPlans.isEmpty()) {
                Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> key = sublistTransferPlans.get(0).pair;
                List<List<PlanAction>> plansForVehicles = key.getFirst();
                List<RideSharingOnDemandVehicle> vehicles = key.getSecond();
                for (int q = 0; q < vehicles.size(); q++) {
                    List<PlanAction> vehPlan = plansForVehicles.get(q);
//                    List<PlanAction> planWithPos = new ArrayList<>();
//                    planWithPos.add(vehicles.get(q).getCurrentPlanNoUpdate().plan.get(0));
//                    planWithPos.addAll(vehPlan);
                    DriverPlan dp = new DriverPlan(vehPlan, 0, 0);
                    planMap.put(vehicles.get(q), dp);
                }

//                DriverPlan newPlan = new DriverPlan(s.get(0).getFirst().get(0), 0, 0);
//                planMap.put(itinerariesPairs.get(0).getSecond().get(0), newPlan);
            }
        }


            // jak hledat plan bez prestupu?
                // v planu nikdo neprestupuje - provedu insertion metodu
                // v planu prestupuje, ale je to prvni cast - provedu insertion metodu, ale musim mit spravne nastaveny maxTime dropoffTransfer akce
                // v planu prestupuje, ale je to druhy usek - muzu zkusit udelat to same, ale nesmim prekrocit maxTime u pickupTransfer akce

        // potom budu hledat stanice ve kterych je mozne prestoupit
        // zkusim vytvorit prestupni plan
                // prvni usek budu vytvaret uplne stejne jako je hledani planu bez prestupu
                    // zjistim cas prijezdu auta na stanici s prestupujicim cestujicim
                // na druhy usek vyzkousim vsechny moznosti kam akce zaradit
                    // do planu pridam waitAkci - spocitam cas prijezdu druheho auta na stanici a cas dorovnam wait akci, aby to sedelo
                    // nastavim maxTransferTime
                    // zkontroluju jestli je plan validni, nevalidni plany zahodim
                    // z validnich planu vyberu ten, co bude mit nejmensi zpozdeni
                // zkombinuji tyto dva plany ?

        // ze vsech planu vyberu nekolik s nejmensim delay

        // budu chtit uprednostnit plany s prestupem

        // zaroven ale budu vybirat takove plany, co maji kratky wait time, aby auta zbytecne nestala ve stanici

        return planMap;
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
//                int waitTime = (int) (Math.round(arrivalToStationForVeh1 / 1000.0) - Math.round(arrivalTime / 1000.0));
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
        return 0;
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

    public boolean canPickupRequestInTime(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        // cas prijezdu auta k pickup pozici je mensi nez maxPickupTime requestu
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = 0;
        SimulationNode previousDestination = vehicle.getPosition();

        // podivam se na current trip plan a spocitam cas potrebny na dokonceni cesty po aktualni hrane
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                // protoze je size trip planu 0, tak to znamena, ze uz je auto rozjete do posledni destinace tripu
                // je ale mozne, ze tam jeste nedojelo, tedy jeho pozice je jina nez pozice posledniho bodu v trip planu
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

        // podivam se na current trip plan a spocitam cas potrebny na dokonceni cesty po aktualni hrane
        if (vehicle.getCurrentTripPlan() != null) {
            if (vehicle.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) vehicle.getCurrentTripPlan().getLastLocation();
                // protoze je size trip planu 0, tak to znamena, ze uz je auto rozjete do posledni destinace tripu
                // je ale mozne, ze tam jeste nedojelo, tedy jeho pozice je jina nez pozice posledniho bodu v trip planu
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
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxPickupTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pickupTransfer.getMaxTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < dropoffTransfer.getMaxTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxDropoffTime() * 1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
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
        }
        Pair<Boolean, Long> p = new Pair<>(ret, time);
        return p;
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
                    // aktivita uz zacala
                    // podivam se jestli uz je i pauznuta
                    // odectu uz odcekany cas
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


}
