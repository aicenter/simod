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
    int timeBuffer;
    List<Car>[] cars;
    private int minCharge;
    
    
    public Solution(Demand demand, TravelTimeProvider travelTimeProvider, StationCentral central, AmodsimConfig config) {
        this.demand = demand;
        this.travelTimeProvider = travelTimeProvider;
        this.central = central;
        this.config = config;
        maxWaitTime = config.amodsim.ridesharing.maxWaitTime * 800;
        timeBuffer = demand.getTimeBuffer();
        maxCar = 10000;
        sigma = 7; //minutes
        cars = new List[3];
        cars[D] = new ArrayList<>();
        cars[C] = new ArrayList<>();
        cars[W] = new ArrayList<>();
        minCharge = 20*60*1000;
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
            int[] starts = demand.getStartNodes(currentNode);
            //LOGGER.debug(Arrays.toString(starts));
            int[] depo = central.findNearestStation(starts);
            
            //LOGGER.debug("trip "+ demand.ind2id(currentNode)+", bestTime "+ demand.getBestTime(currentNode));
            //LOGGER.debug("nearest depo at  "+ depo[1]+" ms");
            Car car = getCar(currentNode, depo);
            if(car == null){
                LOGGER.info("No car found, discarded trip "+demand.ind2id(currentNode));
                continue;
            }
            // if car drives out of depo
            if(car.getLastNode() < 0 ){
                car.addTrip(currentNode, depo[1], demand.getStartTime(currentNode), demand.getBestTime(currentNode));
                currentNode = pair[currentNode];
                usedNodes.add(currentNode);
            }
            //car continues along the path
           // LOGGER.debug("car "+car.id+" drives from "+car.getLastNode());
            while(currentNode != n){
                int prevNode = car.getLastNode();
                int[] startSimNodes = demand.getEndNodes(prevNode);
                int[] endSimNodes = demand.getStartNodes(currentNode);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + demand.getBestTime(currentNode);
                if (car.hasCharge(travelTime)){
                    usedNodes.add(currentNode);
 //                 LOGGER.debug(currentNode+" being add. timeTo="+timeToTripStart+", timeOf="+demand.getBestTime(currentNode));
                    car.addTrip(currentNode, timeToTripStart, demand.getStartTime(currentNode), demand.getBestTime(currentNode));
                    currentNode  = pair[currentNode];
                }else{
                    parkCar(car);
                    break;
                }
            }//while
            if(currentNode == n){
               // LOGGER.debug("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
                //LOGGER.debug("Car moved from D to W " + car.id);
                cars[W].add(car);
                cars[D].remove(car);
                //LOGGER.debug("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
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
        // first check among waiting cars
        List<Car> toPark = new ArrayList<>();
        List<Car> sortedCars = cars[W].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for (Car car: sortedCars){
            if(canServe(car.getLastNode(), car.getLastNodeEndTime(), trip)){
                theCar = car;
                int currentNode = car.getLastNode();
                int[] startSimNodes = demand.getEndNodes(currentNode);
                int[] endSimNodes = demand.getStartNodes(trip);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + demand.getBestTime(trip);
//              LOGGER.debug("traveTime="+travelTime);
                if(car.hasCharge(travelTime)){
                     theCar = car;
                    break;
                }
            }else{
                if(!car.hasCharge(minCharge))
                toPark.add(car);
            }
        }
        if(theCar != null){
            //LOGGER.info("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
           // LOGGER.debug("Car moved from W to D " + theCar.id);
            cars[D].add(theCar);
            cars[W].remove(theCar);
            //LOGGER.debug("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            toPark.forEach((car) -> {parkCar(car);});
            return theCar;
        }
        //second, search for cars in the nearest Depo
        int latestPossibleArrival = demand.getStartTime(trip) + maxWaitTime;
        sortedCars = cars[C].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for(Car car: sortedCars){
            int[] startSimNodes = new int[]{-car.getLastNode(), 0};
            int[] endSimNodes = demand.getStartNodes(trip);
            int timeFromeDepo = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
            if (car.getLastNodeEndTime() + timeFromeDepo + timeBuffer <= latestPossibleArrival){
                theCar = car;
                break;
                }
            }
        if(theCar != null){
           // LOGGER.debug("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            //LOGGER.debug("Car moved from C to D " + theCar.id);
            cars[D].add(theCar);
            cars[C].remove(theCar);
            //LOGGER.debug("D: "+cars[D].size()+", W: "+cars[W].size()+", C: "+cars[C].size());
            return theCar;
        }
        theCar = new Car(depo[0]);
        cars[D].add(theCar);
        LOGGER.debug("New car added "+theCar.id);
        return theCar;
    }
    
    private boolean canServe(int prevNode, int prevDeparture, int trip){
        int[] startNodes = demand.getEndNodes(prevNode);
        int[] endNodes = demand.getStartNodes(trip);
        int timeToTrip = travelTimeProvider.getTravelTimeInMillis(startNodes, endNodes);
        int latestArrival = demand.getStartTime(trip) + maxWaitTime;
        return prevDeparture + timeToTrip + timeBuffer <= latestArrival;
    }
}









    