package cz.cvut.fel.aic.simod.ridesharing.greedyTASeT;

import com.google.inject.Inject;
import com.sun.xml.internal.xsom.impl.scd.Iterators;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.Drive;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.io.SimulationNodeArrayConstructor;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
import cz.cvut.fel.aic.simod.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import me.tongfei.progressbar.ProgressBar;

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
            AgentpolisConfig agentpolisConfig) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
        this.eventProcessor = eventProcessor;
        this.config = config;
        this.timeProvider = timeProvider;
        this.positionUtil = positionUtil;
        this.droppedDemandsAnalyzer = droppedDemandsAnalyzer;
        this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;

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
        // TODO: fix it
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
        List<DriverPlan> lst1 = dispatchVacantTaxi(taxis, requests);
        List<RideSharingOnDemandVehicle> carpoolAcceptingTaxis = taxis;
        List<PlanComputationRequest> carpoolAcceptingPassengers = requests;
        List<DriverPlan> lst2 = heuristics(carpoolAcceptingTaxis, carpoolAcceptingPassengers);
        for(PlanComputationRequest request : requests) {
            // TODO: check duplicates
            // wtf
            // is request in lst?
        }
        lst1.addAll(lst2);
        return lst1;
    }

    /**
     * traditional taxi dispatch strategy that schedules taxis based on the shortest waiting time for the passengers
     * @return
     */
    private List<DriverPlan> dispatchVacantTaxi(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //taxi dispatch strategy that schedules taxis based on the shortest waiting time for the passengers
        //TODO: implement method
        //function can be changed to any dispatch strategy that a taxi company may currently be using (e.g., shortest waiting time and shortest cruising distance)
        return null;
    }


    /**
     * Greedy TASeT heuristics function
     * @return
     */
    private List<DriverPlan> heuristics(List<RideSharingOnDemandVehicle> taxis, List<PlanComputationRequest> requests) {
        //transfer points = charging stations
        List<SimulationNode> transferPoints = new ArrayList<>();
        //TODO: fill transfer points

        //lookup table LT - LT [t][k] stores the earliest arrival time for taxi k to charging station t without violating the tolerable delay for k’s current passengers
        int stationsCount = transferPoints.size();
        int taxisCount = taxis.size();
        long[][] LT = new long[stationsCount][taxisCount];
        //fill the LT table
        for(int i = 0; i < taxisCount; i++) {
            RideSharingOnDemandVehicle taxi = taxis.get(i);
            SimulationNode taxiPosition = taxis.get(i).getPosition();
            List<PlanComputationRequest> requestsOnBoard = taxi.getVehicle().getTransportedEntities();
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
        List<DriverPlan> itinerarylist = new ArrayList<>();

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
            List<DriverPlan> templist = new ArrayList<>();

            List<RideSharingOnDemandVehicle> canPickupRequestTaxis = possiblePickupTaxisMap.get(request);
            for(RideSharingOnDemandVehicle taxi : canPickupRequestTaxis) {
                DriverPlan posbitnry = findPlanWithNoTransfer(request, taxi);
                templist.add(posbitnry);
                //charge stations list = transferPoints
                int stationIndex = 0;
                for(SimulationNode station : transferPoints) {
                    // find k' = taxis that taxi k can transfer to at station
                    for(int k = 0; k < taxisCount; k++)
                    {
                        //not possible to transfer to
                        if(LT[stationIndex][k] == Long.MAX_VALUE) {
                            continue;
                        } else {
                            //TODO (check): get requestFactory and DemandAgent?
                            PlanActionPickup p = (PlanActionPickup) taxi.getCurrentTask();
                            // split request to two requests with transfer point
                            DefaultPlanComputationRequest newRequest1 = requestFactory.create(0, request.getFrom(),
                                    station, p.getRequest().getDemandAgent());
                            DefaultPlanComputationRequest newRequest2 = requestFactory.create(1, station,
                                    request.getTo(), p.getRequest().getDemandAgent());
                            //find optimal plans for these two requests
                            DriverPlan itryp1 = findPlanWithNoTransfer(newRequest1, taxi);
                            DriverPlan itnryp2 = findPlanWithNoTransfer(newRequest2, taxis.get(k));
                            // TODO: create Charge plan (TransferPlan) from itnryp1 and itnryp2
                            // idea: transfer time = zjistim z LT tabulky
                            // create DriverPlan from itnryp1 and itnryp2, add transfer time between
                            // check constraints for DriverPlan
                            // if ok,
//                            templist.add(ZKONTROLOVANY PLAN);
                        }
                    }
                    stationIndex++;
                }
            }
            // TODO:
            // sort templist by delay and transfer time
            // podobna funkce jako findItineraryWithMinimumDelay, akorat nevrati jeden DriverPlan, ale seradi je
            // selected = itinerary with longest transfer time in the top β% shortest delay
            // budu si nekde drzet transfer time pro kazdy DriverPlan (nebo request?)
            // update k, k′ and LT
//            itinerarylist.add(selected)
        }
        return itinerarylist;
    }


    /**
     * Find DriverPlan with smallest delay without transfer allowed.
     * @return valid DriverPlan with smallest delay.
     */
    private DriverPlan findPlanWithNoTransfer(PlanComputationRequest newRequest, RideSharingOnDemandVehicle taxi) {
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
        DriverPlan bestPlan = findItineraryWithMinimumDelay(lst);
        return bestPlan;

    }

    /**
     * Checks time constraints and counts delay among DriverPlans.
     * @return valid DriverPlan with smallest delay.
     */
    private DriverPlan findItineraryWithMinimumDelay(List<DriverPlan> plans)
    {
        long[] delays = new long[plans.size()];
        int index = 0;
        for(DriverPlan driverPlan : plans) {
            long time = 0;
            long delay = 0;
            // TODO: how to set (initialize) previousDestination?
            SimulationNode previousDestination = driverPlan.plan.get(0).getPosition();

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
                }
            }
            // save delay to array
            delays[index] = delay;
            index++;
        }
        //find max in delay
        int maxAt = 0;
        for (int i = 0; i < delays.length; i++) {
            maxAt = delays[i] > delays[maxAt] ? i : maxAt;
        }
        DriverPlan bestPlan = plans.get(maxAt);
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
        for(PlanComputationRequest plan : requestsOnBoard) {
            SimulationNode destination = plan.getTo();
            //get new time of arrival with new via point
            //TODO (check): count new Arrival time
            long newArrivalTime = timeProvider.getCurrentSimTime() + travelTimeProvider.getTravelTime(taxi, viaPoint) + travelTimeProvider.getTravelTime(taxi, viaPoint, destination);
            if (newArrivalTime > plan.getMaxDropoffTime()) {
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
        //TODO (check): if taxi can pick request in time
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








