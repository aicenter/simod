package cz.cvut.fel.aic.simod.ridesharing.greedyTASeT;

import com.google.inject.Inject;
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
import jdk.internal.util.xml.impl.Pair;
import jdk.nashorn.internal.ir.RuntimeNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private final List<SimulationNode> transferPoints;

    protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;

    //copied from insertionHeuristicSolver
    private GreedyTASeTSolver.PlanData bestPlan;





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
            AgentpolisConfig agentpolisConfig, List<SimulationNode> transferPoints) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
        this.eventProcessor = eventProcessor;
        this.config = config;
        this.timeProvider = timeProvider;
        this.positionUtil = positionUtil;
        this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
        this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
        this.requestFactory = requestFactory;
        this.transferPoints = transferPoints;

        //TODO: resolve
        // commented because config is null in test
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
        AgentPolisEntity[] tVvehicles = vehicleStorage.getEntitiesForIteration();
        for(AgentPolisEntity tVvehicle: tVvehicles) {
            RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
            taxis.add(vehicle);
        }
        // TODO
        List<DriverPlan> driverPlans = dispatch(taxis, newRequests);
        for (int i = 0; i < driverPlans.size(); i++) {
            planMap.put(taxis.get(i), driverPlans.get(i));
        }

        return planMap;
    }

    /**
     * Transfer-allowed scheduling function
     * @return
     */
    private List<DriverPlan> dispatch(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        List<RequestPlan> lst1 = dispatchVacantTaxi(taxis, requests);
        List<RideSharingOnDemandVehicle> carpoolAcceptingTaxis = taxis;
        List<PlanComputationRequest> carpoolAcceptingPassengers = requests;
        List<RequestPlan> lst2 = heuristics(carpoolAcceptingTaxis, carpoolAcceptingPassengers);
        for(PlanComputationRequest request : requests) {
            // TODO: check duplicates
            // is request served by both lst?
        }
        // todo prevest na driver plany
//        lst1.addAll(lst2);
        List<DriverPlan> convertedlist = convertRequestPlansToDriverPlans(lst1, taxis);
        return convertedlist;
    }

    /**
     * traditional taxi dispatch strategy that schedules taxis based on the shortest waiting time for the passengers
     * @return
     */
    private List<RequestPlan> dispatchVacantTaxi(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //taxi dispatch strategy that schedules taxis based on the shortest waiting time for the passengers
        //TODO: implement method
        //function can be changed to any dispatch strategy that a taxi company may currently be using (e.g., shortest waiting time and shortest cruising distance)
//        taxis.get(0).getPosition()
        // while mám volné taxíky
        // najdu nejbližší taxík pro request
        // pro ten request vytvorim
        return null;
    }


    /**
     * Greedy TASeT heuristics function
     * @return
     */
    private List<RequestPlan> heuristics(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //transfer points = charging stations
        List<SimulationNode> transferPoints = this.transferPoints;

        //lookup table LT - LT [t][k] stores the earliest arrival time for taxi k to charging station t without violating the tolerable delay for k’s current passengers
        int stationsCount = transferPoints.size();
        int taxisCount = taxis.size();
        long[][] LT = new long[stationsCount][taxisCount];
        //fill the LT table
        for(int i = 0; i < taxisCount; i++) {
            RideSharingOnDemandVehicle taxi = taxis.get(i);
            SimulationNode taxiPosition = taxis.get(i).getPosition();
            Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
            DriverPlan actualPlan = taxi.getCurrentPlan();
            for(PlanAction action : actualPlan) {
                if(action instanceof PlanRequestAction) {
                    PlanRequestAction requestAction = (PlanRequestAction) action;
                    requestsOnBoardSet.add(requestAction.request);
                }
            }
            List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);
            boolean taxiFree = taxi.hasFreeCapacity();
            for(int j = 0; j < stationsCount; j++) {
                //timeProvider is set to null so getCurrentSimTime() wont work
                //the idea is to fill LT with times of arrival of taxis
//                LT[j][i] = this.timeProvider.getCurrentSimTime() + travelTime;
                //check if taxi has free seat
                if (!taxiFree) {
                    LT[j][i] = Long.MAX_VALUE;
                }
                else {
                    SimulationNode station = transferPoints.get(j);
                    long travelTime = this.travelTimeProvider.getExpectedTravelTime(taxiPosition, station);
                    //check if setting a new via point will exceed the tolerable delay for onboard passengers
                    if (checkTolerableDelay(requestsOnBoard, station, taxi)) {
                        LT[j][i] = travelTime;
                    } else {
                        LT[j][i] = Long.MAX_VALUE;
                    }
                }
            }
        }

        //itinerary list
        List<RequestPlan> itinerarylist = new ArrayList<>();

        //we rank the requests in descending order by the number of taxis that are possible to pick them up in time (without considering transfer or destination)
        //get possible taxis for every request and number of possible taxis
        int[] possiblePickupTaxisCounts = new int[requests.size()];
        int i = 0;
        Map<PlanComputationRequest, List<RideSharingOnDemandVehicle>> possiblePickupTaxisMap = new HashMap<>();
        for(PlanComputationRequest request : requests) {
            int counter = 0;
            List<RideSharingOnDemandVehicle> possiblePickupTaxisOneRequest = new ArrayList<>();
            for(RideSharingOnDemandVehicle t : taxis) {
                if (canServeRequestTASeT(t, request)) {
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
//            List<List<PlanAction>> templist = new ArrayList<>(); // list planactionu pro auto
            Map<Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>, List<PlanAction>> templist = new HashMap<>();       //hashmapa RequestPlan : list driverplanu
            List<Long> delays = new ArrayList<>();
            List<Long> transferTimes = new ArrayList<>();
            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            for (RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                List<PlanAction> posbitnry = findPlanWithNoTransfer(request, taxi);     // pro auto
                List<PlanAction> posbitnryR = getActionsForRequestFromActionsForDriver(posbitnry, request, taxi);   // pro request
                List<List<PlanAction>> tmp = new ArrayList<>();
                List<RideSharingOnDemandVehicle> tmpVehs = new ArrayList<>();
                tmpVehs.add(taxi);
                tmp.add(posbitnry);
                Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> submap = new HashMap<>();
                submap.put(tmp, tmpVehs);
                templist.put(submap, posbitnryR);
                delays.add((long) 0);
                transferTimes.add((long) 0);
                long travelTimeNoTransfer = getTravelTime(request, posbitnry);
                //charge stations list = transferPoints
                int stationIndex = 0;
                for (SimulationNode station : transferPoints) {
                    // find k' = taxis that taxi k can transfer to at station
                    for (int k = 0; k < taxisCount; k++) {
                        //not possible to transfer to
                        if (LT[stationIndex][k] == Long.MAX_VALUE) {
                            continue;
                        } else {
                            // split request to two requests with transfer point
                            DefaultPlanComputationRequest newRequest1 = requestFactory.create(0, request.getFrom(),
                                    station, request.getDemandAgent());
                            DefaultPlanComputationRequest newRequest2 = requestFactory.create(1, station,
                                    request.getTo(), request.getDemandAgent());
                            //find optimal plans for these two requests
                            List<PlanAction> itnryp1 = findPlanWithNoTransfer(newRequest1, taxi);       // pro auto
                            //List<PlanAction> itnryp1R = getActionsForRequestFromActionsForDriver(itnryp1, newRequest1, taxi);
                            List<PlanAction> itnryp2 = findPlanWithNoTransfer(newRequest2, taxis.get(k));   // pro auto
                            //List<PlanAction> itnryp2R = getActionsForRequestFromActionsForDriver(itnryp2, newRequest2, taxis.get(k));
                            Map<List<List<PlanAction>>, Long> m = createChargePlan(itnryp1, itnryp2, taxi, taxis.get(k), newRequest1, newRequest2);
                            if (m == null) {
                                // neni mozne prestoupit, takze neudelam nic
                                continue;
                            } else {
                                Map.Entry<List<List<PlanAction>>, Long> entry = m.entrySet().iterator().next();
                                List<List<PlanAction>> itnrys = entry.getKey();
                                itnryp1 = itnrys.get(0);
                                itnryp2 = itnrys.get(1);
                                List<List<PlanAction>> tmp2 = new ArrayList<>();
                                tmp2.add(itnryp1);
                                tmp2.add(itnryp2);
                                List<RideSharingOnDemandVehicle> tmpVehs2 = new ArrayList<>();
                                tmpVehs2.add(taxi);
                                tmpVehs2.add(taxis.get(k));
                                Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> submap2 = new HashMap<>();
                                submap2.put(tmp2, tmpVehs2);
                                List<PlanAction> transferPlan = splittedRequestToPlanForRequest(itnryp1, itnryp2, newRequest1, newRequest2, request);
                                templist.put(submap2, transferPlan);
                                long travelTimeTransfer = getTravelTime(newRequest1, itnryp1) + getTravelTime(newRequest2, itnryp2);
                                delays.add(travelTimeNoTransfer - travelTimeTransfer);
                                long transferTime = entry.getValue();
                                transferTimes.add(transferTime);
                            }
                        }
                    }
                    stationIndex++;
                }
            }

            // potrebuji seradit delays
            // a podle toho vybrat veci z hashmapy

            //create array of indices
            List<Integer> indices = new ArrayList<>();
            for(int q = 0; q < delays.size(); q++)
            {
                indices.add(q);
            }
            List<Long> beforeDelays = new ArrayList<>();
            List<Integer> beforeIndices = new ArrayList<>();
            beforeDelays.addAll(delays);
            beforeIndices.addAll(indices);
            //seradim delays od nejkratsich
            delays.sort(null);
            for(int q = 0; q < beforeDelays.size(); q++) {
                int index = beforeDelays.indexOf(delays.get(q));
                indices.set(q, beforeIndices.get(q));
            }
            // ted mam serazene delays a indexy v indices

            //vezmu hornich beta procent
            double beta = 0.2;
            int numOfTaken = (int) (delays.size() * beta);
            if (numOfTaken == 0) {
                numOfTaken = 1;
            }
            List<Integer> subsetIndices = new ArrayList<>();
            List<Long> subsetTransferTimes = new ArrayList<>();
            for (int q = 0; q < numOfTaken; q++)
            {
                subsetIndices.add(indices.get(q));
                subsetTransferTimes.add(transferTimes.get(indices.get(q)));
            }
            // v subsetIndices mam ted indexy tech vysledku, ktere chci vybrat pro porovnani podle transfer timu
            // v subsetTransferTimes jsou casy prestupu, podle toho to ted budu chtit seradit

            // chci seradit subsetIndices podle subsetTransferTImes
            List<Long> beforeSubsetTransferTimes = new ArrayList<>();
            List<Integer> beforeSubsetIndices = new ArrayList<>();
            beforeSubsetTransferTimes.addAll(subsetTransferTimes);
            beforeSubsetIndices.addAll(subsetIndices);
            //seradim transfer times od nejkratsich
            subsetTransferTimes.sort(null);
            for(int q = 0; q < beforeSubsetTransferTimes.size(); q++) {
                int index = beforeSubsetTransferTimes.indexOf(subsetTransferTimes.get(q));
                subsetIndices.set(q, beforeSubsetIndices.get(q));
            }
            // ted mam serazene transferTimes a indexy v subsetIndices

            // ted bych mela chtit vybrat jeden entry z templistu podle toho subsetIndices
            int indexOfFirst = subsetIndices.get(0);
            int iterateOrder = 0;
            Map.Entry<Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>, List<PlanAction>> returnEntry = null;

            // ziskam entry ktery je nejlepsi podle heuristiky
            for (Map.Entry<Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>>, List<PlanAction>> entry : templist.entrySet())
            {
                if (iterateOrder == indexOfFirst) {
                    returnEntry = entry;
                }
            }
            List<PlanAction> selectedList = returnEntry.getValue();
            RequestPlan selected = new RequestPlan(selectedList, 0, 0);
            selected.setRequest(request);
            itinerarylist.add(selected);
            Map<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> key = returnEntry.getKey();
            for (Map.Entry<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> entry : key.entrySet()) {
                List<List<PlanAction>> plansForVehicles = entry.getKey();
                List<RideSharingOnDemandVehicle> vehicles = entry.getValue();
                for (int q = 0; q < vehicles.size(); q++) {
                    RideSharingOnDemandVehicle veh = vehicles.get(q);
                    List<PlanAction> vehPlan = plansForVehicles.get(q);
                    DriverPlan dp = new DriverPlan(vehPlan, 0, 0);
                    veh.setCurrentPlan(dp);
                }
            }

            // update k, k′ and LT
            //update LT - not efficient
            for(int q = 0; q < taxisCount; q++) {
                RideSharingOnDemandVehicle taxi = taxis.get(q);
                SimulationNode taxiPosition = taxis.get(q).getPosition();
                Set<PlanComputationRequest> requestsOnBoardSet = new HashSet<>();
                DriverPlan actualPlan = taxi.getCurrentPlan();
                for(PlanAction action : actualPlan) {
                    if(action instanceof PlanRequestAction) {
                        PlanRequestAction requestAction = (PlanRequestAction) action;
                        requestsOnBoardSet.add(requestAction.request);
                    }
                }
                List<PlanComputationRequest> requestsOnBoard = new ArrayList<>(requestsOnBoardSet);
                boolean taxiFree = taxi.hasFreeCapacity();
                for(int j = 0; j < stationsCount; j++) {
                    //timeProvider is set to null so getCurrentSimTime() wont work
                    //the idea is to fill LT with times of arrival of taxis
//                LT[j][i] = this.timeProvider.getCurrentSimTime() + travelTime;
                    //check if taxi has free seat
                    if (!taxiFree) {
                        LT[j][q] = Long.MAX_VALUE;
                    }
                    else {
                        SimulationNode station = transferPoints.get(j);
                        long travelTime = this.travelTimeProvider.getExpectedTravelTime(taxiPosition, station);
                        //check if setting a new via point will exceed the tolerable delay for onboard passengers
                        if (checkTolerableDelay(requestsOnBoard, station, taxi)) {
                            LT[j][q] = travelTime;
                        } else {
                            LT[j][q] = Long.MAX_VALUE;
                        }
                    }
                }
            }

            //update k
            for(PlanComputationRequest req : requests) {
                int counter = 0;
                List<RideSharingOnDemandVehicle> possiblePickupTaxisOneRequest = new ArrayList<>();
                for(RideSharingOnDemandVehicle t : taxis) {
                    if (canServeRequestTASeT(t, req)) {
                        counter++;
                        possiblePickupTaxisOneRequest.add(t);
                    }
                }
                possiblePickupTaxisCounts[i] = counter;
                possiblePickupTaxisMap.put(req, possiblePickupTaxisOneRequest);
                i++;
            }

        }
        return itinerarylist;
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
                 previousPosition = action.getPosition();
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
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     if (pcq.getPickUpAction().request == newRequest1) {
                         PlanActionPickup pickup = (PlanActionPickup) pcq;
                         PlanActionPickup newPickup = new PlanActionPickup(originalRequest, pickup.getPosition(), pickup.getMaxTime());
                         listForOriginalRequest.add(newPickup);
                     }
                 } else if (action instanceof PlanActionDropoff) {
                     if (pcq.getDropOffAction().request == newRequest1) {
                         PlanActionDropoff dropoff = (PlanActionDropoff) pcq;
                         PlanActionDropoff newDropoff = new PlanActionDropoff(originalRequest, dropoff.getPosition(), dropoff.getMaxTime());
                         listForOriginalRequest.add(newDropoff);
                     }
                 }
             }
         }
         for (PlanAction action : itnryp2) {
             if (action instanceof PlanRequestAction) {
                 PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                 if (action instanceof PlanActionPickup) {
                     if (pcq.getPickUpAction().request == newRequest2) {
                         PlanActionPickup pickup = (PlanActionPickup) pcq;
                         PlanActionPickup newPickup = new PlanActionPickup(originalRequest, pickup.getPosition(), pickup.getMaxTime());
                         listForOriginalRequest.add(newPickup);
                     }
                 } else if (action instanceof PlanActionDropoff) {
                     if (pcq.getDropOffAction().request == newRequest2) {
                         PlanActionDropoff dropoff = (PlanActionDropoff) pcq;
                         PlanActionDropoff newDropoff = new PlanActionDropoff(originalRequest, dropoff.getPosition(), dropoff.getMaxTime());
                         listForOriginalRequest.add(newDropoff);
                     }
                 }
             }
         }

        return listForOriginalRequest;

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
         if (waitTime < 0) {
             //transfer time je -waitTime
             PlanActionWait waitAction = new PlanActionWait(null, pickup.getPosition(), pickup.getMaxTime(), -waitTime);
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
                         if (!(time < pcq.getMaxPickupTime())) {
                             //not valid itinerary - check new driver plan
                             valid = false;
                             break;
                         }
                         previousDestination = dest;
                     } else if (action instanceof PlanActionDropoff) {
                         SimulationNode dest = pcq.getTo();
                         time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                         if (!(time < pcq.getMaxDropoffTime())) {
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
             Map<List<List<PlanAction>>, Long> map = new HashMap<>();
             map.put(itnrys, transferTime);
             return map;
         }
         else {
             return null;
         }
     }


    /**
     * Find DriverPlan with smallest delay without transfer allowed.
     * @return valid DriverPlan with smallest delay.
     */
    private List<PlanAction> findPlanWithNoTransfer(PlanComputationRequest newRequest, RideSharingOnDemandVehicle taxi) {
        int n = taxi.getOnBoardCount();
        List<DriverPlan> lst = new ArrayList<>();
        DriverPlan currentPlan = taxi.getCurrentPlan();
        //add pickup and dropoff for new request
        currentPlan.plan.add(newRequest.getPickUpAction());
        currentPlan.plan.add(newRequest.getDropOffAction());
        //get pickup order based on heuristic from TASeT paper
        List<PlanAction> pickups = currentPlan.getPickupActions();
        //get dropoff actions in currentPlan
        List<PlanAction> dropoffs = currentPlan.getDropoffActions();
        //permute dropoff orders
        List<List<PlanAction>> dropoffOrders = permute(dropoffs);
        for (List<PlanAction> dropoffPlan : dropoffOrders) {
            lst.add(createItinerary(pickups, dropoffPlan));
        }
        List<PlanAction> bestPlan = findItineraryWithMinimumDelay(lst);
        return bestPlan;

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
                        PlanAction currentAction = actionsForRequest.get(0);
                        PlanRequestAction currentRAction = (PlanRequestAction) currentAction;
                        int index = 0;
                        while(currentRAction.getMaxTime() <= onboard.getMaxTime()) {
                            index++;
                            currentAction = actionsForRequest.get(index);
                            currentRAction = (PlanRequestAction) currentAction;
                        }
                        actionsForRequest.add(index, onboard);
                    }
                    if (action instanceof PlanActionDropoff) {
                        PlanActionDropoff dropoff = new PlanActionDropoff(request, action.getPosition(), planRequestAction.getMaxTime());
                        PlanAction currentAction = actionsForRequest.get(0);
                        PlanRequestAction currentRAction = (PlanRequestAction) currentAction;
                        int index = 0;
                        while(currentRAction.getMaxTime() <= dropoff.getMaxTime()) {
                            index++;
                            currentAction = actionsForRequest.get(index);
                            currentRAction = (PlanRequestAction) currentAction;
                        }
                        actionsForRequest.add(index, dropoff);
                    }
                }
            }
        }
        return actionsForRequest;
    }

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
//                            TODO: iterate over existing actions and find a timestamp HOPEFULLY DONE
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
//                            TODO: iterate over existing actions and find a timestamp
                            requestPlans.get(j).plan.add(planActionOffboard);
                        } else if (action instanceof PlanActionWait) {

                        }
