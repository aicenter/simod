/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class Solution {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Solution.class);

    private final int maxProlongation;
    
    private final int timeToStart;
    
    private final Demand demand;
    
    private final TravelTimeProvider travelTimeProvider;
    
    private final AmodsimConfig config;
    
    private final List<Car> cars;
    
    private final Map<Integer, Map<Integer,Integer>> carPlanTimeMap;

    /**
     *
     * @param demand {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.Demand}
     * @param travelTimeProvider {@link cz.cvut.fel.aic.amodsim.ridesharing.offline.search.TravelTimeProviderTaxify}
     * @param config {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify}
     */
    public Solution(Demand demand, TravelTimeProvider travelTimeProvider,  AmodsimConfig config) {
        this.demand = demand;
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        timeToStart = config.ridesharing.offline.timeToStart;
        cars = new ArrayList<>();
        carPlanTimeMap = new HashMap<>();
    }
           
    /**
     * 
     * @return 
     */
    public List<Car> getAllCars(){
        Collections.sort(cars, Comparator.comparing(Car::getId));
        return cars;
    }

    /**
     * Creates paths from map cover, and assigns cars to the paths.
     * 
     */
    public void buildPaths(){
        
        int[] pair = demand.findMapCover(config.ridesharing.offline.sigma);
        int n = pair.length;
        
         //print pairs for debug
        Set<Integer> seen = new HashSet<>();
//        LOGGER.debug("PAIRS");
//        for (int i = 0 ; i < pair.length; i++){
//            while(i != demand.N){
//                if(seen.contains(i)) break;
//                seen.add(i);
//                if(pair[i] < demand.N){
//                    LOGGER.debug( "d("+i+") "+ demand.getEndTime(i)+" -> p("+pair[i] +") "+ demand.getStartTime(pair[i]));
//                }else{
//                    LOGGER.debug( " last d("+i+") "+ demand.getEndTime(i)+" -> end");
//                }
//                i = pair[i];
//            }
//           LOGGER.debug("Done");
//        }
        

        Set<Integer> seenNodes = new HashSet<>();
        for(int i = 0; i < n; i++ ){
            if(seenNodes.contains(i)){
                continue;
            }
            seenNodes.add(i);
            // start of new segment, which doesn't have an assigned car
            int currentNodeInd = i; 
            //DriverPlan dp = (DriverPlan) demand.getPlanByIndex(i);
            int planStartTime = demand.getStartTime(i);
//            LOGGER.debug("New plan " + currentNodeInd + " time [" + planStartTime + ", " + demand.getEndTime(i) +
//                "]" + ", next plan "+pair[currentNodeInd]);
            Car car = getCar(currentNodeInd);//returns car with current node already added
            int time = car.getLastActionTime();//end of the 1s plan
            currentNodeInd  = pair[currentNodeInd];//2nd plan index
            
//            LOGGER.debug("returned car "+car.id +"; node "+car.getLastDemandNode()+", time "+time);
            seenNodes.add(currentNodeInd);
            
            while(currentNodeInd != n){
//                LOGGER.debug("current node "+ currentNodeInd);
               
                int travelTime = (int) travelTimeProvider.getExpectedTravelTime(demand.getEndNode(car.getLastDemandNode()),
                    demand.getStartNode(currentNodeInd));
                int requestTime = demand.getStartTime(currentNodeInd);
             
                int startTime = Math.max(time + travelTime, requestTime);
//                LOGGER.debug("travel time "+ travelTime +"; "+ requestTime + " < "+startTime + " < " + (requestTime + maxProlongation));
                             
                seenNodes.add(currentNodeInd);
                car.addDemandNode(currentNodeInd, startTime, demand.getBestTime(currentNodeInd));
               
//                LOGGER.debug(currentNodeInd +" being add. start="+startTime+", length="+demand.getBestTime(currentNodeInd));
                
                currentNodeInd  = pair[currentNodeInd];
            }
//            LOGGER.debug("FINISHED.");
        }
        int totaTrips = getAllCars().stream().map(c->c.getSize()).mapToInt(Integer::intValue).sum();
        LOGGER.info("Total trip nodes in paths: "+totaTrips);
        LOGGER.info("Cars used: "+ cars.size());
        
    
    }
    
    /**
     * Tries to find existing car able to arrive to the
     * origin of the trip in time. 
     * If car is not found, creates new car located
     *  timeToStart ms from the origin of the trip, 
     * adds it to the list.
     * Adds trip to the car, returns the car.
     * 
     * @param trip trip index in demand
     * @return 
     */
    private Car getCar(int trip){
        Car theCar = null;
        List<Car> filteredCars = cars.stream()
            .filter(c -> c.getLastActionTime() < demand.getStartTime(trip))
            .collect(Collectors.toList());
        int bestArrivalTime = Integer.MAX_VALUE;
        int time = -1;
        
        for (Car car: filteredCars){
            if((time = canServe(car, trip)) > 0){
//              LOGGER.debug("time "+ time);
                if (time < bestArrivalTime){
                    bestArrivalTime = time;
                    theCar = car;
//                    LOGGER.debug("Best time "+ bestArrivalTime);
                }
            }
        }
        if(theCar != null){
            theCar.addDemandNode(trip, bestArrivalTime, demand.getBestTime(trip));
//            LOGGER.debug("Car found " + theCar.id + " in node " + theCar.getLastDemandNode() + " time: " + theCar.getLastActionTime());
//            LOGGER.debug("for trip " + demand.indToTripId(trip) + " starting from " + demand.getStartNodeId(trip) +
//                 " at " + demand.getStartTime(trip));
            return theCar;
        }
        theCar = new Car();
        theCar.addDemandNode(trip, demand.getStartTime(trip) + timeToStart, demand.getBestTime(trip));
//        LOGGER.debug("New car created: " + theCar.id + " at node "+ theCar.getLastDemandNode() + " time "+ theCar.getLastActionTime());
        cars.add(theCar);
        return theCar;
    }


    /**
     * Checks if existing car from the list
     * is able to arrive from it's last node
     * to the origin of the new trip in time.
     * Returns time of arrival if it is feasible, 
     *  - 1 otherwise.
    */
    private int canServe(Car car, int trip){
        int lastTripId = car.getLastDemandNode();
        int lastActionTime = car.getLastActionTime();
//        LOGGER.debug("car last action time " + lastActionTime);
        SimulationNode startNode = demand.getEndNode(lastTripId);
        SimulationNode endNode = demand.getStartNode(trip);
        int travelTime = (int) travelTimeProvider.getExpectedTravelTime(startNode, endNode);
//        LOGGER.debug("travelTime "+ travelTime);
        int requestTime = demand.getStartTime(trip);
//        LOGGER.debug("request time  "+ requestTime + " epa time " + (travelTime + lastActionTime));
        int actionTime = lastActionTime + travelTime;
        if (actionTime  <= (requestTime + timeToStart)){
            return actionTime;
        } else {
            return -1;
       }
    }
    
    
 }















