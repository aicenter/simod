/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class Solution {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Solution.class);
    private final int D = 0;
    private final int C = 1;
    private final int W = 2;
    final int maxCar;
    final int sigma;
    private final int maxWaitTime;
    Demand demand;
    TravelTimeProvider travelTimeProvider;
    StationCentral central;
    AmodsimConfig config;
        
    List<Car>[] cars;

    public Solution(Demand demand, TravelTimeProvider travelTimeProvider, StationCentral central, AmodsimConfig config) {
        this.demand = demand;
        this.travelTimeProvider = travelTimeProvider;
        this.central = central;
        this.config = config;
        maxWaitTime = config.amodsim.ridesharing.maxWaitTime * 1000;
        
        maxCar = 10000;
        sigma = 5; //minutes
        cars = new List[3];
        cars[D] = new ArrayList<>();
        cars[C] = new ArrayList<>();
        cars[W] = new ArrayList<>();
    }
           
    /**
     * Returns list of cars from sublists in one list.
     * @return 
     */
    public List<Car> getAllCars(){
        List<Car> carList = new ArrayList<>();
        carList.addAll(cars[D]);
        carList.addAll(cars[W]);
        carList.addAll(cars[C]);
        return carList;
    }

    /**
     * Creates path with cars assigned from pairing returned by HK.
     * 
     */
    public void buildPaths(){
        
        int[] pair = demand.findMapCover(sigma);
        int n = pair.length;
        Set<Integer> usedNodes = new HashSet<>();
        for(int i = 0; i < n; i++ ){
           // LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            // the node is already used in the path
            if(usedNodes.contains(i)){
                continue;
            }
            // start of new segment, which doesn't have an assigned car
            int currentNode = i; 
 //           LOGGER.debug("node ind "+currentNode+", simId "+demand.ind2id(currentNode));
            int[] starts = demand.getStartNodes(currentNode);{
//            for(int k=0; k<starts.length;k+=2){
//                if(starts[k]>=10019){
//                    System.out.println("Invalid simNode "+starts[k]+", dist "+starts[k+1]);
//                }
//            }
        }
            //LOGGER.debug(Arrays.toString(starts));
            int[] depo = central.findNearestStation(starts);
            
            //LOGGER.debug("trip "+ demand.ind2id(currentNode)+", bestTime "+ demand.getBestTime(currentNode));
            //LOGGER.debug("nearest depo at  "+ depo[1]+" ms");
            Car car = getCar(currentNode, depo);
            if(car == null){
                LOGGER.info("No car found, discarded trip "+demand.ind2id(currentNode));
                continue;
                // creates new car in the nearest depo
//                car = new Car(depo[0]);
//                cars[D].add(car);
//                LOGGER.info("New car added "+car.id);
                //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            }
            // if car drives out of depo
            if(car.getLastNode() < 0 ){
                //System.out.println(car.id+" drives out of depo "+depo[1]);
 //               LOGGER.debug(currentNode+" being add. timeTo="+depo[1]+", bestTime="+demand.getBestTime(currentNode));
                car.addTrip(currentNode, depo[1], demand.getStartTime(currentNode), demand.getBestTime(currentNode));
                currentNode = pair[currentNode];
                usedNodes.add(currentNode);
            }
            //car continues along the path
          //  System.out.println("car "+car.id+" drives from "+car.getLastNode());
            while(currentNode != n){
                int prevNode = car.getLastNode();
               // System.out.println("currentNode "+currentNode);
//                depo = central.findNearestStation(demand.getStartNodes(currentNode));
                int[] startSimNodes = demand.getEndNodes(prevNode);
                int[] endSimNodes = demand.getStartNodes(currentNode);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + demand.getBestTime(currentNode);
                if (car.hasCharge(travelTime)){
                    usedNodes.add(currentNode);
 //                   LOGGER.debug(currentNode+" being add. timeTo="+timeToTripStart+", timeOf="+demand.getBestTime(currentNode));
                    car.addTrip(currentNode, timeToTripStart, demand.getStartTime(currentNode), demand.getBestTime(currentNode));
                    currentNode  = pair[currentNode];
                }else{
                    parkCar(car);
                    break;
                }
            }//while
            if(currentNode == n){
               // LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
                //LOGGER.info("Car moved from D to W " + car.id);
                cars[W].add(car);
                cars[D].remove(car);
                //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            }
        }//main for
        int totaTrips = cars[D].stream().map(c->c.getTripCount()).mapToInt(Integer::intValue).sum();
        totaTrips += cars[W].stream().map(c->c.getTripCount()).mapToInt(Integer::intValue).sum();
        totaTrips += cars[C].stream().map(c->c.getTripCount()).mapToInt(Integer::intValue).sum();
        LOGGER.info("Total trip nodes in paths: "+totaTrips);
        LOGGER.info("Cars in driving list: "+cars[D].size());
        LOGGER.info("Cars in driving waiting list: "+cars[W].size());
        LOGGER.info("Cars in charging list: "+cars[C].size());
        LOGGER.info("Cars used: "+(cars[C].size()+cars[W].size()+cars[D].size()));
    }
    

    private void parkCar(Car car){
        int[] depo = central.findNearestStation(demand.getEndNodes(car.getLastNode()));
        car.addChargingStation(depo[0], depo[1]);
        cars[D].remove(car);
        cars[W].remove(car);
        cars[C].add(car);
    }
    
    private Car getCar(int trip, int[] depo){
        Car theCar = null;
        int bestTime = Integer.MAX_VALUE;
        // first check among waiting cars
        List<Car> toPark = new ArrayList<>();
        //cars[W].((c0, c1)->(c0.getLastNodeEndTime() - c1.getLastNodeEndTime()));
        List<Car> sortedCars = cars[W].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for (Car car: sortedCars){
            //System.out.println(car.id);
            if(canServe(car.getLastNode(), car.getLastNodeEndTime(), trip)){
                theCar = car;
                int currentNode = car.getLastNode();
                int[] startSimNodes = demand.getEndNodes(currentNode);
                int[] endSimNodes = demand.getStartNodes(trip);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + demand.getBestTime(trip);
//                LOGGER.debug("traveTime="+travelTime);
                if(car.hasCharge(travelTime)){
                    //if(travelTime < bestTime){
                    //LOGGER.info("Found car in waiting list");
                    theCar = car;
                    break;
                    //bestTime = travelTime;
                    }
                }else{
                    if(!car.hasCharge(20*60*1000))
                        toPark.add(car);
                    }
            }
       // }
        if(theCar != null){
            //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
           // LOGGER.info("Car moved from W to D " + theCar.id);
            cars[D].add(theCar);
            cars[W].remove(theCar);
            //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            toPark.forEach((car) -> {parkCar(car);});
            return theCar;
        }
        //second, search for cars in the nearest Depo
        //List<Car> sameDepo = cars[C].stream().filter(c->c.getLastNode() == -depo[0]).collect(Collectors.toList());
        int latestPossibleArrival = demand.getStartTime(trip) + maxWaitTime;
        sortedCars = cars[C].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for(Car car: sortedCars){
            int[] startSimNodes = new int[]{-car.getLastNode(), 0};
            int[] endSimNodes = demand.getStartNodes(trip);
            int timeFromeDepo = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
            if (car.getLastNodeEndTime() + timeFromeDepo <= latestPossibleArrival){
                //if(car.getLastNodeEndTime() + timeFromeDepo < bestTime){
                    //LOGGER.info("Found car in depo list");
                   // bestTime = car.getLastNodeEndTime() + timeFromeDepo;
                    theCar = car;
                    break;
                }
            }
        //}
        if(theCar != null){
           // LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            //LOGGER.info("Car moved from C to D " + theCar.id);
            cars[D].add(theCar);
            cars[C].remove(theCar);
            //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            return theCar;
        }
     //   if(canServe(depo[0], 0, trip)){
            //TODO add max car check
            theCar = new Car(depo[0]);
            cars[D].add(theCar);
            LOGGER.debug("New car added "+theCar.id);
            return theCar;
       // }
       // return theCar;
    }
    
    private boolean canServe(int prevNode, int prevDeparture, int trip){
        int[] startNodes = demand.getEndNodes(prevNode);
        int[] endNodes = demand.getStartNodes(trip);
        int timeToTrip = travelTimeProvider.getTravelTimeInMillis(startNodes, endNodes);
        int latestArrival = demand.getStartTime(trip) + maxWaitTime;
        return prevDeparture + timeToTrip <= latestArrival;
    }
}









    /**
     * Checks if the car has enough charge to serve the trip.
     * (Drive from the current location to the start, from start to target,
     * so that resulting charge is more or equal to min defined in Car.class;
     * @param car
     * @param node
     * @return 
     */
//    private boolean checkCharge(Car car, int travelTime){
//        //LOGGER.debug("car "+car.id);
//        int currentNode = car.getLastNode();
//        if(currentNode < 0){
//            currentNode = - currentNode;
//            LOGGER.error("Car from station in checkCharge "+car.id);
//            //return true;
//        }
//
//        //if(car.hasCharge(travelTime)){
//        //    return true;
//       // }else{
//            //parkCar(car);
////            car.addChargingStation(depo[0], (int) Math.round(depo[1]/13.88));
////            LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
////            LOGGER.info("Car moved from D or W to C" + car.id);
////            cars[D].remove(car);
////            cars[W].remove(car);
////            cars[C].add(car);
////            LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
//         //   return false;
//        //}
//        return car.hasCharge(travelTime);
//    }


//    public int[][] buildPaths(int sigma, StationCentral central){
//        int[][] longPaths = buildPaths(sigma);
//        int[][] segments = cutPaths(longPaths, central);
//        
//        //convertPathsToIds(segments);
//  
//        return segments;
//    }
    
//    public int[][] cutPaths(int[][] paths, StationCentral central){
//        List<int[]> result = new ArrayList<>();
//        for(int[] path : paths){
//            result.addAll(cutPath(path, central));
//        }
//        System.out.println("Total number of segments "+result.size());
//        return result.toArray(new int[result.size()][]);
//    }
    
    /**
     * 
     * @param path array of node indices
     * @param central link to stations
     * @return List of path segments not exceeding max ride distance with stations attached
     */
//    private List<int[]> cutPath(int[] path, StationCentral central){
//        List<int[]> result = new ArrayList<>();
//        int pathInd = 0;
//        while(pathInd < path.length){
//            //start of new segment
//            int drivenDist = 0;
//            List<Integer> newPath = new ArrayList<>();
//            int current = path[pathInd];
//            //find nearest depo node, compute distance from depo to start, from start to target, and from target
//            // to possible end station.
//            int[] depo1 = central.findNearestStation(startNodes[current]);
//            newPath.add(-depo1[0]);
//            int toStart = depo1[1];
//            int startToTarget = (int) Math.round(bestTimes[current]/1000*13.88);
//            int[] depo2 = central.findNearestStation(endNodes[current]);
//            int targetToDepo = depo2[1];
//            int distToDrive = toStart + startToTarget + targetToDepo;
//            
//            // if distance from here to next node, through the node, and to depo is less than limit
//            while(drivenDist + distToDrive < 190000){ // 200km
//                depo1 = depo2;
//                newPath.add(current); // add node to path, increase distance
//                drivenDist += (toStart + startToTarget);
//                if(++pathInd == path.length){// it's the last node in the segment
//                    break;
//                }
//                int next = path[pathInd];
//                toStart = (int) Math.round(travelTimeProvider.getTravelTimeInMillis(endNodes[current], startNodes[next]));
//                startToTarget = (int) Math.round(bestTimes[next]/1000*13.88);
//                depo2 = central.findNearestStation(endNodes[next]);
//                targetToDepo = depo2[1];
//                distToDrive = toStart + startToTarget + targetToDepo;
//                current = next;
//                
//            }
//            newPath.add(-depo1[0]);
//            drivenDist += targetToDepo;
//            System.out.println("Driven dist= "+drivenDist);
//            if(drivenDist > 200000){
//                LOGGER.warn("Max ride distance exceeded "+drivenDist);
//            }
//            result.add(newPath.stream().mapToInt(Integer::intValue).toArray());
//            //System.out.println(" Number of pieces "+newPath.size());
//        }
//        int pathSize = result.stream().map(sl->sl.length).mapToInt(Integer::intValue).sum() - result.size()*2;    
//        //System.out.println("nodes in original path "+path.length+", in parts "+pathSize);
//        //System.out.println(Arrays.toString(result.get(0)));
//        return result;
//    }