//                        TODO: add Wait Actions
                    }
                }
            }
        }

        return requestPlans;
    }

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
//                            TODO: iterate over existing actions and find a timestamp
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
//                TODO: resolve Wait Actions
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
            previousDestination = driverPlan.vehicle.getPosition();

            for (int i = 0; i < driverPlan.getLength(); i++) {
                PlanAction action = driverPlan.plan.get(i);
                if (action instanceof PlanRequestAction) {
                    PlanComputationRequest pcq = ((PlanRequestAction) action).getRequest();
                    if(action instanceof PlanActionPickup) {
                        PlanActionPickup pickup = (PlanActionPickup) action;
                        SimulationNode dest = pcq.getFrom();
                        time = time + travelTimeProvider.getExpectedTravelTime(previousDestination, dest);
                        if(!(time < pcq.getMaxPickupTime())) {
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
                        if(!(time < pcq.getMaxDropoffTime())) {
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
        List<PlanAction> listOfActionsOrdered = new LinkedList<>(pickupOrder);
        listOfActionsOrdered.addAll(dropoffOrder);
        return new DriverPlan(listOfActionsOrdered, 0, 0);
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
        //for every onboard passenger in taxi
        for(PlanComputationRequest request : requestsOnBoard) {
            SimulationNode destination = request.getTo();
            //get new time of arrival with new via point
//            long newArrivalTime = timeProvider.getCurrentSimTime() + travelTimeProvider.getTravelTime(taxi, viaPoint) + travelTimeProvider.getTravelTime(taxi, viaPoint, destination);
            long newArrivalTime = travelTimeProvider.getTravelTime(taxi, viaPoint) + travelTimeProvider.getTravelTime(taxi, viaPoint, destination);
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
        //wont work since timeProvider is set null in test
//        return travelTimeProvider.getTravelTime(vehicle, request.getFrom()) + timeProvider.getCurrentSimTime()
//                < request.getMaxPickupTime();
        return travelTimeProvider.getTravelTime(vehicle, request.getFrom()) < request.getMaxPickupTime();
    }


















    //copied from InsertionHeuristicSolver
    private void computeOptimalPlan(RideSharingOnDemandVehicle vehicle, DriverPlan currentPlan, PlanComputationRequest planComputationRequest) {

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
                    GreedyTASeTSolver.PlanData bestPlanData = new PlanData(vehicle, potentialPlan, costIncrement);
                    tryUpdateBestPlan(bestPlanData);
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
    }

    //copied from InsertionHeuristicSolver
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
                if(previousTask instanceof PlanActionCurrentPosition){
                    newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, newTask.getPosition());
                }
                else{
                    newPlanTravelTime += travelTimeProvider.getTravelTime(vehicle, previousTask.getPosition(),
                            newTask.getPosition());
                }
            }
            long currentTaskTimeInSeconds = (timeProvider.getCurrentSimTime() + newPlanTravelTime) / 1000;
//			LOGGER.debug("currentTaskTimeInSeconds: {}", currentTaskTimeInSeconds);

            /* check max time for all unfinished demands */

            // check max time check for the new action
            if(newTask instanceof PlanRequestAction){
                int maxTime = ((PlanRequestAction) newTask).getMaxTime();
                if(maxTime < currentTaskTimeInSeconds){
//                                    LOGGER.debug("currentTaskTimeInSeconds {} \n> maxTime {}",currentTaskTimeInSeconds, maxTime);
                    return null;
                }
            }

            // check max time for actions in the current plan
            for(int index = indexInOldPlan + 1; index < currentPlan.getLength(); index++){
                PlanAction remainingAction = currentPlan.plan.get(index);
                if(!(remainingAction instanceof PlanActionCurrentPosition)){
                    PlanRequestAction remainingRequestAction = (PlanRequestAction) remainingAction;
                    if(remainingRequestAction.getMaxTime() < currentTaskTimeInSeconds){
                        return null;
                    }
                }
            }

            // check max time for pick up action
            if(newPlanIndex <= pickupOptionIndex){
                if(planComputationRequest.getPickUpAction().getMaxTime() < currentTaskTimeInSeconds){
                    return null;
                }
            }

            // check max time for drop off action
            if(newPlanIndex <= dropoffOptionIndex){
                if(planComputationRequest.getDropOffAction().getMaxTime() < currentTaskTimeInSeconds){
                    return null;
                }
            }


            /* pickup and drop off handeling */
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
    //copied from InsertionHeuristicSolver
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

    //copied from InsertionHeuristicSolver - edited
    private synchronized void tryUpdateBestPlan(GreedyTASeTSolver.PlanData newPlanData){
        if(newPlanData != null){
            bestPlan = newPlanData;
        }
    }
}