//    private Car getCar(int trip, int[] depo){
//        Car theCar = null;
//        // check among waiting cars
//        List<Car> sortedCars = cars[W].stream().sorted(Comparator.comparingInt(Car::getLastActionTime))
//            .collect(Collectors.toList());
//        for (Car car: sortedCars){
//            if(canServe(car.getLastDemandNode(), car.getLastActionTime(), trip)){
//                theCar = car;
////                int currentNode = car.getLastDemandNode();
////                SimulationNode startSimNode = demand.getEndNode(currentNode);
////                SimulationNode endSimNode = demand.getStartNode(trip);
////                int timeToTripStart = (int) travelTimeProvider.getExpectedTravelTime(startSimNode, endSimNode);
//                break;
//                }
//        }
//        if(theCar != null){
//            
//            cars[D].add(theCar);
//            cars[W].remove(theCar);
//            return theCar;
//        }
//        //second, search for cars in the nearest Depo
//        int latestPossibleArrival = demand.getStartTime(trip) + maxWaitTime;
////        sortedCars = cars[C].stream().sorted(Comparator.comparingInt(Car::getLastActionTime))
////            .collect(Collectors.toList());
////        for(Car car: sortedCars){
////            SimulationNode startSimNode = demand.getNodeById(-car.getLastDemandNode());
////            SimulationNode endSimNode = demand.getStartNode(trip);
////            int timeFromeDepo = (int) travelTimeProvider.getExpectedTravelTime(startSimNode, endSimNode);
////            if (car.getLastActionTime() + timeFromeDepo + timeBuffer <= latestPossibleArrival){
////                theCar = car;
////                break;
////                }
////            }
////        if(theCar != null){
////            cars[D].add(theCar);
////            cars[C].remove(theCar);
////            return theCar;
////           
////        }
//        if(depo[1] < latestPossibleArrival){
//         
//            theCar = new Car(depo[0]);
//            cars[D].add(theCar);
//            return theCar;
//        }
//        return null;
//    }









    