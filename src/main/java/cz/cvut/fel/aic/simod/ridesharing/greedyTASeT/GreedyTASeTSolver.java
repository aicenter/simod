package cz.cvut.fel.aic.simod.ridesharing.greedyTASeT;

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
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class GreedyTASeTSolver extends DARPSolver implements EventHandler {


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
    public GreedyTASeTSolver(
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

    /**
     * Transfer-allowed scheduling solver
     * @return Map
     */
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, List<PlanComputationRequest> waitingRequests) {
        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new ConcurrentHashMap<>();
        Map<RideSharingOnDemandVehicle, DriverPlan> planMapReturn = new ConcurrentHashMap<>();
        List<RideSharingOnDemandVehicle> taxis = new ArrayList<>();

        for(AgentPolisEntity tVvehicle: vehicleStorage.getEntitiesForIteration()) {
            RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
            taxis.add(vehicle);
        }

        planMap = dispatch(taxis, newRequests);



        return planMap;
    }

    /**
     * Transfer-allowed scheduling function
     * @return
     */
    private Map<RideSharingOnDemandVehicle, DriverPlan> dispatch(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        // because all passengers allow ridesharing, only greedy taset will be called
        List<RideSharingOnDemandVehicle> carpoolAcceptingTaxis = taxis;
        List<PlanComputationRequest> carpoolAcceptingPassengers = requests;
        Map<RideSharingOnDemandVehicle, DriverPlan> map = heuristics(carpoolAcceptingTaxis, carpoolAcceptingPassengers);
        return map;
    }


    private boolean isWithTransfer(DriverPlan plan) {
        for(PlanAction action : plan.plan) {
            if(action instanceof PlanRequestAction) {
                if (action instanceof PlanActionDropoffTransfer || action instanceof PlanActionPickupTransfer || action instanceof PlanActionWait) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findLastPickupIndex(DriverPlan plan) {
        int index = -1;
        for (int i = 0; i < plan.getLength(); i++) {
            if (plan.plan.get(i) instanceof PlanActionPickup) {
                index = i;
            }
        }
        return index;
    }

    private int findLastPickupIndexList(List<PlanAction> plan) {
        int index = -1;
        for (int i = 0; i < plan.size(); i++) {
            if (plan.get(i) instanceof PlanActionPickup) {
                index = i;
            }
        }
        return index;
    }

    private int findLastTransferActionIndex(DriverPlan plan) {
        int index = -1;
        for (int i = 0; i < plan.getLength(); i++) {
            if (plan.plan.get(i) instanceof PlanActionDropoffTransfer || plan.plan.get(i) instanceof PlanActionPickupTransfer || plan.plan.get(i) instanceof PlanActionWait) {
                index = i;
            }
        }
        return index;
    }

    private long countTimeToNewPickup(RideSharingOnDemandVehicle taxi, PlanComputationRequest request, List<PlanComputationRequest> requestsOnBoard) {
        DriverPlan taxiPlan;
        if (planMap.containsKey(taxi)) {
            taxiPlan = planMap.get(taxi);
        } else {
            taxiPlan = taxi.getCurrentPlanNoUpdate();
        }
        if (isWithTransfer(taxiPlan)) {
            int indexLastTransferAction = findLastTransferActionIndex(taxiPlan);
            long timeToFinishCurrentEdge = 0;
            SimulationNode previousPos = taxi.getPosition();
            if (taxi.getCurrentTask() != null) {
                timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                previousPos = taxi.getCurrentTask().getPosition();
            }
            long timeToLastTransferAction = 0;
            for (int q = 0; q < indexLastTransferAction + 1; q++) {
                if (taxiPlan.plan.get(q) instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) taxiPlan.plan.get(q);
                    timeToLastTransferAction = timeToLastTransferAction + wait.getWaitTime();
                } else {
                    timeToLastTransferAction = timeToLastTransferAction + travelTimeProvider.getExpectedTravelTime(previousPos, taxiPlan.plan.get(q).getPosition());
                }
                previousPos = taxiPlan.plan.get(q).getPosition();
            }
            List<PlanAction> segmentAfterTransfer = taxiPlan.plan.subList(indexLastTransferAction+1, taxiPlan.plan.size());
            int indexLastPickupSegment = findLastPickupIndexList(segmentAfterTransfer);
            long timeToLastPickup = 0;
            SimulationNode previousPos2 = taxiPlan.plan.get(taxiPlan.plan.size()-1).getPosition();
            if (segmentAfterTransfer.size() > 0) {
                previousPos2 = segmentAfterTransfer.get(0).getPosition();
                for (int q = 0; q < indexLastPickupSegment + 1; q++) {
                    timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos2, segmentAfterTransfer.get(q).getPosition());
                    previousPos2 = segmentAfterTransfer.get(q).getPosition();
                }
            }
            SimulationNode newPickupFrom = request.getFrom();
            long timeToNewPick = travelTimeProvider.getExpectedTravelTime(previousPos2, newPickupFrom);
            long estimatedArrivalToPickup = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToNewPick;
                long maxTime = request.getMaxDropoffTime() * 1000;
                if (estimatedArrivalToPickup > maxTime) {
                    return Long.MAX_VALUE;
            }
            return estimatedArrivalToPickup;
        }
        else {
            int indexLastPickup = findLastPickupIndex(taxiPlan);
            long timeToFinishCurrentEdge = 0;
            SimulationNode previousPos = taxi.getPosition();
            if (taxi.getCurrentTask() != null) {
                timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                previousPos = taxi.getCurrentTask().getPosition();
            }
            long timeToLastPickup = 0;
            for (int q = 0; q < indexLastPickup + 1; q++) {
                timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos, taxiPlan.plan.get(q).getPosition());
                previousPos = taxiPlan.plan.get(q).getPosition();
            }
            SimulationNode newPickupFrom = request.getFrom();
            long timeToNewPick = travelTimeProvider.getExpectedTravelTime(previousPos, newPickupFrom);
            long estimatedArrivalToNewPickup = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToNewPick;
                long maxTime = request.getMaxDropoffTime() * 1000;
                if (estimatedArrivalToNewPickup > maxTime) {
                    return Long.MAX_VALUE;
                }
            return estimatedArrivalToNewPickup;
        }
    }

    /**
     * Greedy TASeT heuristics function
     * @return
     */
    private Map<RideSharingOnDemandVehicle, DriverPlan> heuristics(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //transfer points = charging stations
        List<SimulationNode> transferPoints = this.transferPoints;

        //lookup table LT - LT [t][k] stores the earliest arrival time for taxi k to charging station t without violating the tolerable delay for k’s current passengers
        int stationsCount = transferPoints.size();
        int taxisCount = taxis.size();
        long[][] LT = new long[stationsCount][taxisCount];
        //fill the LT table
        for (int i = 0; i < taxisCount; i++) {
            RideSharingOnDemandVehicle taxi = taxis.get(i);
            Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
            DriverPlan actualPlan = taxi.getCurrentPlanNoUpdate();
            for(PlanAction action : actualPlan) {
                if(action instanceof PlanRequestAction) {
                    PlanRequestAction requestAction = (PlanRequestAction) action;
                    requestsOnBoardSet.add(requestAction.request);
                }
            }
            List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);
            // kolik lidi je prave ted v aute
            boolean taxiFree = true;
            int taxiCapacity = taxi.getCapacity();
            if (requestsOnBoardSet.size() >= taxiCapacity) {
                taxiFree = false;
            }
            for(int j = 0; j < stationsCount; j++) {
                //check if taxi has free seat
                if (!taxiFree) {
                    LT[j][i] = Long.MAX_VALUE;
                }
                else {
                    if (isWithTransfer(taxi.getCurrentPlanNoUpdate())) {
                        // nekdo prestupuje
                        int indexLastTransferAction = findLastTransferActionIndex(taxi.getCurrentPlanNoUpdate());
                        long timeToFinishCurrentEdge = 0;
                        SimulationNode previousPos = taxi.getPosition();
                        if (taxi.getCurrentTask() != null) {
                            timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                            previousPos = taxi.getCurrentTask().getPosition();
                        }
                        long timeToLastTransferAction = 0;
                        for (int q = 0; q < indexLastTransferAction + 1; q++) {
                            if (taxi.getCurrentPlanNoUpdate().plan.get(q) instanceof PlanActionWait) {
                                PlanActionWait wait = (PlanActionWait) taxi.getCurrentPlanNoUpdate().plan.get(q);
                                timeToLastTransferAction = timeToLastTransferAction + wait.getWaitTime();
                            } else {
                                timeToLastTransferAction = timeToLastTransferAction + travelTimeProvider.getExpectedTravelTime(previousPos, taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition());
                            }
                            previousPos = taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition();
                        }
                        // ted potrebuju najit posledni pickup ve zbyvajicim driver planu po prestupu
                        List<PlanAction> segmentAfterTransfer = taxi.getCurrentPlanNoUpdate().plan.subList(indexLastTransferAction+1, taxi.getCurrentPlanNoUpdate().plan.size());
                        int indexLastPickupSegment = findLastPickupIndexList(segmentAfterTransfer);
                        long timeToLastPickup = 0;
                        SimulationNode previousPos2 = taxi.getCurrentPlanNoUpdate().plan.get(taxi.getCurrentPlanNoUpdate().plan.size()-1).getPosition();
                        if (segmentAfterTransfer.size() > 0) {
                            previousPos2 = segmentAfterTransfer.get(0).getPosition();
                            for (int q = 0; q < indexLastPickupSegment + 1; q++) {
                                timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos2, segmentAfterTransfer.get(q).getPosition());
                                previousPos2 = segmentAfterTransfer.get(q).getPosition();
                            }
                        }
                        SimulationNode station = transferPoints.get(j);
                        long timeToStation = travelTimeProvider.getExpectedTravelTime(previousPos, station);
                        long estimatedArrivalToStation = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToStation;
                        LT[j][i] = estimatedArrivalToStation;
                        Set<PlanComputationRequest> requestsInSegmentSet = new HashSet<>();
                        for(PlanAction action : segmentAfterTransfer) {
                            if(action instanceof PlanRequestAction) {
                                PlanRequestAction requestAction = (PlanRequestAction) action;
                                requestsInSegmentSet.add(requestAction.request);
                            }
                        }
                        List<PlanComputationRequest> requestsInSegment = new ArrayList<>(requestsInSegmentSet);
                        for (PlanComputationRequest request : requestsInSegment) {
                            long maxTime = request.getMaxDropoffTime() * 1000;
                            if (LT[j][i] > maxTime) {
                                LT[j][i] = Long.MAX_VALUE;
                                break;
                            }
                        }
                    }
                    else {
                        int indexLastPickup = findLastPickupIndex(taxi.getCurrentPlanNoUpdate());
                        long timeToFinishCurrentEdge = 0;
                        SimulationNode previousPos = taxi.getPosition();
                        if (taxi.getCurrentTask() != null) {
                            timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                            previousPos = taxi.getCurrentTask().getPosition();
                        }
                        long timeToLastPickup = 0;
                        for (int q = 0; q < indexLastPickup + 1; q++) {
                            timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos, taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition());
                            previousPos = taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition();
                        }
                        SimulationNode station = transferPoints.get(j);
                        long timeToStation = travelTimeProvider.getExpectedTravelTime(previousPos, station);
                        long estimatedArrivalToStation = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToStation;
                        LT[j][i] = estimatedArrivalToStation;
                        for (PlanComputationRequest request : requestsOnBoard) {
                            long maxTime = request.getMaxDropoffTime() * 1000;
                            if (LT[j][i] > maxTime) {
                                LT[j][i] = Long.MAX_VALUE;
                                break;
                            }
                        }
                    }
                }
            }
        }

        //we rank the requests in descending order by the number of taxis that are possible to pick them up in time (without considering transfer or destination)
        //get possible taxis for every request and number of possible taxis
        int[] possiblePickupTaxisCounts = new int[requests.size()];
        int i = 0;
        Map<PlanComputationRequest, List<RideSharingOnDemandVehicle>> possiblePickupTaxisMap = new HashMap<>();
        for(PlanComputationRequest request : requests) {
            int counter = 0;
            List<RideSharingOnDemandVehicle> possiblePickupTaxisOneRequest = new ArrayList<>();
            for(RideSharingOnDemandVehicle t : taxis) {
                if (canServeRequestTASeT2(t, request)) {
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


        for(PlanComputationRequest request : requests) {
            List<Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>> templistP = new ArrayList<>();
            List<Long> delays = new ArrayList<>();
            List<Long> transferTimes = new ArrayList<>();
            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            for (RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                List<PlanAction> posbitnry = findPlanWithNoTransferNew(request, taxi);
                if (posbitnry != null) {
                    if (checkValidItinerary(posbitnry, taxi)) {
                        List<List<PlanAction>> tmp = new ArrayList<>();
                        List<RideSharingOnDemandVehicle> tmpVehs = new ArrayList<>();
                        tmpVehs.add(taxi);
                        tmp.add(posbitnry);
                        Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> pair = new Pair<>(tmp, tmpVehs);
                        templistP.add(pair);
                        long minimalArrivalTime = timeProvider.getCurrentSimTime() + travelTimeProvider.getExpectedTravelTime(request.getFrom(), request.getTo());
                        HashMap<PlanComputationRequest, Long> dropoffs = getEstimatedTimesOfDropoff(posbitnry, taxi);
                        long realArrivalTime = dropoffs.get(request);
                        long delay = realArrivalTime - minimalArrivalTime;
                        delays.add(delay);
                        transferTimes.add((long) 0);
                    }
                }
                Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
                DriverPlan actualPlan;
                if (planMap.containsKey(taxi)) {
                    actualPlan = planMap.get(taxi);
                } else {
                    actualPlan = taxi.getCurrentPlanNoUpdate();
                }
                for(PlanAction action : actualPlan) {
                    if(action instanceof PlanRequestAction) {
                        PlanRequestAction requestAction = (PlanRequestAction) action;
                        requestsOnBoardSet.add(requestAction.request);
                    }
                }
                List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);

                long timeToNewPickup = countTimeToNewPickup(taxi, request, requestsOnBoard);
                int stationIndex = 0;
                for (SimulationNode station : transferPoints) {
                    if (timeToNewPickup + travelTimeProvider.getExpectedTravelTime(request.getFrom(), station) >
                            request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                        continue;
                    }

                    // find k' = taxis that taxi k can transfer to at station
                    for (int k = 0; k < taxisCount; k++) {
                        //not possible to transfer to
                        if (LT[stationIndex][k] == Long.MAX_VALUE) {
                            continue;
                        } else if(LT[stationIndex][k] > request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                            continue;
                        } else if(taxi.equals(taxis.get(k))) {
                            continue;
                        } else if (station == request.getTo()) {
                            continue;
                        } else if (station == request.getFrom()) {
                            continue;
                        } else {
                            // split request to two requests with transfer point
                            long travelTimeFromStationToDest = travelTimeProvider.getExpectedTravelTime(station, request.getTo());
                            int maxDropOffTime = request.getMaxDropoffTime() - (int) Math.round(travelTimeFromStationToDest / 1000.0);
                            PlanActionDropoffTransfer dropoffActionTransfer = new PlanActionDropoffTransfer(request, station, maxDropOffTime);
                            PlanActionPickupTransfer pickupActionTransfer = new PlanActionPickupTransfer(request, station, maxDropOffTime);

                            List<PlanAction> itnryp1 = findPlanWithNoTransferActionsNew(request.getPickUpAction(), dropoffActionTransfer, taxi);       // pro auto
                            List<PlanAction> itnryp2 = findPlanWithNoTransferActionsNew(pickupActionTransfer, request.getDropOffAction(), taxis.get(k));
                            if (itnryp1 == null || itnryp2 == null) {
                                continue;
                            }
                            Pair<List<List<PlanAction>>, Long> p = createChargePlanNoNewRequests(itnryp1, itnryp2, taxi, taxis.get(k), request);
                            if (p == null) {
                                continue;
                            } else {
                                List<List<PlanAction>> itnrys = p.getFirst();
                                itnryp1 = itnrys.get(0);
                                itnryp2 = itnrys.get(1);
                                List<List<PlanAction>> tmp2 = new ArrayList<>();
                                tmp2.add(itnryp1);
                                tmp2.add(itnryp2);
                                List<RideSharingOnDemandVehicle> tmpVehs2 = new ArrayList<>();
                                tmpVehs2.add(taxi);
                                tmpVehs2.add(taxis.get(k));
                                Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> pair2 = new Pair<>(tmp2, tmpVehs2);
                                templistP.add(pair2);
                                HashMap<PlanComputationRequest, Long> drops = getEstimatedTimesOfDropoff(itnryp2, taxis.get(k));
                                if (drops == null) {
                                    delays.add(Long.MAX_VALUE);
                                    transferTimes.add((long) -1);
                                    continue;
                                }
                                long minimalArrivalTime = timeProvider.getCurrentSimTime() + travelTimeProvider.getExpectedTravelTime(request.getFrom(), request.getTo());
                                long realArrivalTime = drops.get(request);
                                long delay = realArrivalTime - minimalArrivalTime;
                                delays.add(delay);
                                long transferTime = p.getSecond();
                                transferTimes.add(transferTime);
                            }
                        }
                    }
                    stationIndex++;
                }
            }
            List<TransferPlan> listTransferPlans = new ArrayList<>();
            for (int j = 0; j < templistP.size(); j++) {
                TransferPlan t = new TransferPlan(transferTimes.get(j), delays.get(j), templistP.get(j));
                listTransferPlans.add(t);
            }
            listTransferPlans.sort(TransferPlan::compareByDelay);

            double beta = 0.2;
            int numOfTaken = (int) (delays.size() * beta);
            if (numOfTaken == 0) {
                numOfTaken = 1;
            }
            List<TransferPlan> sublistTransferPlans = new ArrayList<>();
            for (int q = 0; q < numOfTaken; q++)
            {
                if(!listTransferPlans.isEmpty()) {
                    sublistTransferPlans.add(listTransferPlans.get(q));
                }
            }
            sublistTransferPlans.sort(TransferPlan::compareByTransferTime);
            Collections.reverse(sublistTransferPlans);

            if (sublistTransferPlans.isEmpty()) {
                continue;
            }
            else
            {
                if (sublistTransferPlans.get(0).delay == Long.MAX_VALUE || sublistTransferPlans.get(0).trasferTime < 0) {
                    continue;
                }
                Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> key = sublistTransferPlans.get(0).pair;
                List<List<PlanAction>> plansForVehicles = key.getFirst();
                List<RideSharingOnDemandVehicle> vehicles = key.getSecond();
                for (int q = 0; q < vehicles.size(); q++) {
                    List<PlanAction> vehPlan = plansForVehicles.get(q);
                    List<PlanAction> planWithPos = new ArrayList<>();
                    planWithPos.add(vehicles.get(q).getCurrentPlanNoUpdate().plan.get(0));
                    planWithPos.addAll(vehPlan);
                    DriverPlan dp = new DriverPlan(planWithPos, 0, 0);
                    planMap.put(vehicles.get(q), dp);
                }
            }

            // update k, k′ and LT
            for (int z = 0; z < taxisCount; z++) {
                RideSharingOnDemandVehicle taxi = taxis.get(z);
                Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
                DriverPlan actualPlan;
                if (planMap.containsKey(taxis.get(z))) {
                    actualPlan = planMap.get(taxi);
                } else {
                    actualPlan = taxi.getCurrentPlanNoUpdate();
                }
                for(PlanAction action : actualPlan) {
                    if(action instanceof PlanRequestAction) {
                        PlanRequestAction requestAction = (PlanRequestAction) action;
                        requestsOnBoardSet.add(requestAction.request);
                    }
                }
                List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);
                boolean taxiFree = true;
                int taxiCapacity = taxi.getCapacity();
                if (requestsOnBoardSet.size() >= taxiCapacity) {
                    taxiFree = false;
                }
                for(int j = 0; j < stationsCount; j++) {
                    if (!taxiFree) {
                        LT[j][z] = Long.MAX_VALUE;
                    } else {
                        if (isWithTransfer(actualPlan)) {
                            int indexLastTransferAction = findLastTransferActionIndex(actualPlan);
                            long timeToFinishCurrentEdge = 0;
                            SimulationNode previousPos = taxi.getPosition();
                            if (taxi.getCurrentTask() != null) {
                                timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                                previousPos = taxi.getCurrentTask().getPosition();
                            }
                            long timeToLastTransferAction = 0;
                            for (int q = 0; q < indexLastTransferAction + 1; q++) {
                                if (actualPlan.plan.get(q) instanceof PlanActionWait) {
                                    PlanActionWait wait = (PlanActionWait) actualPlan.plan.get(q);
                                    timeToLastTransferAction = timeToLastTransferAction + wait.getWaitTime();
                                } else {
                                    timeToLastTransferAction = timeToLastTransferAction + travelTimeProvider.getExpectedTravelTime(previousPos, actualPlan.plan.get(q).getPosition());
                                }
                                previousPos = actualPlan.plan.get(q).getPosition();
                            }
                            List<PlanAction> segmentAfterTransfer = actualPlan.plan.subList(indexLastTransferAction + 1, actualPlan.plan.size());
                            int indexLastPickupSegment = findLastPickupIndexList(segmentAfterTransfer);
                            long timeToLastPickup = 0;
                            SimulationNode previousPos2 = actualPlan.plan.get(actualPlan.plan.size()-1).getPosition();
                            if (segmentAfterTransfer.size() > 0) {
                                previousPos2 = segmentAfterTransfer.get(0).getPosition();
                                for (int q = 0; q < indexLastPickupSegment + 1; q++) {
                                    timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos2, segmentAfterTransfer.get(q).getPosition());
                                    previousPos2 = segmentAfterTransfer.get(q).getPosition();
                                }
                            }
                            SimulationNode station = transferPoints.get(j);
                            long timeToStation = travelTimeProvider.getExpectedTravelTime(previousPos2, station);
                            long estimatedArrivalToStation = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToStation;
                            LT[j][z] = estimatedArrivalToStation;
                            Set<PlanComputationRequest> requestsInSegmentSet = new HashSet<>();
                            for (PlanAction action : segmentAfterTransfer) {
                                if (action instanceof PlanRequestAction) {
                                    PlanRequestAction requestAction = (PlanRequestAction) action;
                                    requestsInSegmentSet.add(requestAction.request);
                                }
                            }
                            List<PlanComputationRequest> requestsInSegment = new ArrayList<>(requestsInSegmentSet);
                            for (PlanComputationRequest r : requestsInSegment) {
                                long maxTime = r.getMaxDropoffTime() * 1000;
                                if (LT[j][z] > maxTime) {
                                    LT[j][z] = Long.MAX_VALUE;
                                    break;
                                }
                            }
                        } else {
                            int indexLastPickup = findLastPickupIndex(actualPlan);
                            long timeToFinishCurrentEdge = 0;
                            SimulationNode previousPos = taxi.getPosition();
                            if (taxi.getCurrentTask() != null) {
                                timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                                previousPos = taxi.getCurrentTask().getPosition();
                            }
                            long timeToLastPickup = 0;
                            for (int q = 0; q < indexLastPickup + 1; q++) {
                                timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos, actualPlan.plan.get(q).getPosition());
                                previousPos = actualPlan.plan.get(q).getPosition();
                            }
                            SimulationNode station = transferPoints.get(j);
                            long timeToStation = travelTimeProvider.getExpectedTravelTime(previousPos, station);
                            long estimatedArrivalToStation = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToStation;
                            LT[j][z] = estimatedArrivalToStation;
                            for (PlanComputationRequest r : requestsOnBoard) {
                                long maxTime = r.getMaxDropoffTime() * 1000;
                                if (LT[j][z] > maxTime) {
                                    LT[j][z] = Long.MAX_VALUE;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return planMap;
    }

    private boolean checkValidItinerary(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        SimulationNode previousDestination = vehicle.getPosition();
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
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxPickupTime()*1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (!(time < pcq.getMaxDropoffTime()*1000)) {
                        ret = false;
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) action;
                    time = time + wait.getWaitTime();
                }
            }
        }
        return ret;
    }

    private Pair<List<List<PlanAction>>, Long> createChargePlanNoNewRequests(List<PlanAction> itnryp1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh1, RideSharingOnDemandVehicle veh2, PlanComputationRequest request) {
        long time1 = 0;
        long timeToFinishEdge1 = 0;
        SimulationNode previousDestination = veh1.getPosition();

        if (veh1.getCurrentTripPlan() != null) {
            if (veh1.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) veh1.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge1 += travelTimeProvider.getTravelTime(veh1, stopLoc);
                previousDestination = stopLoc;
            }
            else if (veh1.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) veh1.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge1 += travelTimeProvider.getTravelTime(veh1, currLoc);
                    }
                    if (currLoc == veh1.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time1 = timeToFinishEdge1;

        long transferTime = 0;
        //expected arrival time of first car
        for (PlanAction action : itnryp1) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (dropoffTransfer.request == request) {
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) action;
                    time1 = time1 + wait.getWaitTime();
                }
            }
        }
        // expected arrival of second car
        int indexPickupSecondCar = 0;
        long time2 = 0;
        long timeToFinishEdge2 = 0;
        PlanActionPickupTransfer pickup = null;
        previousDestination = veh2.getPosition();

        if (veh2.getCurrentTripPlan() != null) {
            if (veh2.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge2 += travelTimeProvider.getTravelTime(veh2, stopLoc);
                previousDestination = stopLoc;
            } else if (veh2.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge2 += travelTimeProvider.getTravelTime(veh2, currLoc);
                    }
                    if (currLoc == veh2.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time2 = timeToFinishEdge2;

        for (PlanAction action : itnryp2) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    if (pickupTransfer.request == request) {
                        pickup = pickupTransfer;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) action;
                    time2 = time2 + wait.getWaitTime();
                }
            }
            indexPickupSecondCar++;
        }
        long waitTime = time2 - time1;

        boolean valid = true;

        if (waitTime > 0 && waitTime < 2000) {
            long newWait = 2000 - waitTime;
            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), newWait);
            transferTime = newWait;
            itnryp2.add(indexPickupSecondCar, waitAction);

            //check tolerable delay for passengers in vehicle2
            long time = 0;
            previousDestination = veh2.getPosition();
            if (veh2.getCurrentTripPlan() != null) {
                if (veh2.getCurrentTripPlan().getSize() == 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
                    time += travelTimeProvider.getTravelTime(veh2, stopLoc);
                    previousDestination = stopLoc;
                }
                else if (veh2.getCurrentTripPlan().getSize() > 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
                    SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
                    boolean currLocIsVehiclePosition = false;
                    int curridx = 0;
                    while (currLoc != stopLoc) {
                        if (currLocIsVehiclePosition) {
                            time += travelTimeProvider.getTravelTime(veh2, currLoc);
                        }
                        if (currLoc == veh2.getPosition()) {
                            currLocIsVehiclePosition = true;
                        }
                        previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                        curridx++;
                        currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                    }
                }
            }

            for (PlanAction action : itnryp2) {
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if (action instanceof PlanActionPickup) {
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxPickupTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionDropoff) {
                        SimulationNode dest = pcq.getTo();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionWait) {
                        PlanActionWait wait = (PlanActionWait) action;
                        time = time + wait.getWaitTime();
                        previousDestination = wait.getPosition();
                    } else if (action instanceof PlanActionPickupTransfer) {
                        PlanActionPickupTransfer p = (PlanActionPickupTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    } else if(action instanceof PlanActionDropoffTransfer) {
                        PlanActionDropoffTransfer p = (PlanActionDropoffTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    }
                }
            }
        }
        if (waitTime <= 0) {
            waitTime = waitTime - 2000;
            assert pickup != null;
            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), -waitTime);
            transferTime = -waitTime;
            itnryp2.add(indexPickupSecondCar, waitAction);

            long time = 0;
            previousDestination = veh2.getPosition();
            if (veh2.getCurrentTripPlan() != null) {
                if (veh2.getCurrentTripPlan().getSize() == 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
                    time += travelTimeProvider.getTravelTime(veh2, stopLoc);
                    previousDestination = stopLoc;
                }
                else if (veh2.getCurrentTripPlan().getSize() > 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
                    SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
                    boolean currLocIsVehiclePosition = false;
                    int curridx = 0;
                    while (currLoc != stopLoc) {
                        if (currLocIsVehiclePosition) {
                            time += travelTimeProvider.getTravelTime(veh2, currLoc);
                        }
                        if (currLoc == veh2.getPosition()) {
                            currLocIsVehiclePosition = true;
                        }
                        previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                        curridx++;
                        currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                    }
                }
            }

            for (PlanAction action : itnryp2) {
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if (action instanceof PlanActionPickup) {
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxPickupTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionDropoff) {
                        SimulationNode dest = pcq.getTo();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionWait) {
                        PlanActionWait wait = (PlanActionWait) action;
                        time = time + wait.getWaitTime();
                        previousDestination = wait.getPosition();
                    } else if (action instanceof PlanActionPickupTransfer) {
                        PlanActionPickupTransfer p = (PlanActionPickupTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    } else if(action instanceof PlanActionDropoffTransfer) {
                        PlanActionDropoffTransfer p = (PlanActionDropoffTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    }
                }
            }
        }
        if(valid) {
            List<List<PlanAction>> itnrys = new ArrayList<>();
            itnrys.add(itnryp1);
            itnrys.add(itnryp2);
            Pair<List<List<PlanAction>>, Long> ret = new Pair<>(itnrys, transferTime);
            return ret;
        }
        else {
            return null;
        }
    }

    private Pair<List<List<PlanAction>>, Long> createChargePlanNoNewRequestsWithRounding(List<PlanAction> itnryp1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh1, RideSharingOnDemandVehicle veh2, PlanComputationRequest request) {
        long time1 = 0;
        int time1Int = 0;
        long timeToFinishEdge1 = 0;
        SimulationNode previousDestination = veh1.getPosition();

        if (veh1.getCurrentTripPlan() != null) {
            if (veh1.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) veh1.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge1 += travelTimeProvider.getTravelTime(veh1, stopLoc);
                previousDestination = stopLoc;
            }
            else if (veh1.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) veh1.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge1 += travelTimeProvider.getTravelTime(veh1, currLoc);
                    }
                    if (currLoc == veh1.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) veh1.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time1 = timeToFinishEdge1;
        time1Int += (int) Math.ceil(timeToFinishEdge1 / 1000.0);

        long transferTime = 0;

        //expected arrival time of first car
        for (PlanAction action : itnryp1) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time1Int += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest)/1000.0);
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time1Int += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest)/1000.0);
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time1Int += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest)/1000.0);
                    if (dropoffTransfer.request == request) {
                        break;
                    }
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time1Int += (int) Math.ceil(travelTimeProvider.getExpectedTravelTime(previousDestination, dest)/1000.0);
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) action;
                    time1 = time1 + wait.getWaitTime();
                    time1Int += (int) Math.ceil(wait.getWaitTime())/1000.0;
                }
            }
        }
        // expected arrival of second car
        int indexPickupSecondCar = 0;
        long time2 = 0;
        int time2Int = 0;
        long timeToFinishEdge2 = 0;
        PlanActionPickupTransfer pickup = null;
        previousDestination = veh2.getPosition();

        if (veh2.getCurrentTripPlan() != null) {
            if (veh2.getCurrentTripPlan().getSize() == 0) {
                SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
                timeToFinishEdge2 += travelTimeProvider.getTravelTime(veh2, stopLoc);
                previousDestination = stopLoc;
            } else if (veh2.getCurrentTripPlan().getSize() > 0) {
                SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
                SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
                boolean currLocIsVehiclePosition = false;
                int curridx = 0;
                while (currLoc != stopLoc) {
                    if (currLocIsVehiclePosition) {
                        timeToFinishEdge2 += travelTimeProvider.getTravelTime(veh2, currLoc);
                    }
                    if (currLoc == veh2.getPosition()) {
                        currLocIsVehiclePosition = true;
                    }
                    previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                    curridx++;
                    currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                }
            }
        }
        time2 = timeToFinishEdge2;
        time2Int += (int) Math.floor(timeToFinishEdge2/1000.0);

        for (PlanAction action : itnryp2) {
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                if (action instanceof PlanActionPickup) {
                    SimulationNode dest = pcq.getFrom();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time2Int += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    previousDestination = dest;
                } else if(action instanceof PlanActionPickupTransfer) {
                    PlanActionPickupTransfer pickupTransfer = (PlanActionPickupTransfer) action;
                    SimulationNode dest = pickupTransfer.getPosition();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time2Int += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    if (pickupTransfer.request == request) {
                        pickup = pickupTransfer;
                        break;
                    }
                    previousDestination = dest;
                } else if(action instanceof PlanActionDropoffTransfer) {
                    PlanActionDropoffTransfer dropoffTransfer = (PlanActionDropoffTransfer) action;
                    SimulationNode dest = dropoffTransfer.getPosition();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time2Int += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    previousDestination = dest;
                } else if (action instanceof PlanActionDropoff) {
                    SimulationNode dest = pcq.getTo();
                    time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                    time2Int += (int) Math.floor(travelTimeProvider.getExpectedTravelTime(previousDestination, dest) / 1000.0);
                    previousDestination = dest;
                } else if (action instanceof PlanActionWait) {
                    PlanActionWait wait = (PlanActionWait) action;
                    time2 = time2 + wait.getWaitTime();
                    time2Int += (int) Math.floor(wait.getWaitTime() / 1000.0);
                }
            }
            indexPickupSecondCar++;
        }
        long waitTime = time2 - time1;
        int waitTimeInt = time2Int - time1Int;

        boolean valid = true;

        if (waitTime > 0) {
            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), waitTimeInt * 1000);
            transferTime = waitTimeInt * 1000;
            itnryp2.add(indexPickupSecondCar, waitAction);

            long time = 0;
            previousDestination = veh2.getPosition();
            if (veh2.getCurrentTripPlan() != null) {
                if (veh2.getCurrentTripPlan().getSize() == 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
                    time += travelTimeProvider.getTravelTime(veh2, stopLoc);
                    previousDestination = stopLoc;
                }
                else if (veh2.getCurrentTripPlan().getSize() > 0) {
                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
                    SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
                    boolean currLocIsVehiclePosition = false;
                    int curridx = 0;
                    while (currLoc != stopLoc) {
                        if (currLocIsVehiclePosition) {
                            time += travelTimeProvider.getTravelTime(veh2, currLoc);
                        }
                        if (currLoc == veh2.getPosition()) {
                            currLocIsVehiclePosition = true;
                        }
                        previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                        curridx++;
                        currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
                    }
                }
            }

            for (PlanAction action : itnryp2) {
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if (action instanceof PlanActionPickup) {
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxPickupTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionDropoff) {
                        SimulationNode dest = pcq.getTo();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionWait) {
                        PlanActionWait wait = (PlanActionWait) action;
                        time = time + wait.getWaitTime();
                        previousDestination = wait.getPosition();
                    } else if (action instanceof PlanActionPickupTransfer) {
                        PlanActionPickupTransfer p = (PlanActionPickupTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    } else if(action instanceof PlanActionDropoffTransfer) {
                        PlanActionDropoffTransfer p = (PlanActionDropoffTransfer) action;
                        SimulationNode dest = p.getPosition();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        previousDestination = dest;
                    }
                }
            }
        }

