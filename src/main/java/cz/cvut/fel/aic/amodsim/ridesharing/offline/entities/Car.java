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
//        LOGGER.debug("trip start "+ startTime + ", length "+tripDuration);
//        LOGGER.debug("car last action time "+getLastActionTime());
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

//public void addDepo(int node, int arrivalTime, int stayDuration){
//    nodes[size] = -node;
//    times[size][0] = arrivalTime;
//    times[size][1] = arrivalTime + stayDuration;
//    int previousTime = size > 0 ? getLastActionTime() : 0;
//    emptyTravelTime += (arrivalTime - previousTime);
//    size++;
//}

//    private void realloc(){
//        nodes = Arrays.copyOf(nodes, arr*= 2);
//        times = Arrays.copyOf(times, arr);
//        
//    }
}

    /**
//     * Returns the car statistics for {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.Stats}
//     * @return  list of values for each node visited in the following order
//     * car_id  passenger_id  start_time   end_time   time_since_charging  times_charged   last_charge_location
//     */
//    public List<int[]> getPathStats(){
//        //LOGGER.debug("Car  "+id);
//        List<int[]> paths = new ArrayList<>();
//        int lastDepo = 0;
//        int time = 0;
//        for(int i = 0; i < size; i++){
//            //LOGGER.debug("left depo at "+leftDepoAt);
//            int node = nodes[i];
//                if(node < 0){
//                    node = -node;
//                    lastDepo = node;
//            }
//            int[] result = new int[5];
//            result[0] = id; //car_id
//            result[1] = node; //passenger_id
//            result[2] = times[i][0];//start_time
//            result[3] = times[i][1];//end_time
//            result[4] = lastDepo;
//            
//            paths.add(result);
//        }
//        return paths;
//    }
//    

    
//    /**
//     * Adds new trip to the route.
//     * @param node index of the trip in {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.Demand}
//     * @param bestTimeToStart best possible time to get to the start of the inserted trip
//     * @param startTime start time of the trip
//     * @param tripDuration best possible time for the trip itself
//     */
//    public void addDemandNode(int node, int bestTimeToStart, int startTime, int tripDuration){
//        nodes[size] = node;
//        
//       int earliestPossibleArrival = times[size-1][1] + bestTimeToStart;
//       if(earliestPossibleArrival - startTime >= maxWaitTime){
//           LOGGER.error(String.format("%d arrives to %d  %f sec late: earliestPossibleArrival %f, start %f",
//               id, node, (earliestPossibleArrival-startTime)/1000.0,  earliestPossibleArrival/1000.0, startTime/1000.0));
//       }
//        
//        times[size][0] = Math.max(startTime, earliestPossibleArrival);
//        times[size][1] = times[size][0] + tripDuration;
//        timeTraveled[size] = bestTimeToStart + tripDuration;
//        size++;
//        if(size == arr){
//            LOGGER.error("Warning: array size exceeded ");
//            realloc();
//        }
//    }
