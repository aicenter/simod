/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.entities;

import java.util.Arrays;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class Car {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Car.class);
    
    private static int count = 0;
   
    
    /**
     * Unique id of the car.
     */
    public final int id;
    
    int arr = 10000;
    
    private final int[] demandNodes;
    
    public int[][] times;
    
    private int size;
     
    private int tripTravelTime;
    
    private int emptyTravelTime;

    /**
     * Keeps track of it's route.
     * tripNodes are indices of demand entries in Demand 
     * in the order they were visited by the car.
     *  for nth demand node in demandNodes
     *  times[n][0] is actual start time
     *  times[n][1] is best travel time.
     */

    public Car(){
        id = count++;
        size = 0;
        demandNodes = new int[arr];
        times = new int[arr][2];
        tripTravelTime = 0;
        emptyTravelTime = 0;
    }
    
    private Car(Car car){
        this.id = car.id;
        this.size = car.size;
        this.demandNodes = car.demandNodes;
        this.times = car.times;
        this.tripTravelTime = car.tripTravelTime;
        this.emptyTravelTime = car.emptyTravelTime;
    }
    
    public Car copyCar(Car car){
        return new Car(car);
    }
    
    public int getId() {
        return id;
    }
    
    /**
     * @return ind of last node.
     */
    public int getLastDemandNode(){
        return demandNodes[size-1];
    }
    /**
     * @return ind of last node.
     */
    public int getFirstDemandNode(){
        return demandNodes[0];
    }
    /**
     * @return departure time from the last node.
     */
    public int getLastActionTime(){
        return times[size-1][1];
    }

    /**
     * @return car's arrival to the first node in the path.
     */
    public int getFirstActionTime(){
        return times[0][0];
    }

    /**
     * @return total number of visited nodes.
     */
    public int getSize() {
        return size;
    }
       
    /**
     * 
     * @return list of visited request nodes (without stations).
     */
    public int[] getAllDemandNodes(){
        return Arrays.stream(demandNodes).limit(size).toArray();
    }
        
    public void addDemandNode(int tripNode, int startTime, int tripDuration){
        demandNodes[size] = tripNode;
        times[size][0] = startTime;
        times[size][1] = startTime + tripDuration;
        tripTravelTime += tripDuration;
        int previousTime = size > 0 ? getLastActionTime() : 0;
        emptyTravelTime += (startTime - previousTime);
        size++;
    }

    public int getTripBusyTime() {
        return tripTravelTime;
    }

    public int getEmptyTime() {
        return emptyTravelTime;
    }
}
    
 
