/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.*;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.StationCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.Car;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
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
public class SolutionRS {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolutionRS.class);
    private final int D = 0;
    private final int C = 1;
    private final int W = 2;
    
    private final int maxCar;
    private final int maxWaitTime;
    private final Ridesharing ridesharing;
    private final TravelTimeProvider travelTimeProvider;
    private final  StationCentral central;
    private final ConfigTaxify config;
    int timeBuffer;
    List<Car>[] cars;

       
    public SolutionRS(Ridesharing rs, TravelTimeProvider travelTimeProvider, StationCentral central, ConfigTaxify config) {
        this.ridesharing = rs;
        this.travelTimeProvider = travelTimeProvider;
        this.central = central;
        this.config = config;
        maxWaitTime = (int) (config.maxWaitTime * 0.8);
        timeBuffer = config.timeBuffer;
        maxCar = config.maxCar;
        cars = new List[3];
        cars[D] = new ArrayList<>(); //TODO remove D, 2 lists are enough;
        cars[C] = new ArrayList<>();
        cars[W] = new ArrayList<>();
    }
           
    /**
     * Returns list of all cars.
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
     * Creates path and assigns cars without ride-sharing.
     * 
     */
    public void buildPaths(){
        int[] pair = ridesharing.findMapCover(config.hkSigma);
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
            LOGGER.debug("node ind "+currentNode);
            int[] starts = ridesharing.getStartNodes(currentNode);
            LOGGER.debug(Arrays.toString(starts));
            int[] depo = central.findNearestStation(starts);
            
            LOGGER.debug("trip "+currentNode+", bestTime "+ ridesharing.getBestTime(currentNode));
            LOGGER.debug("nearest depo at  "+ depo[1]+" ms");
            Car car = getCar(currentNode, depo);
            if(car == null){
                LOGGER.error("No car found, discarded trip ");//+demand.ind2id(currentNode)
                continue;
            }
            // if car drives out of depo
            if(car.getLastNode() < 0 ){
                car.addTrip(currentNode, depo[1], ridesharing.getStartTime(currentNode), ridesharing.getBestTime(currentNode));
                currentNode = pair[currentNode];
                usedNodes.add(currentNode);
            }
            //car continues along the path
            LOGGER.debug("car "+car.id+" drives from "+car.getLastNode());
            while(currentNode != n){
                int prevNode = car.getLastNode();
                int[] startSimNodes = ridesharing.getEndNodes(prevNode);
                int[] endSimNodes = ridesharing.getStartNodes(currentNode);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + ridesharing.getBestTime(currentNode);
                if (car.hasCharge(travelTime)){
                    usedNodes.add(currentNode);
                   LOGGER.debug(currentNode+" being added. timeTo="+timeToTripStart+", timeOf="+ridesharing.getBestTime(currentNode));
                    car.addTrip(currentNode, timeToTripStart, ridesharing.getStartTime(currentNode), 
                                                              ridesharing.getBestTime(currentNode));
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
        int[] depo = central.findNearestStation(ridesharing.getEndNodes(car.getLastNode()));
        car.addChargingStation(depo[0], depo[1]);
        cars[D].remove(car);
        cars[W].remove(car);
        cars[C].add(car);
    }
    
    private Car getCar(int trip, int[] depo){
        double minCharge = config.maxChargeMs/12; //20 minutes
        Car theCar = null;
        // first check among waiting cars
        List<Car> toPark = new ArrayList<>();
        List<Car> sortedCars = cars[W].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for (Car car: sortedCars){
            if(canServe(car.getLastNode(), car.getLastNodeEndTime(), trip)){
                theCar = car;
                int currentNode = car.getLastNode();
                int[] startSimNodes = ridesharing.getEndNodes(currentNode);
                int[] endSimNodes = ridesharing.getStartNodes(trip);
                int timeToTripStart = travelTimeProvider.getTravelTimeInMillis(startSimNodes, endSimNodes);
                int travelTime = timeToTripStart + ridesharing.getBestTime(trip);
                LOGGER.debug("       traveTime="+travelTime);
                if(car.hasCharge(travelTime)){
                     theCar = car;
                    break;
                }
            }else{
                if(!car.hasCharge((int) minCharge))
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
        int latestPossibleArrival = ridesharing.getStartTime(trip) + maxWaitTime;
        sortedCars = cars[C].stream().sorted(Comparator.comparingInt(Car::getLastNodeEndTime))
            .collect(Collectors.toList());
        for(Car car: sortedCars){
            int[] startSimNodes = new int[]{-car.getLastNode(), 0};
            int[] endSimNodes = ridesharing.getStartNodes(trip);
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
        int[] startNodes = ridesharing.getEndNodes(prevNode);
        int[] endNodes = ridesharing.getStartNodes(trip);
        int timeToTrip = travelTimeProvider.getTravelTimeInMillis(startNodes, endNodes);
        int latestArrival = ridesharing.getStartTime(trip) + maxWaitTime;
        return prevDeparture + timeToTrip + timeBuffer <= latestArrival;
    }
}









    