/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.StationCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import java.util.ArrayList;
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
    
    private final int maxCar;
    private final int maxWaitTime;
    private final Demand demand;
    private final TravelTimeProvider travelTimeProvider;
    private final  StationCentral central;
    private final ConfigTaxify config;
    int timeBuffer;
    List<Car>[] cars;

       
    public Solution(Demand demand, TravelTimeProvider travelTimeProvider, StationCentral central, ConfigTaxify config) {
        this.demand = demand;
        this.travelTimeProvider = travelTimeProvider;
        this.central = central;
        this.config = config;
        maxWaitTime = (int) (config.maxWaitTime - config.timeBuffer);
        timeBuffer = config.timeBuffer;
        maxCar = config.maxCar;
        cars = new List[3];
        cars[D] = new ArrayList<>(); //TODO remove D, 2 lists seems to be enough.
        cars[C] = new ArrayList<>();
        cars[W] = new ArrayList<>();
    }
           
    /**
     * Returns list size maxCar with cars that earned more value.
     * @return 
     */
    public List<Car> getAllCars(){
        List<Car> carList = new ArrayList<>();
        carList.addAll(cars[D]);
        carList.addAll(cars[W]);
        carList.addAll(cars[C]);
        if(maxCar < 0){
            return carList;
        }else{
            List<Car> filteredCars =  carList.stream()
                .sorted((car1, car2)-> demand.compareByValue(car1.getAllTrips(), car2.getAllTrips()))
                .limit(maxCar).collect(Collectors.toList());
            //LOGGER.debug(String.format("Max car allowed %d, cars in list %d", maxCar, filteredCars.size()));
            if(maxCar < filteredCars.size()){
                LOGGER.error("Max car exceeded");
            }
            return filteredCars;
        }
        
    }

    /**
     * Creates path and assigns cars without ride-sharing.
     * 
     */
    public void buildPaths(){
        int[] pair = demand.findMapCover(config.hkSigma);
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
                LOGGER.error("No car found, discarded trip "+demand.ind2id(currentNode));
                continue;
            }
            // if car drives out of depo
            if(car.getLastNode() < 0 ){
                //TODO add charge check
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
                int[] nextDepo = central.findNearestStation(demand.getEndNodes(currentNode));
                if (car.hasCharge(travelTime+nextDepo[1])){
                    usedNodes.add(currentNode);
 //                 LOGGER.debug(currentNode+" being add. timeTo="+timeToTripStart+", timeOf="+demand.getBestTime(currentNode));
                    car.addTrip(currentNode, timeToTripStart, demand.getStartTime(currentNode), demand.getBestTime(currentNode));
                    currentNode  = pair[currentNode];
                }else{
                     parkCar(car, depo);
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
        LOGGER.info("Cars in waiting list: "+cars[W].size());
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
    
    private void parkCar(Car car, int[] depo){
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
                int[] nextDepo = central.findNearestStation(demand.getEndNodes(trip));
//              LOGGER.debug("traveTime="+travelTime);
                if(car.hasCharge(travelTime+nextDepo[1])){
                    theCar = car;
                    break;
                }
            }else{
                if(!car.hasCharge())
                    parkCar(car, depo);
            }
        }
        if(theCar != null){
            cars[D].add(theCar);
            cars[W].remove(theCar);
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
            cars[D].add(theCar);
            cars[C].remove(theCar);
            return theCar;
        }
        if(depo[1] < latestPossibleArrival){
       // LOGGER.debug("New car added "+theCar.id);
            theCar = new Car(depo[0]);
            cars[D].add(theCar);
            return theCar;
        }
        return null;
    }
    
    private boolean canServe(int prevNode, int prevDeparture, int trip){
        int[] startNodes = demand.getEndNodes(prevNode);
        int[] endNodes = demand.getStartNodes(trip);
        int timeToTrip = travelTimeProvider.getTravelTimeInMillis(startNodes, endNodes);
        int latestArrival = demand.getStartTime(trip) + maxWaitTime;
        return prevDeparture + timeToTrip + timeBuffer <= latestArrival;
    }
}









    