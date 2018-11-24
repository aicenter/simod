/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class Car {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Car.class);
    private static int count = 0;
    private static  int maxChargeMs = 14400000; // TODO get them here from config?
    private static int chargingTimeMs = 7200000;
    private static int minCharge = maxChargeMs/25;
    int arr = 1000;
    int[] nodes;
    int[][] times;
    public final int id;
    int chargeLeft;
    int size;
    int depoCount;
    Map<Integer, Integer> chargeAtDepo;
    
    public Car(int depoNode) {
        id = count++;
        size = 1;
        chargeLeft = maxChargeMs;
        nodes = new int[arr];
        nodes[0] = -depoNode;
        times = new int[arr][2];
        times[0] = new int[]{0,0}; 
        depoCount = 1;
        chargeAtDepo = new HashMap<>();
    }
    
    public int getTripCount(){
        return size - depoCount;
    }
    public int getFirstNode(){
        return nodes[0];
    }
    public int getLastNode(){
        return nodes[size-1];
    }
    public int getLastNodeEndTime(){
        return times[size-1][1];
    }
    public int getFirstNodeStartTime(){
        return times[1][0];
    }
    public int getChargeLeft() {
        return chargeLeft;
    }
    public int getSize() {
        return size;
    }
    
    /**
     *  Return true if the car travel the given time not going below
     * min charge level.
     * @param travelTime, time in millis
     * @return 
     */
    public boolean hasCharge(int travelTime){
        //LOGGER.debug(id+" charge left " + chargeLeft);
        return (chargeLeft - travelTime) >= minCharge;
    }
    public boolean hasCharge(){
        //LOGGER.debug(id+" charge left " + chargeLeft);
        return chargeLeft >= minCharge;
    }
    
    /**
     *  adds new trip to the route.
     * @param node, index of the trip in Demand
     * @param bestTimeToStart, best possible time to get to the start of the inserted trip
     * @param startTime, start time of the trip
     * @param tripDuration, best possible time for the trip itself
     */
    public void addTrip(int node, int bestTimeToStart, int startTime, int tripDuration){
        //LOGGER.debug(node+" received by "+id+"; charge "+chargeLeft+", toStart="+bestTimeToStart+", duration in sec="+tripDuration/1000
      // + "; app travel time "+ (tripDuration+bestTimeToStart)/1000);
        if(chargeLeft < tripDuration+bestTimeToStart){
            LOGGER.error("Not enough charge, car "+id);
        }
        nodes[size] = node;
        //LOGGER.debug(id+": toStart="+bestTimeToStart+", lastEndTime="+times[size-1][1]);
       int earliestPossibleArrival = times[size-1][1] + bestTimeToStart;
        //LOGGER.debug(id+": earliestPossibleArrival="+earliestPossibleArrival+", start="+startTime);
        times[size][0] = Math.max(startTime, earliestPossibleArrival);
       // LOGGER.debug(id+":startTime="+times[size][0]);
        times[size][1] = times[size][0] + tripDuration;
      //  LOGGER.debug(id+": endTime="+times[size][1]);
        chargeLeft -= (bestTimeToStart + tripDuration);
        size++;
        if(size == arr){
            LOGGER.error("Warning: array size exceeded ");
            realloc();
        }
    }
   /**
    * 
    * @param station, id of SimulationNode there station is located
    * @param bestTimeToNode, best possible time to the station from the last location
    */
    public void addChargingStation(int station, int bestTimeToNode){
        nodes[size] = -station;
        times[size][0] = getLastNodeEndTime() + bestTimeToNode;
        chargeLeft -= bestTimeToNode;
        chargeAtDepo.put(size, chargeLeft);
        if(chargeLeft < 0){
            LOGGER.error("Car arrives to the station with less than zero charge!");
           // LOGGER.error("Path "+Arrays.toString(nodes));
        }
        //LOGGER.debug("Car "+id+" arrives to the station with charge "+chargeLeft);
        times[size][1] = times[size][0] + chargingTimeMs;
        chargeLeft = maxChargeMs;
        size++;
        depoCount++;
    }

    /**
     * Returns the whole route for Stats.
     * @return  list of values for each node visited in the following order
     * "car_id", "trip_id", "start_time","end_time","start_lat", "start_lon", "end_lat", "end_lon", "is_depo";
     */
    public List<int[]> getPathStats(){
       
        List<int[]> paths = new ArrayList<>();
        for(int i = 0; i<size;i++){
            int node = nodes[i];
            int[] result = new int[10];
            result[0] = id;
            result[1] = node;
            result[3] = times[i][0];
            result[4] = times[i][1];
            if(i > 0 && node < 0){
                result[9] = chargeAtDepo.get(i);
            }else{
                result[9] = 0;
            }
            paths.add(result);
        }
        paths.get(size-1)[9] = chargeLeft;
        return paths;
    }
    
        
    private void realloc(){
        nodes = Arrays.copyOf(nodes, arr*=2);
        times = Arrays.copyOf(times, arr);
        
    }
    
}
