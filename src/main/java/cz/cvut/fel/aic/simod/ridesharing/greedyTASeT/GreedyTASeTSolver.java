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

    private final double maxDistance = 100;

    private final double maxDistanceSquared = 10000;

    private final int maxDelayTime = 10;

    private List<SimulationNode> transferPoints;

    protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;




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

//        // max distance in meters between vehicle and request for the vehicle to be considered to serve the request
//        maxDistance = (double) config.ridesharing.maxProlongationInSeconds
//                * agentpolisConfig.maxVehicleSpeedInMeters;
//        maxDistanceSquared = maxDistance * maxDistance;
//
//        // the traveltime from vehicle to request cannot be greater than max prolongation in milliseconds for the
//        // vehicle to be considered to serve the request
//        maxDelayTime = config.ridesharing.maxProlongationInSeconds  * 1000;

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
        List<RideSharingOnDemandVehicle> taxis = new ArrayList<>();

        for(AgentPolisEntity tVvehicle: vehicleStorage.getEntitiesForIteration()) {
            RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
            taxis.add(vehicle);
        }

        List<RideSharingOnDemandVehicle> vehiclesWithPlans = dispatch(taxis, newRequests);

        for (int i = 0; i < vehiclesWithPlans.size(); i++) {
            planMap.put(vehiclesWithPlans.get(i), vehiclesWithPlans.get(i).getCurrentPlanNoUpdate());
        }

        return planMap;
    }

    /**
     * Transfer-allowed scheduling function
     * @return
     */
    private List<RideSharingOnDemandVehicle> dispatch(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        // because all passengers allow ridesharing, only greedy taset will be called
        List<RideSharingOnDemandVehicle> carpoolAcceptingTaxis = taxis;
        List<PlanComputationRequest> carpoolAcceptingPassengers = requests;
        List<RideSharingOnDemandVehicle> lst2 = heuristics(carpoolAcceptingTaxis, carpoolAcceptingPassengers);
        return lst2;
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
            SimulationNode newPickupFrom = request.getFrom();
            long timeToNewPick = travelTimeProvider.getExpectedTravelTime(previousPos, newPickupFrom);
            long estimatedArrivalToPickup = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToNewPick;
            // zkontrolovat, zda arrival je v pohode z hlediska delay - kontroluju pro requesty po ukonceni prestupu
            Set<PlanComputationRequest> requestsInSegmentSet = new HashSet<>();
            for(PlanAction action : segmentAfterTransfer) {
                if(action instanceof PlanRequestAction) {
                    PlanRequestAction requestAction = (PlanRequestAction) action;
                    requestsInSegmentSet.add(requestAction.request);
                }
            }
            List<PlanComputationRequest> requestsInSegment = new ArrayList<>(requestsInSegmentSet);
            for (PlanComputationRequest req : requestsInSegment) {
                long maxTime = req.getMaxDropoffTime() * 1000;
                if (estimatedArrivalToPickup > maxTime) {
                    return Long.MAX_VALUE;
                }
            }
            return estimatedArrivalToPickup;
        }
        else {
            //neprestupuje nikdo
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
            SimulationNode newPickupFrom = request.getFrom();
            long timeToNewPick = travelTimeProvider.getExpectedTravelTime(previousPos, newPickupFrom);
            long estimatedArrivalToNewPickup = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToNewPick;
            // zkontrolovat, zda arrival je v pohode z hlediska delay
            for (PlanComputationRequest req : requestsOnBoard) {
                long maxTime = request.getMaxDropoffTime() * 1000;
                if (estimatedArrivalToNewPickup > maxTime) {
                    return Long.MAX_VALUE;
                }
            }
            return estimatedArrivalToNewPickup;
        }
    }

    /**
     * Greedy TASeT heuristics function
     * @return
     */
    private List<RideSharingOnDemandVehicle> heuristics(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //transfer points = charging stations
        List<SimulationNode> transferPoints = this.transferPoints;

        //lookup table LT - LT [t][k] stores the earliest arrival time for taxi k to charging station t without violating the tolerable delay for k’s current passengers
        int stationsCount = transferPoints.size();
        int taxisCount = taxis.size();
        long[][] LT = new long[stationsCount][taxisCount];
        //fill the LT table
        for (int i = 0; i < taxisCount; i++) {
            RideSharingOnDemandVehicle taxi = taxis.get(i);
            SimulationNode taxiPosition = taxis.get(i).getPosition();
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
                        // zkontrolovat, zda arrival je v pohode z hlediska delay - kontroluju pro requesty po ukonceni prestupu
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
                        //neprestupuje nikdo
                        int indexLastPickup = findLastPickupIndex(taxi.getCurrentPlanNoUpdate());
                        long timeToFinishCurrentEdge = 0;
                        SimulationNode previousPos = taxi.getPosition();
                        if (taxi.getCurrentTask() != null) {
                            timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                            previousPos = taxi.getCurrentTask().getPosition();
                        }
//                        long timeToFinishCurrentEdge = travelTimeProvider.getTravelTime(taxi, taxi.getCurrentTask().getPosition());
                        long timeToLastPickup = 0;
//                        SimulationNode previousPos = taxi.getCurrentTask().getPosition();
                        for (int q = 0; q < indexLastPickup + 1; q++) {
                            timeToLastPickup = timeToLastPickup + travelTimeProvider.getExpectedTravelTime(previousPos, taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition());
                            previousPos = taxi.getCurrentPlanNoUpdate().plan.get(q).getPosition();
                        }
                        SimulationNode station = transferPoints.get(j);
                        long timeToStation = travelTimeProvider.getExpectedTravelTime(previousPos, station);
                        long estimatedArrivalToStation = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastPickup + timeToStation;
                        LT[j][i] = estimatedArrivalToStation;
                        // zkontrolovat, zda arrival je v pohode z hlediska delay
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


        for(PlanComputationRequest request : requests) {
            List<Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>> templistP = new ArrayList<>();
            // list ve kterem je list dvojic - list dvojic, protoze dvojice muze byt jen jedna (neni prestup) nebo dve (je prestup)
            List<Long> delays = new ArrayList<>();
            List<Long> transferTimes = new ArrayList<>();
            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            for (RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                List<PlanAction> posbitnry = findPlanWithNoTransferNew(request, taxi);
                // pokud neexistuje ani jeden validni itinerar, tak je posbitnry null
                // tehdy ho nebudu pridavat do templistu
                if (posbitnry != null) {
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
                Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
                DriverPlan actualPlan = taxi.getCurrentPlanNoUpdate();
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
                    // je stanice potencialne vhodna pro prestup?
                    if (timeToNewPickup + travelTimeProvider.getExpectedTravelTime(request.getFrom(), station) >
                            request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                        continue;
                    }

                    // find k' = taxis that taxi k can transfer to at station
                    for (int k = 0; k < taxisCount; k++) {
                        // minimalni cas na druhy usek
                        int minimalTimeFromStation = (int) Math.round(travelTimeProvider.getExpectedTravelTime(station, request.getFrom()) / 1000.0);
                        //not possible to transfer to
                        if (LT[stationIndex][k] == Long.MAX_VALUE) {
                            continue;
                        } else if(LT[stationIndex][k] > request.getMaxDropoffTime() * 1000 - travelTimeProvider.getExpectedTravelTime(station, request.getTo())) {
                            continue;
                        } else if(taxi.equals(taxis.get(k))) {
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
                                // neexistuje plan
                                continue;
                            }
                            Pair<List<List<PlanAction>>, Long> p = createChargePlanNoNewRequests(itnryp1, itnryp2, taxi, taxis.get(k), request);
                            if (p == null) {
                                // neni zadny validni plan a tedy neni mozne prestoupit, takze neudelam nic
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
                                // travel time daneho requestu s prestupem spocitam jako:
                                // cas nez prvni auto dojede pro request a vyzvedne ho
                                // + cas jizdy v prvnim vozidle
                                // + pokud druhe auto prijede pozdeji nez to prvni tak k tomu prictu rozdil
                                // + doba jizdy v druhem aute
                                HashMap<PlanComputationRequest, Long> drops = getEstimatedTimesOfDropoff(itnryp2, taxis.get(k));
                                if (drops == null) {
                                    // not valid
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

            //vezmu hornich beta procent
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

            // ted mam serazene transferTimes
            if (sublistTransferPlans.isEmpty()) {
                continue;
            }
            else
            {
                // ziskam entry ktery je nejlepsi podle heuristiky
                Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> key = sublistTransferPlans.get(0).pair;
                List<List<PlanAction>> plansForVehicles = key.getFirst();
                List<RideSharingOnDemandVehicle> vehicles = key.getSecond();
                for (int q = 0; q < vehicles.size(); q++) {
                    List<PlanAction> vehPlan = plansForVehicles.get(q);
                    DriverPlan dp = new DriverPlan(vehPlan, 0, 0);
                    vehicles.get(q).setCurrentPlan(dp);
                }
            }

            // update k, k′ and LT
            //update LT - not efficient
            for (int z = 0; z < taxisCount; z++) {
                RideSharingOnDemandVehicle taxi = taxis.get(z);
                SimulationNode taxiPosition = taxis.get(z).getPosition();
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
                        LT[j][z] = Long.MAX_VALUE;
                    } else {
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
                            List<PlanAction> segmentAfterTransfer = taxi.getCurrentPlanNoUpdate().plan.subList(indexLastTransferAction + 1, taxi.getCurrentPlanNoUpdate().plan.size());
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
                            LT[j][z] = estimatedArrivalToStation;
                            // zkontrolovat, zda arrival je v pohode z hlediska delay - kontroluju pro requesty po ukonceni prestupu
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
                            //neprestupuje nikdo
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
                            LT[j][z] = estimatedArrivalToStation;
                            // zkontrolovat, zda arrival je v pohode z hlediska delay
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
        return taxis;
    }

     private long getTravelTime(PlanComputationRequest request, List<PlanAction> planOfCar) {
        long time = 0;
        int index = 0;
        SimulationNode previousPosition = planOfCar.get(0).getPosition();
        PlanAction lastAction = null;
        // find first action
        for (int i = 0; i < planOfCar.size(); i++) {
            PlanAction action = planOfCar.get(i);
            if (action instanceof PlanRequestAction) {
                PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                previousPosition = action.getPosition();
                if (pcq == request) {
                    index = i;
                    break;
                }
            }
        }
        //find last action
         for (int i = planOfCar.size()-1; i > 0; i--) {
             PlanAction action = planOfCar.get(i);
             if (action instanceof PlanRequestAction) {
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (pcq == request) {
                     lastAction = action;
                     break;
                 }
             }
         }
         for (int i = index+1; i < planOfCar.size(); i++) {
            PlanAction action = planOfCar.get(i);
            time = time + travelTimeProvider.getExpectedTravelTime(previousPosition, action.getPosition());
            previousPosition = action.getPosition();
            if (action == lastAction) {
                break;
            }
         }
         return time;
     }

     private List<PlanAction> splittedRequestToPlanForRequest(List<PlanAction> itnryp1, List<PlanAction> itnryp2, PlanComputationRequest newRequest1,
                                                              PlanComputationRequest newRequest2, PlanComputationRequest originalRequest) {
        List<PlanAction> listForOriginalRequest = new ArrayList<>();
        // iterate over first itnryp
         //find actions that belongs to newrequest1
         //create similar action with originalrequest
         //add to list
         //do the same with the second itnryp
         for (PlanAction action : itnryp1) {
             if (action instanceof PlanRequestAction) {
                 PlanRequestAction requestAction = (PlanRequestAction) action;
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     if (pcq.getPickUpAction().request == newRequest1) {
//                         PlanActionPickup pickup = (PlanActionPickup) pcq;
                         PlanActionPickup newPickup = new PlanActionPickup(originalRequest, action.getPosition(), requestAction.getMaxTime());
                         listForOriginalRequest.add(newPickup);
                     }
                 } else if (action instanceof PlanActionDropoff) {
                     if (pcq.getDropOffAction().request == newRequest1) {
//                         PlanActionDropoff dropoff = (PlanActionDropoff) pcq;
                         PlanActionDropoff newDropoff = new PlanActionDropoff(originalRequest, action.getPosition(), requestAction.getMaxTime());
                         listForOriginalRequest.add(newDropoff);
                     }
                 }
             }
         }
         for (PlanAction action : itnryp2) {
             if (action instanceof PlanRequestAction) {
                 PlanRequestAction requestAction = (PlanRequestAction) action;
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     if (pcq.getPickUpAction().request == newRequest2) {
//                         PlanActionPickup pickup = (PlanActionPickup) pcq;
                         PlanActionPickup newPickup = new PlanActionPickup(originalRequest, action.getPosition(), requestAction.getMaxTime());
                         listForOriginalRequest.add(newPickup);
                     }
                 } else if (action instanceof PlanActionDropoff) {
                     if (pcq.getDropOffAction().request == newRequest2) {
//                         PlanActionDropoff dropoff = (PlanActionDropoff) pcq;
                         PlanActionDropoff newDropoff = new PlanActionDropoff(originalRequest, action.getPosition(), requestAction.getMaxTime());
                         listForOriginalRequest.add(newDropoff);
                     }
                 }
             }
         }

        return listForOriginalRequest;

     }

    private Pair<List<List<PlanAction>>, Long> createChargePlanNoNewRequests(List<PlanAction> itnryp1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh1, RideSharingOnDemandVehicle veh2, PlanComputationRequest request) {
        long time1 = 0;
        long timeToFinishEdge1 = 0;
        SimulationNode previousDestination = veh1.getPosition();
        if (veh1.getCurrentTask() != null) {
            timeToFinishEdge1 = travelTimeProvider.getTravelTime(veh1, veh1.getCurrentTask().getPosition());
            previousDestination = veh1.getCurrentTask().getPosition();
        }
        time1 = timeToFinishEdge1;
        long time2 = 0;
        long timeToFinishEdge2 = 0;
        if (veh2.getCurrentTask() != null) {
            timeToFinishEdge2 = travelTimeProvider.getTravelTime(veh2, veh2.getCurrentTask().getPosition());
        }
        time2 = timeToFinishEdge2;
        // nemusim pricitat current sim time, protoze budu od sebe oba casy odecitat, jde mi jen o jejich rozdil
        long transferTime = 0;
        //expected arrival time of first car
        int indexDropoffFirstCar = 0;
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
                indexDropoffFirstCar++;
            }
        }
        // expected arrival of second car
        int indexPickupSecondCar = 0;
        PlanActionPickupTransfer pickup = null;
        previousDestination = veh2.getPosition();
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
        long waitTime = time2 - time1; // k wait time prictu navic rezervu

        // pokud je zaporny, tak druhe auto bude muset cekat waitTime dlouho
        // pokud je kladny, tak to znamena ze prvni auto prijede drive nez druhe - bude cekat cestujici

        boolean valid = true;
        // pridam wait time do planu pro druhe auto pokud je wait time zaporny
        if (waitTime < 0) {
            waitTime = waitTime - 5000;
            //transfer time je -waitTime
            PlanActionWait waitAction = new PlanActionWait(request, pickup.getPosition(), pickup.getMaxTime(), -waitTime);
            transferTime = -waitTime;
            itnryp2.add(indexPickupSecondCar, waitAction);

            //check tolerable delay for passengers in vehicle2
            long time = 0;
            time = timeToFinishEdge2;
            previousDestination = veh2.getPosition();
            if (veh2.getCurrentTask() != null) {
                previousDestination = veh2.getCurrentTask().getPosition();
            }
            for (PlanAction action : itnryp2) {
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if (action instanceof PlanActionPickup) {
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxPickupTime()*1000)) {
                            //not valid itinerary - check new driver plan
                            valid = false;
                            break;
                        }
                        previousDestination = dest;
                    } else if (action instanceof PlanActionDropoff) {
                        SimulationNode dest = pcq.getTo();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if (!(time < pcq.getMaxDropoffTime()*1000)) {
                            //not valid itinerary - check new driver plan
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

     private Map<List<List<PlanAction>>, Long> createChargePlan(List<PlanAction> itnryp1, List<PlanAction> itnryp2, RideSharingOnDemandVehicle veh1, RideSharingOnDemandVehicle veh2,
                                   DefaultPlanComputationRequest request1, DefaultPlanComputationRequest request2) {
        long time1 = 0;
        long time2 = 0;
        long transferTime = 0;
        SimulationNode previousDestination = veh1.getPosition();
         //expected arrival time of first car
         for (PlanAction action : itnryp1) {
             if (action instanceof PlanRequestAction) {
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     SimulationNode dest = pcq.getFrom();
                     time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                     previousDestination = dest;
                 } else if (action instanceof PlanActionDropoff) {
                     SimulationNode dest = pcq.getTo();
                     time1 = time1 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                     if (pcq.getDropOffAction().request == request1) {
                         break;
                     }
                     previousDestination = dest;
                 } else if (action instanceof PlanActionWait) {
                     PlanActionWait wait = (PlanActionWait) action;
                     time1 = time1 + wait.getWaitTime();
                 }
             }
         }
         // expected arrival of second car
         int indexPickupSecondCar = 0;
         PlanActionPickup pickup = null;
         previousDestination = veh2.getPosition();
         for (PlanAction action : itnryp2) {
             if (action instanceof PlanRequestAction) {
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     SimulationNode dest = pcq.getFrom();
                     time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                     if (pcq.getPickUpAction().request == request2) {
                         pickup = pcq.getPickUpAction();
                         break;
                     }
                     previousDestination = dest;
                 } else if (action instanceof PlanActionDropoff) {
                     SimulationNode dest = pcq.getTo();
                     time2 = time2 + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                     previousDestination = dest;
                 } else if (action instanceof PlanActionWait) {
                     PlanActionWait wait = (PlanActionWait) action;
                     time2 = time2 + wait.getWaitTime();
                 }
                 indexPickupSecondCar++;
             }
         }
         long waitTime = time2 - time1;
         // pokud je zaporny, tak druhe auto bude muset cekat waitTime dlouho
         // pokud je kladny, tak to znamena ze prvni auto prijede drive nez druhe - bude cekat cestujici

         boolean valid = true;
         // pridam wait time do planu pro druhe auto pokud je wait time zaporny
         if (waitTime <= 0) {
             //transfer time je -waitTime
             PlanActionWait waitAction = new PlanActionWait(request2, pickup.getPosition(), pickup.getMaxTime(), -waitTime);
             transferTime = -waitTime;
             itnryp2.add(indexPickupSecondCar, waitAction);

             //check tolerable delay for passengers in vehicle2
             long time = 0;
             for (PlanAction action : itnryp2) {
                 if (action instanceof PlanRequestAction) {
                     PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                     if (action instanceof PlanActionPickup) {
                         SimulationNode dest = pcq.getFrom();
                         time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                         if (!(time < pcq.getMaxPickupTime()*1000)) {
                             //not valid itinerary - check new driver plan
                             valid = false;
                             break;
                         }
                         previousDestination = dest;
                     } else if (action instanceof PlanActionDropoff) {
                         SimulationNode dest = pcq.getTo();
                         time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                         if (!(time < pcq.getMaxDropoffTime()*1000)) {
                             //not valid itinerary - check new driver plan
                             valid = false;
                             break;
                         }
                         previousDestination = dest;
                     } else if (action instanceof PlanActionWait) {
                         PlanActionWait wait = (PlanActionWait) action;
                         time = time + wait.getWaitTime();
                     }
                 }
             }
         }
         if(valid) {
             List<List<PlanAction>> itnrys = new ArrayList<>();
             itnrys.add(itnryp1);
             itnrys.add(itnryp2);
             Map<List<List<PlanAction>>, Long> map = new LinkedHashMap<>();
             map.put(itnrys, transferTime);
             return map;
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

    /**
     * Find DriverPlan with smallest delay without transfer allowed.
     * @return valid DriverPlan with smallest delay.
     */
    private List<PlanAction> findPlanWithNoTransfer(PlanComputationRequest newRequest, RideSharingOnDemandVehicle taxi) {
        List<DriverPlan> lst = new ArrayList<>();
        List<PlanAction> currentPlan = taxi.getCurrentPlanNoUpdate().plan;
        List<PlanAction> newPlan = new ArrayList<>();
        for (PlanAction action : currentPlan) {
            newPlan.add(action);
        }
        //add pickup and dropoff for new request
        newPlan.add(newRequest.getPickUpAction());
        newPlan.add(newRequest.getDropOffAction());
        //get pickup order based on heuristic from TASeT paper
        List<PlanAction> pickups = getPickupActions(newPlan);
        //get dropoff actions in currentPlan
        List<PlanAction> dropoffs = getDropoffActions(newPlan);
        //permute dropoff orders
        // TODO fix tady se mi ztrati wait akce, pokud tam nejake jsou!
        List<List<PlanAction>> dropoffOrders = permute(dropoffs);
        for (List<PlanAction> dropoffPlan : dropoffOrders) {
            lst.add(createItinerary(pickups, dropoffPlan));
        }
        List<PlanAction> bestPlan = findItineraryWithMinimumDelay(lst);
        return bestPlan;
    }

    private List<PlanAction> findPlanWithNoTransferActions(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle taxi) {
        List<DriverPlan> lst = new ArrayList<>();
        List<PlanAction> currentPlan = taxi.getCurrentPlanNoUpdate().plan;
        List<PlanAction> newPlan = new ArrayList<>();
        for (PlanAction action : currentPlan) {
            newPlan.add(action);
        }

        //add pickup and dropoff for new request
        newPlan.add(pickup);
        newPlan.add(dropoff);
        //get pickup order based on heuristic from TASeT paper
        List<PlanAction> pickups = getPickupActions(newPlan);
        //get dropoff actions in currentPlan
        List<PlanAction> dropoffs = getDropoffActions(newPlan);
        //permute dropoff orders
        List<List<PlanAction>> dropoffOrders = permute(dropoffs);
        for (List<PlanAction> dropoffPlan : dropoffOrders) {
            lst.add(createItinerary(pickups, dropoffPlan));
        }
        List<PlanAction> bestPlan = findItineraryWithMinimumDelay(lst);
        return bestPlan;
    }

    private HashMap<PlanComputationRequest, Long> getEstimatedTimesOfDropoff(List<PlanAction> itinerary, RideSharingOnDemandVehicle vehicle) {
        long time = timeProvider.getCurrentSimTime();
        long timeToFinishEdge = travelTimeProvider.getTravelTime(vehicle, itinerary.get(0).getPosition());
        time = time + timeToFinishEdge;
        HashMap<PlanComputationRequest, Long> times = new HashMap<>();
        SimulationNode previousPosition = itinerary.get(0).getPosition();
        for (PlanAction action : itinerary) {
            if (action instanceof PlanActionPickup || action instanceof PlanActionPickupTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousPosition, action.getPosition());
                if (time > ((PlanRequestAction) action).request.getMaxPickupTime() * 1000) {
                    //not valid
                    return null;
                }
            } else if (action instanceof PlanActionDropoff || action instanceof PlanActionDropoffTransfer) {
                time = time + travelTimeProvider.getExpectedTravelTime(previousPosition, action.getPosition());
                if (time > ((PlanRequestAction) action).request.getMaxDropoffTime() * 1000) {
                    //not valid
                    return null;
                }
                times.put(((PlanRequestAction) action).getRequest(), time);
            } else if (action instanceof PlanActionWait) {
                time = time + ((PlanActionWait) action).getWaitTime();
            }
            previousPosition = action.getPosition();
        }
        return times;
    }

    private List<PlanAction> findItineraryWithMinimumDelayNew(List<List<PlanAction>> lst, List<PlanAction> originalPlan, RideSharingOnDemandVehicle vehicle) {
        HashMap<PlanComputationRequest, Long> timesOfDropOriginal = getEstimatedTimesOfDropoff(originalPlan, vehicle);
        if (timesOfDropOriginal == null) {
            //plan neni validni
            // to je chyba, puvodni plan by mel byt validni vzdy
            return null;
            //TODO throw exception
        }
        List<Long> delays = new ArrayList<>();
        for (List<PlanAction> itnry : lst) {
            HashMap<PlanComputationRequest, Long> timesOfDropNew = getEstimatedTimesOfDropoff(itnry, vehicle);
            if (timesOfDropNew == null) {
                //plan neni validni
                delays.add(Long.MAX_VALUE);
            } else {
                // plan je validni, spocitam zpozdeni
                long delay = countDelayDifference(timesOfDropOriginal, timesOfDropNew);
                delays.add(delay);
            }
        }
        //find max in delays
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
        if (isWithTransfer(vehicle.getCurrentPlanNoUpdate())) {
            // v aute nekdo prestupuje
            // musim oddelit segment s prestupem
            // ze zbytku vezmu pickups, pridam novy a potom hledam permutace dropoff akci
            int indexLastTransfer = findLastTransferActionIndex(vehicle.getCurrentPlanNoUpdate());
            List<PlanAction> segmentWithTransfer = vehicle.getCurrentPlanNoUpdate().plan.subList(0, indexLastTransfer+1);
            List<PlanAction> segmentWithTransferWithoutPositionAction = removeCurrentPositionActions(segmentWithTransfer);

            List<PlanAction> segmentAfterTransfer = vehicle.getCurrentPlanNoUpdate().plan.subList(indexLastTransfer+1, vehicle.getCurrentPlanNoUpdate().plan.size());
            List<List<PlanAction>> lstTemp = new ArrayList<>();
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            for (PlanAction action : segmentAfterTransfer) {
                newPlan.add(action);
            }
            //add pickup and dropoff for new request
            newPlan.add(newRequest.getPickUpAction());
            newPlan.add(newRequest.getDropOffAction());
            //get pickup order based on heuristic from TASeT paper
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lstTemp.add(createItineraryList(pickups, dropoffPlan));
            }
            // ted spojit puvodni segment a kadzy itinerare z lst
            for (List<PlanAction> itnry : lstTemp) {
                List<PlanAction> newList = new ArrayList<>(segmentWithTransferWithoutPositionAction);
                newList.addAll(itnry);
                lst.add(newList);
            }
            return findItineraryWithMinimumDelayNew(lst, vehicle.getCurrentPlanNoUpdate().plan, vehicle);

        } else {
            //neni prestup v aute
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            for (PlanAction action : vehicle.getCurrentPlanNoUpdate().plan) {
                newPlan.add(action);
            }
            //add pickup and dropoff for new request
            newPlan.add(newRequest.getPickUpAction());
            newPlan.add(newRequest.getDropOffAction());
            //get pickup order based on heuristic from TASeT paper
            List<PlanAction> pickups = getPickupActions(newPlan);
            List<PlanAction> dropoffs = getDropoffActions(newPlan);
            //permute dropoff orders
            List<List<PlanAction>> dropoffOrders = permute(dropoffs);
            for (List<PlanAction> dropoffPlan : dropoffOrders) {
                lst.add(createItineraryList(pickups, dropoffPlan));
            }
            return findItineraryWithMinimumDelayNew(lst, vehicle.getCurrentPlanNoUpdate().plan, vehicle);
        }
    }

    private List<PlanAction> findPlanWithNoTransferActionsNew(PlanAction pickup, PlanAction dropoff, RideSharingOnDemandVehicle vehicle) {
        if (isWithTransfer(vehicle.getCurrentPlanNoUpdate())) {
            // v aute nekdo prestupuje
            // musim oddelit segment s prestupem
            // ze zbytku vezmu pickups, pridam novy a potom hledam permutace dropoff akci
            int indexLastTransfer = findLastTransferActionIndex(vehicle.getCurrentPlanNoUpdate());
            List<PlanAction> segmentWithTransfer = vehicle.getCurrentPlanNoUpdate().plan.subList(0, indexLastTransfer+1);
            List<PlanAction> segmentWithTransferWithoutPositionAction = removeCurrentPositionActions(segmentWithTransfer);

            List<PlanAction> segmentAfterTransfer = vehicle.getCurrentPlanNoUpdate().plan.subList(indexLastTransfer+1, vehicle.getCurrentPlanNoUpdate().plan.size());
            List<List<PlanAction>> lstTemp = new ArrayList<>();
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            for (PlanAction action : segmentAfterTransfer) {
                newPlan.add(action);
            }
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
            // ted spojit puvodni segment a kadzy itinerare z lst
            for (List<PlanAction> itnry : lstTemp) {
                List<PlanAction> newList = new ArrayList<>(segmentWithTransferWithoutPositionAction);
                newList.addAll(itnry);
                lst.add(newList);
            }
            return findItineraryWithMinimumDelayNew(lst, vehicle.getCurrentPlanNoUpdate().plan, vehicle);

        } else {
            //neni prestup v aute
            List<List<PlanAction>> lst = new ArrayList<>();
            List<PlanAction> newPlan = new ArrayList<>();
            for (PlanAction action : vehicle.getCurrentPlanNoUpdate().plan) {
                newPlan.add(action);
            }
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
            return findItineraryWithMinimumDelayNew(lst, vehicle.getCurrentPlanNoUpdate().plan, vehicle);
        }
    }

    private List<PlanAction> getActionsForRequestFromActionsForDriver(List<PlanAction> planActionsVehicle, PlanComputationRequest request, RideSharingOnDemandVehicle vehicle) {
        List<PlanAction> actionsForRequest = new ArrayList<>();
        for (int i = 0; i < planActionsVehicle.size(); i++) {
            PlanAction action = planActionsVehicle.get(i);
            if (action instanceof PlanRequestAction) {
                PlanRequestAction planRequestAction = (PlanRequestAction) action;
                if(request == planRequestAction.getRequest()) {
                    if (action instanceof PlanActionPickup) {
                        PlanActionOnboard onboard = new PlanActionOnboard(request, action.getPosition(), planRequestAction.getMaxTime(), vehicle);
                        int index = 0;
                        if (actionsForRequest.size() != 0) {
                            PlanAction currentAction = actionsForRequest.get(0);
                            PlanRequestAction currentRAction = (PlanRequestAction) currentAction;
                            while(currentRAction.getMaxTime() <= onboard.getMaxTime()) {
                                index++;
                                if (index < actionsForRequest.size()) {
                                    currentAction = actionsForRequest.get(index);
                                    currentRAction = (PlanRequestAction) currentAction;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        actionsForRequest.add(index, onboard);
                    }
                    if (action instanceof PlanActionDropoff) {
                        PlanActionDropoff dropoff = new PlanActionDropoff(request, action.getPosition(), planRequestAction.getMaxTime());
                        int index = 0;
                        if (actionsForRequest.size() != 0) {
                            PlanAction currentAction = actionsForRequest.get(0);
                            PlanRequestAction currentRAction = (PlanRequestAction) currentAction;
                            while(currentRAction.getMaxTime() <= dropoff.getMaxTime()) {
                                index++;
                                if (index < actionsForRequest.size()) {
                                    currentAction = actionsForRequest.get(index);
                                    currentRAction = (PlanRequestAction) currentAction;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        actionsForRequest.add(index, dropoff);
                    }
                }
            }
        }
        return actionsForRequest;
    }

    // neni dodelana ale nepouzivam ji
    private List<RequestPlan> convertDriverPlansToRequestPlans(List<DriverPlan> driverPlans, List<PlanComputationRequest> requests) {
        int lenRequests = requests.size();
        List<RequestPlan> requestPlans = new ArrayList<>();
        List<PlanAction> emptyPlan = new ArrayList<>();
        RequestPlan empty = new RequestPlan(emptyPlan, 0, 0);
        for (int i = 0; i < lenRequests; i++) {
            requestPlans.add(empty);
        }

        for(DriverPlan driverPlan : driverPlans) {
            for (int i = 0; i < driverPlan.plan.size(); i++) {
                PlanAction action = driverPlan.plan.get(i);
                PlanRequestAction rAction = (PlanRequestAction) action;
                PlanComputationRequest requestAssigned = rAction.getRequest();
                for(int j = 0; j < requests.size(); j++) {
                    if(requestAssigned == requests.get(j))
                    {
                        if (action instanceof PlanActionPickup) {
                            PlanActionOnboard planActionOnboard = new PlanActionOnboard(requestAssigned, action.getPosition(), rAction.getMaxTime(),  driverPlan.getVehicle());
//                             iterate over existing actions and find a timestamp HOPEFULLY DONE
                            PlanAction currentAction = requestPlans.get(j).plan.get(0);
                            PlanRequestAction currentRAction = (PlanRequestAction) currentAction;
                            int index = 0;
                            while(currentRAction.getMaxTime() <= planActionOnboard.getMaxTime()) {
                                index++;
                                currentAction = requestPlans.get(j).plan.get(index);
                                currentRAction = (PlanRequestAction) currentAction;
                            }
                            requestPlans.get(j).plan.add(index, planActionOnboard);
                        } else if (action instanceof PlanActionDropoff) {
                            PlanActionOffboard planActionOffboard = new PlanActionOffboard(requestAssigned, action.getPosition(), rAction.getMaxTime(), driverPlan.getVehicle());
//                            : iterate over existing actions and find a timestamp
                            requestPlans.get(j).plan.add(planActionOffboard);
                        } else if (action instanceof PlanActionWait) {

                        }
//                        : add Wait Actions
                    }
                }
            }
        }
        return requestPlans;
    }
    // neni dodelana ale nepouzivam ji
    private List<DriverPlan> convertRequestPlansToDriverPlans(List<RequestPlan> requestPlans, List<RideSharingOnDemandVehicle> vehicles) {
        int lenVehicles = vehicles.size();
        List<DriverPlan> driverPlans = new ArrayList<>();
        List<PlanAction> emptyPlan = new ArrayList<>();
        DriverPlan empty = new DriverPlan(emptyPlan, 0, 0);
        for (int i = 0; i < lenVehicles; i++) {
            driverPlans.add(empty);
        }

        for(RequestPlan requestPlan : requestPlans) {
            for(int i = 0; i < requestPlan.plan.size(); i++) {
                PlanAction action = requestPlan.plan.get(i);
                if (action instanceof PlanActionOffboard) {
                    PlanActionOffboard planActionOffboard = (PlanActionOffboard) action;
                    RideSharingOnDemandVehicle veh = planActionOffboard.getFromVehicle();
                    PlanActionDropoff planActionDropoff = new PlanActionDropoff(requestPlan.getRequest(), planActionOffboard.getPosition(), planActionOffboard.getMaxTime());
                    for (int j = 0; j < vehicles.size(); j++) {
                        if (veh == vehicles.get(i))
                        {
//                            : iterate over existing actions and find a timestamp
                            driverPlans.get(j).plan.add(planActionDropoff);
                        }
                    }
                }
                else if (action instanceof PlanActionOnboard) {
                    PlanActionOnboard planActionOnboard = (PlanActionOnboard) action;
                    RideSharingOnDemandVehicle veh = planActionOnboard.getToVehicle();
                    PlanActionPickup planActionPickup = new PlanActionPickup(requestPlan.getRequest(), planActionOnboard.getPosition(), planActionOnboard.getMaxTime());
                    for (int j = 0; j < vehicles.size(); j++) {
                        if (veh == vehicles.get(i))
                        {
                            driverPlans.get(j).plan.add(planActionOnboard);
                        }
                    }
                }
//                : resolve Wait Actions
            }
        }
        return driverPlans;
    }

    /**
     * Checks time constraints and counts delay among DriverPlans.
     * @return valid DriverPlan with smallest delay.
     */
    private List<PlanAction> findItineraryWithMinimumDelay(List<DriverPlan> plans)
    {
        long[] delays = new long[plans.size()];
        int index = 0;
        for(DriverPlan driverPlan : plans) {
            long time = 0;
            long delay = 0;
            // TODO: how to set (initialize) previousDestination?
            // vychozi pozice, odkud auto vyjizdi
            SimulationNode previousDestination = driverPlan.plan.get(0).getPosition();
//            previousDestination = driverPlan.vehicle.getPosition();

            for (int i = 0; i < driverPlan.getLength(); i++) {
                PlanAction action = driverPlan.plan.get(i);
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if(action instanceof PlanActionPickup) {
                        PlanActionPickup pickup = (PlanActionPickup) action;
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if(!(time <= pcq.getMaxPickupTime())) {
                            //not valid itinerary - check new driver plan
                            break;
                        } else {
                            //valid itinerary
                            delay = delay + (pcq.getMaxPickupTime() - time);
                        }
                        previousDestination = dest;
                    }
                    else if (action instanceof PlanActionDropoff) {
                        PlanActionDropoff dropoff = (PlanActionDropoff) action;
                        SimulationNode dest = pcq.getTo();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if(!(time <= pcq.getMaxDropoffTime())) {
                            //not valid itinerary - check new driver plan
                            break;
                        } else {
                            delay = delay + (pcq.getMaxDropoffTime() - time);
                        }
                        previousDestination = dest;
                    }
                    else if (action instanceof PlanActionWait) {
                        PlanActionWait wait = (PlanActionWait) action;
                        time = time + wait.getWaitTime();
                    }
                }
            }
            // save delay to array
            delays[index] = delay;
            index++;
        }
        //find max in delays
        int maxAt = 0;
        for (int i = 0; i < delays.length; i++) {
            maxAt = delays[i] > delays[maxAt] ? i : maxAt;
        }
        List<PlanAction> bestPlan = plans.get(maxAt).plan;
        return bestPlan;
    }

    /**
     * Creates new DriverPlan with pickups and dropoffs.
     * @return new DriverPlan
     */
    private DriverPlan createItinerary(List<PlanAction> pickupOrder, List<PlanAction> dropoffOrder) {
        List<PlanAction> listOfActionsOrdered = new ArrayList<>(pickupOrder);
        listOfActionsOrdered.addAll(dropoffOrder);
        return new DriverPlan(listOfActionsOrdered, 0, 0);
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

    /**
     * Checks if setting a new via point will exceed the tolerable delay for onboard passengers
     * @return boolean
     */
    private boolean checkTolerableDelay(List<PlanComputationRequest> requestsOnBoard, SimulationNode viaPoint, RideSharingOnDemandVehicle taxi) {
        //TODO
        // uvazuju ze auto se ted rozhodne jet do stanice
        // potrebuju zjistit jestli to nebude vadit ostatnim cestujicim


        //for every onboard passenger in taxi
        for(PlanComputationRequest request : requestsOnBoard) {
            SimulationNode destination = request.getTo();
            //get new time of arrival with new via point
            long newArrivalTime = timeProvider.getCurrentSimTime() + travelTimeProvider.getTravelTime(taxi, viaPoint) + travelTimeProvider.getTravelTime(taxi, viaPoint, destination);
//            long newArrivalTime = travelTimeProvider.getTravelTime(taxi, viaPoint) + travelTimeProvider.getTravelTime(taxi, viaPoint, destination);
            if (newArrivalTime > request.getMaxDropoffTime()) {
                return false;
            }
        }
        return true;
    }

    /**
     * counts arrival time of taxi to pickup location and check if is smaller than MaxPickupTime
     * @return boolean.
     */
    private boolean canServeRequestTASeT(RideSharingOnDemandVehicle vehicle, PlanComputationRequest request) {
        return travelTimeProvider.getTravelTime(vehicle, request.getFrom()) + timeProvider.getCurrentSimTime()
                < request.getMaxPickupTime() * 1000;

        //TODO upravit
        // pokud je taxik prazdny, tak se podivam jestli taxik prijede do cile driv nez je max dropoff time
        // dojede do cile?
        // = expectedTravelTime(aktualni pozice taxiku, zacatek) + expected(zacatek, cil) + currentSimTime
        // tohle zaokrouhlene na integer musi byt <= maxDropoff
        // neboli expectedTravelTime(aktualni, zacatek) + currentSimTime <= maxPickUpTime

        // kdyz taxik neni prazdny, tak krome vyse uvedene podminky musi splnovat podminku i pro ostatni cestujici
        // tento constraint se ale kontroluje v findPlanWithMinimumDelay - az to opravim teda xD
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
        // kolik lidi je prave ted v aute
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
                // nekdo prestupuje
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
                // ted potrebuju najit posledni pickup ve zbyvajicim driver planu po prestupu
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

                //odtud jestli muze dojet k requestu  - tj. cas od mista kde skoncil k vyzvednuti requestu
                long timeToNewRequest = travelTimeProvider.getExpectedTravelTime(previousPos2, request.getFrom());
                long estimatedArrival = timeProvider.getCurrentSimTime() + timeToFinishCurrentEdge + timeToLastTransferAction + timeToLastPickup + timeToNewRequest;
                if (estimatedArrival <= request.getMaxPickupTime() * 1000) {
                     return true;
                } else {
                    return false;
                }
            }
            else {
                //neprestupuje nikdo
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