//        if (waitTime > 0 && waitTime < 2000) {
//            long newWait = 2000 - waitTime;
//            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), newWait);
//            transferTime = newWait;
//            itnryp2.add(indexPickupSecondCar, waitAction);
//
//            //check tolerable delay for passengers in vehicle2
//            long time = 0;
//            previousDestination = veh2.getPosition();
//            if (veh2.getCurrentTripPlan() != null) {
//                if (veh2.getCurrentTripPlan().getSize() == 0) {
//                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
//                    time += travelTimeProvider.getTravelTime(veh2, stopLoc);
//                    previousDestination = stopLoc;
//                }
//                else if (veh2.getCurrentTripPlan().getSize() > 0) {
//                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
//                    SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
//                    boolean currLocIsVehiclePosition = false;
//                    int curridx = 0;
//                    while (currLoc != stopLoc) {
//                        if (currLocIsVehiclePosition) {
//                            time += travelTimeProvider.getTravelTime(veh2, currLoc);
//                        }
//                        if (currLoc == veh2.getPosition()) {
//                            currLocIsVehiclePosition = true;
//                        }
//                        previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
//                        curridx++;
//                        currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
//                    }
//                }
//            }
//
//            for (PlanAction action : itnryp2) {
//                if (action instanceof PlanRequestAction) {
//                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
//                    if (action instanceof PlanActionPickup) {
//                        SimulationNode dest = pcq.getFrom();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        if (!(time < pcq.getMaxPickupTime()*1000)) {
//                            valid = false;
//                            break;
//                        }
//                        previousDestination = dest;
//                    } else if (action instanceof PlanActionDropoff) {
//                        SimulationNode dest = pcq.getTo();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
//                            valid = false;
//                            break;
//                        }
//                        previousDestination = dest;
//                    } else if (action instanceof PlanActionWait) {
//                        PlanActionWait wait = (PlanActionWait) action;
//                        time = time + wait.getWaitTime();
//                        previousDestination = wait.getPosition();
//                    } else if (action instanceof PlanActionPickupTransfer) {
//                        PlanActionPickupTransfer p = (PlanActionPickupTransfer) action;
//                        SimulationNode dest = p.getPosition();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        previousDestination = dest;
//                    } else if(action instanceof PlanActionDropoffTransfer) {
//                        PlanActionDropoffTransfer p = (PlanActionDropoffTransfer) action;
//                        SimulationNode dest = p.getPosition();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        previousDestination = dest;
//                    }
//                }
//            }
//        }
//        if (waitTime <= 0) {
//            waitTime = waitTime - 2000;
//            assert pickup != null;
//            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), -waitTime);
//            transferTime = -waitTime;
//            itnryp2.add(indexPickupSecondCar, waitAction);
//
//            long time = 0;
//            previousDestination = veh2.getPosition();
//            if (veh2.getCurrentTripPlan() != null) {
//                if (veh2.getCurrentTripPlan().getSize() == 0) {
//                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getLastLocation();
//                    time += travelTimeProvider.getTravelTime(veh2, stopLoc);
//                    previousDestination = stopLoc;
//                }
//                else if (veh2.getCurrentTripPlan().getSize() > 0) {
//                    SimulationNode stopLoc = (SimulationNode) veh2.getCurrentTripPlan().getFirstLocation();
//                    SimulationNode currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[0];
//                    boolean currLocIsVehiclePosition = false;
//                    int curridx = 0;
//                    while (currLoc != stopLoc) {
//                        if (currLocIsVehiclePosition) {
//                            time += travelTimeProvider.getTravelTime(veh2, currLoc);
//                        }
//                        if (currLoc == veh2.getPosition()) {
//                            currLocIsVehiclePosition = true;
//                        }
//                        previousDestination = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
//                        curridx++;
//                        currLoc = (SimulationNode) veh2.getCurrentTripPlan().getAllLocations()[curridx];
//                    }
//                }
//            }
//
//            for (PlanAction action : itnryp2) {
//                if (action instanceof PlanRequestAction) {
//                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
//                    if (action instanceof PlanActionPickup) {
//                        SimulationNode dest = pcq.getFrom();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        if (!(time < pcq.getMaxPickupTime()*1000)) {
//                            valid = false;
//                            break;
//                        }
//                        previousDestination = dest;
//                    } else if (action instanceof PlanActionDropoff) {
//                        SimulationNode dest = pcq.getTo();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
//                            valid = false;
//                            break;
//                        }
//                        previousDestination = dest;
//                    } else if (action instanceof PlanActionWait) {
//                        PlanActionWait wait = (PlanActionWait) action;
//                        time = time + wait.getWaitTime();
//                        previousDestination = wait.getPosition();
//                    } else if (action instanceof PlanActionPickupTransfer) {
//                        PlanActionPickupTransfer p = (PlanActionPickupTransfer) action;
//                        SimulationNode dest = p.getPosition();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        previousDestination = dest;
//                    } else if(action instanceof PlanActionDropoffTransfer) {
//                        PlanActionDropoffTransfer p = (PlanActionDropoffTransfer) action;
//                        SimulationNode dest = p.getPosition();
//                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
//                        previousDestination = dest;
//                    }
//                }
//            }
//        }
        if(valid) {
            List<List<PlanAction>> itnrys = new ArrayList<>();
            itnrys.add(itnryp1);
            itnrys.add(itnryp2);
            Pair<List<List<PlanAction>>, Long> ret = new Pair<>(itnrys, transferTime);
            return ret;
        }
        else {
            return null;
        }
    }

    public List<PlanAction> getPickupActions(List<PlanAction> plan) {
        List<PlanAction> pickups = new ArrayList<>();
        for(PlanAction action : plan) {
            if(action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                pickups.add(action);
            }
        }
        return pickups;
    }

    public List<PlanAction> getDropoffActions(List<PlanAction> plan) {
        List<PlanAction> dropoffs = new ArrayList<>();
        for(PlanAction action : plan) {
            if(action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                dropoffs.add(action);
            }
        }
        return dropoffs;
    }

    private HashMap<PlanComputationRequest, Long> getEstimatedTimesOfDropoff(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
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

        HashMap<PlanComputationRequest, Long> times = new HashMap<>();
        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                if (time > ((PlanRequestAction) action).request.getMaxPickupTime() * 1000) {
                    return null;
                }
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, action.getPosition());
                if (time > ((PlanRequestAction) action).request.getMaxDropoffTime() * 1000) {
                    return null;
                }
                times.put(((PlanRequestAction) action).getRequest(), time);
                previousDestination = action.getPosition();
            } else if (action instanceof PlanActionWait) {
                time = time + ((PlanActionWait) action).getWaitTime();
            }
        }
        return times;
    }

    private List<PlanAction> findItineraryWithMinimumDelayNew(List<List<PlanAction>> lst, List<PlanAction> originalPlan, RideSharingOnDemandVehicle vehicle) {
        HashMap<PlanComputationRequest, Long> timesOfDropOriginal = getEstimatedTimesOfDropoff(originalPlan, vehicle);
        if (timesOfDropOriginal == null) {
            return null;
            //TODO throw exception
        }
        List<Long> delays = new ArrayList<>();
        for (List<PlanAction> itnry : lst) {
            HashMap<PlanComputationRequest, Long> timesOfDropNew = getEstimatedTimesOfDropoff(itnry, vehicle);
            if (timesOfDropNew == null) {
                delays.add(Long.MAX_VALUE);
            } else {
                long delay = countDelayDifference(timesOfDropOriginal, timesOfDropNew);
                delays.add(delay);
            }
        }
        int maxAt = 0;
        for (int i = 0; i < delays.size(); i++) {
            maxAt = delays.get(i) > delays.get(maxAt) ? i : maxAt;
        }
        if (delays.get(maxAt) == Long.MAX_VALUE) {
            return null;
        }
        List<PlanAction> bestPlan = lst.get(maxAt);
        return bestPlan;
    }

    private long countDelayDifference(HashMap<PlanComputationRequest, Long> originalMap, HashMap<PlanComputationRequest, Long> newMap) {
        long time = 0;
        for (Map.Entry<PlanComputationRequest, Long> entry : originalMap.entrySet()) {
            long difference = Math.abs(entry.getValue() - newMap.get(entry.getKey()));
            time = time + difference;
        }
        // todo maybe add delay for a new passenger?
        return time;
    }

    private List<PlanAction> removeCurrentPositionActions(List<PlanAction> listOfActionsWithPositions) {
        List<PlanAction> copyOfList = new ArrayList<>();
        copyOfList.addAll(listOfActionsWithPositions);
        for (PlanAction action : listOfActionsWithPositions) {
            if (action instanceof PlanActionCurrentPosition) {
                copyOfList.remove(action);
            }
        }
        return copyOfList;
    }


    private List<PlanAction> findPlanWithNoTransferNew(PlanComputationRequest newRequest, RideSharingOnDemandVehicle vehicle) {
        DriverPlan vehiclePlan;
        if (planMap.containsKey(vehicle)) {
            vehiclePlan = planMap.get(vehicle);
        } else {
            vehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        if (isWithTransfer(vehiclePlan)) {
            int indexLastTransfer = findLastTransferActionIndex(vehiclePlan);
            List<PlanAction> segmentWithTransfer = vehiclePlan.plan.subList(0, indexLastTransfer+1);
            List<PlanAction> segmentWithTransferWithoutPositionAction = removeCurrentPositionActions(segmentWithTransfer);

            List<PlanAction> segmentAfterTransfer = vehiclePlan.plan.subList(indexLastTransfer+1, vehiclePlan.plan.size());
            List<List<PlanAction>> lstTemp = new ArrayList<>();
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            for (PlanAction action : segmentAfterTransfer) {
                newPlan.add(action);
            }
            newPlan.add(newRequest.getPickUpAction());
            newPlan.add(newRequest.getDropOffAction());
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lstTemp.add(createItineraryList(pickups, dropoffPlan));
            }
            for (List<PlanAction> itnry : lstTemp) {
                List<PlanAction> newList = new ArrayList<>(segmentWithTransferWithoutPositionAction);
                newList.addAll(itnry);
                lst.add(newList);
            }
            return findItineraryWithMinimumDelayNew(lst, vehiclePlan.plan, vehicle);

        } else {
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            newPlan.addAll(vehiclePlan.plan);
            newPlan.add(newRequest.getPickUpAction());
            newPlan.add(newRequest.getDropOffAction());
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lst.add(createItineraryList(pickups, dropoffPlan));
            }
            return findItineraryWithMinimumDelayNew(lst, vehiclePlan.plan, vehicle);
        }
    }

    private List<PlanAction> findPlanWithNoTransferActionsNew(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle) {
        DriverPlan vehiclePlan;
        if (planMap.containsKey(vehicle)) {
            vehiclePlan = planMap.get(vehicle);
        } else {
            vehiclePlan = vehicle.getCurrentPlanNoUpdate();
        }
        if (isWithTransfer(vehiclePlan)) {
            int indexLastTransfer = findLastTransferActionIndex(vehiclePlan);
            List<PlanAction> segmentWithTransfer = vehiclePlan.plan.subList(0, indexLastTransfer+1);
            List<PlanAction> segmentWithTransferWithoutPositionAction = removeCurrentPositionActions(segmentWithTransfer);

            List<PlanAction> segmentAfterTransfer = vehiclePlan.plan.subList(indexLastTransfer+1, vehiclePlan.plan.size());
            List<List<PlanAction>> lstTemp = new ArrayList<>();
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            newPlan.addAll(segmentAfterTransfer);
            //add pickup and dropoff for new request
            newPlan.add(pickup);
            newPlan.add(dropoff);
            //get pickup order based on heuristic from TASeT paper
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lstTemp.add(createItineraryList(pickups, dropoffPlan));
            }
            for (List<PlanAction> itnry : lstTemp) {
                List<PlanAction> newList = new ArrayList<>(segmentWithTransferWithoutPositionAction);
                newList.addAll(itnry);
                lst.add(newList);
            }
            return findItineraryWithMinimumDelayNew(lst, vehiclePlan.plan, vehicle);

        } else {
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            newPlan.addAll(vehiclePlan.plan);
            //add pickup and dropoff for new request
            newPlan.add(pickup);
            newPlan.add(dropoff);
            //get pickup order based on heuristic from TASeT paper
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lst.add(createItineraryList(pickups, dropoffPlan));
            }
            return findItineraryWithMinimumDelayNew(lst, vehiclePlan.plan, vehicle);
        }
    }

    private List<PlanAction> createItineraryList(List<PlanAction> pickupOrder, List<PlanAction> dropoffOrder) {
        List<PlanAction> listOfActionsOrdered = new ArrayList<>(pickupOrder);
        listOfActionsOrdered.addAll(dropoffOrder);
        return listOfActionsOrdered;
    }

    /**
     * @return new list with all permutations of PlanActrions from lst List.
     */
    public List<List<PlanAction>> permute(List<PlanAction> lst) {
        List<List<PlanAction>> list = new ArrayList<>();
        permuteHelper(list, new ArrayList<>(), lst);
        return list;
    }

    /**
     * Helper function for permute()
     */
    private void permuteHelper(List<List<PlanAction>> list, List<PlanAction> resultList, List<PlanAction> lst){
        // Base case
        if(resultList.size() == lst.size()){
            list.add(new ArrayList<>(resultList));
        }
        else{
            for(int i = 0; i < lst.size(); i++){
                if(resultList.contains(lst.get(i)))
                {
                    // If element already exists in the list then skip
                    continue;
                }
                // Choose element
                resultList.add(lst.get(i));
                // Explore
                permuteHelper(list, resultList, lst);
                // Unchoose element
                resultList.remove(resultList.size() - 1);
            }
        }
    }

    private boolean canServeRequestTASeT2(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
        DriverPlan actualPlan = vehicle.getCurrentPlanNoUpdate();
        for(PlanAction action : actualPlan) {
            if(action instanceof PlanRequestAction) {
                PlanRequestAction requestAction = (PlanRequestAction) action;
                requestsOnBoardSet.add(requestAction.request);
            }
        }
        List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);
        boolean taxiFree = true;
        int taxiCapacity = vehicle.getCapacity();
        if (requestsOnBoardSet.size() >= taxiCapacity) {
            taxiFree = false;
        }
        if (!taxiFree) {
            return false;
        }
        else {
            if (requestsOnBoard.size() == 0) {
                long timeToNewRequest = travelTimeProvider.getTravelTime(vehicle, request.getFrom());
                if (timeToNewRequest <= request.getMaxPickupTime() * 1000) {
                    return true;
                }
                return false;
            }
            if (isWithTransfer(vehicle.getCurrentPlanNoUpdate())) {
                int indexLastTransferAction = findLastTransferActionIndex(vehicle.getCurrentPlanNoUpdate());
                long timeToFinishCurrentEdge = 0;
                SimulationNode previousPos = vehicle.getPosition();
                if (vehicle.getCurrentTask() != null) {
                    timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(vehicle, vehicle.getCurrentTask().getPosition());
                    previousPos = vehicle.getCurrentTask().getPosition();
                }
                long timeToLastTransferAction = 0;
                for (int q = 0; q < indexLastTransferAction + 1; q++) {
                    if (vehicle.getCurrentPlanNoUpdate().plan.get(q) instanceof PlanActionWait) {
                        PlanActionWait wait = (PlanActionWait) vehicle.getCurrentPlanNoUpdate().plan.get(q);
                        timeToLastTransferAction = timeToLastTransferAction + wait.getWaitTime();
                    } else {
                        timeToLastTransferAction = timeToLastTransferAction + travelTimeProvider.getExpectedTravelTime(previousPos, vehicle.getCurrentPlanNoUpdate().plan.get(q).getPosition());
                    }
                    previousPos = vehicle.getCurrentPlanNoUpdate().plan.get(q).getPosition();
                }
                List<PlanAction> segmentAfterTransfer = vehicle.getCurrentPlanNoUpdate().plan.subList(indexLastTransferAction+1, vehicle.getCurrentPlanNoUpdate().plan.size());
                int indexLastPickupSegment = findLastPickupIndexList(segmentAfterTransfer);
                long timeToLastPickup = 0;
                SimulationNode previousPos2 = vehicle.getCurrentPlanNoUpdate().plan.get(vehicle.getCurrentPlanNoUpdate().plan.size()-1).getPosition();
                if (segmentAfterTransfer.size() > 0) {
                    previousPos2 = segmentAfterTransfer.get(0).getPosition();
                    for (int q = 0; q < indexLastPickupSegment + 1; q++) {
                        timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos2, segmentAfterTransfer.get(q).getPosition());
                        previousPos2 = segmentAfterTransfer.get(q).getPosition();
                    }
                }
                long timeToNewRequest = travelTimeProvider.getExpectedTravelTime(previousPos2, request.getFrom());
                long estimatedArrival = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToNewRequest;
                if (estimatedArrival <= request.getMaxPickupTime() * 1000) {
                     return true;
                } else {
                    return false;
                }
            }
            else {
                int indexLastPickup = findLastPickupIndex(vehicle.getCurrentPlanNoUpdate());
                long timeToFinishCurrentEdge = 0;
                SimulationNode previousPos = vehicle.getPosition();
                if (vehicle.getCurrentTask() != null) {
                    timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(vehicle, vehicle.getCurrentTask().getPosition());
                    previousPos = vehicle.getCurrentTask().getPosition();
                }
                long timeToLastPickup = 0;
                for (int q = 0; q < indexLastPickup + 1; q++) {
                    timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos, vehicle.getCurrentPlanNoUpdate().plan.get(q).getPosition());
                    previousPos = vehicle.getCurrentPlanNoUpdate().plan.get(q).getPosition();
                }
                long timeToNewRequest = travelTimeProvider.getExpectedTravelTime(previousPos, request.getFrom());
                long estimatedArrival = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToNewRequest;
                if (estimatedArrival <= request.getMaxPickupTime() * 1000) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}

