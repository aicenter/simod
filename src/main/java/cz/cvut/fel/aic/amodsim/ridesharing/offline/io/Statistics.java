/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.io;

import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Demand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.GroupDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.GroupNormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class Statistics {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Statistics.class);
    
    Demand demand;
    
    List<Car> cars;
    
    TravelTimeProvider travelTimeProvider;
    
    AmodsimConfig config;
        
     public Statistics(Demand demand, List<Car> cars,  TravelTimeProvider travelTimeProvider, 
         AmodsimConfig config){
        this.demand = demand;
        this.cars = cars;
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
    }
    
    private  List<String[]> prepareResults(){
        
        Set<Integer> seenPickups = new HashSet<>();
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"car_id", "demand_id", "action", 
            "time", "node_id", "lat",  "lon"});
        for(Car car : cars){
//            LOGGER.debug("Car "+car.id + " num trips " + car.getTripCount() + " , length " + car.getAllTrips().length);
            int[] trips = car.getAllTrips();
            for(int tripInd : trips){
                int tripId = demand.indToTripId(tripInd);
             //    LOGGER.debug("Trip # "+tripId + " at position " + tripInd);
                if(seenPickups.contains(tripId)){
                    LOGGER.error("Duplicate pickup trip "+tripId);
                    //continue;
                }
                seenPickups.add(tripId);
                String[] pickupEntry = {String.valueOf(car.id), 
                    String.valueOf(tripId), "Pick-up", 
                    String.valueOf(demand.getStartTime(tripInd)), 
                    String.valueOf(demand.getStartNodeId(tripInd)),
                    String.valueOf(demand.getStartNode(tripInd).getLatitude()),
                    String.valueOf(demand.getStartNode(tripInd).getLongitude())
                };
                result.add(pickupEntry);
                
                String[] dropoffEntry = {String.valueOf(car.id), 
                    String.valueOf(tripId), "Drop-off", 
                    String.valueOf(demand.getEndTime(tripInd)),
                    String.valueOf(demand.getEndNodeId(tripInd)),
                    String.valueOf(demand.getEndNode(tripInd).getLatitude()),
                    String.valueOf(demand.getEndNode(tripInd).getLongitude())
                };
                result.add(dropoffEntry);
                    }
            }
        return result;
    }
    
    boolean dbg = false;
    private List<String[]> prepareGroupResults(){
        
        int maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        int timeToStart = config.ridesharing.offline.timeToStart;
        
        Set<Integer> seenPickups = new HashSet<>();
        int[] plansBysize = new int[config.ridesharing.vehicleCapacity+1];
        for(int i =0; i < plansBysize.length; i++){
            plansBysize[i]=0;
        }
        List<String[]> result = new ArrayList<>();
        result.add(0, new String[]{"car_id", "demand_id", "action", 
            "ept",  "action time",  "lpt",  "time-ept", "lpt-time" , "node_id", "lat",  "lon"});
        for(Car car : cars){
           int[] groups = car.getAllTrips();
           if(dbg) LOGGER.debug("\n"+car.id +" car with "+groups.length + " plans");
           SimulationNode node = null;
           int time = 0;
           for(int i = 0; i < car.getTripCount(); i++){
                if(dbg) LOGGER.debug("\nnode  "+ (node == null? "null":node.id) + ", time "+time);
                int groupInd = groups[i];
                DriverPlan plan = (DriverPlan) demand.getTripByIndex(groupInd);
                int planSize = plan.size()/2;
                if(dbg) LOGGER.debug("\nPlan size "+planSize + ", cost "+plan.cost+", totalTime "+plan.totalTime);
                for (PlanAction action : plan){
                     if( action instanceof PlanActionPickup ){
                           if(dbg) LOGGER.debug("\nPickup");  
                        PlanComputationRequest request = ((PlanActionPickup) action).request;
                        if(seenPickups.contains(request.getId())){
                             LOGGER.error("Duplicate pickup trip "+request.getId());
                            //continue;
                        }
                        seenPickups.add(request.getId());
                        
                        int requestTime = request.getOriginTime()*1000;
                         if(dbg)LOGGER.debug("\n request time "+requestTime);
                        String actionType = "Pickup";
                        int earliestPossibleTime = requestTime;
                        int actionTime = node == null ? requestTime + timeToStart : 
                            time + (int)travelTimeProvider.getExpectedTravelTime(node, request.getFrom());
                         if(dbg)LOGGER.debug("\n action time "+actionTime);
                        int latestPossibleTime = earliestPossibleTime + maxProlongation;
                         if(dbg)LOGGER.debug("\n ept "+earliestPossibleTime + ", lpt "+latestPossibleTime);
                        actionTime = actionTime > earliestPossibleTime ? actionTime : earliestPossibleTime;
                         if(dbg)LOGGER.debug("\n action time "+actionTime);
                        String[] entry = { 
                            String.valueOf(car.id), 
                            String.valueOf(request.getId()), 
                            actionType,
                            String.valueOf(earliestPossibleTime), 
                            String.valueOf(actionTime), 
                            String.valueOf(latestPossibleTime),
                            String.valueOf(actionTime - earliestPossibleTime),
                            String.valueOf(latestPossibleTime - actionTime),
                            String.valueOf(request.getFrom().id),
                            String.valueOf(request.getFrom().getLatitude()), 
                            String.valueOf(request.getFrom().getLongitude())
                        };
                        node = request.getFrom();
                        time = actionTime;
                        result.add(entry);
                            
                    }else if (action instanceof PlanActionDropoff ){
                         if(dbg)LOGGER.debug("\nDropoff");  
                        PlanComputationRequest request = ((PlanActionDropoff) action).request;
                        int requestTime = request.getOriginTime()*1000;
                        String actionType = "Dropoff";
                        int minTravelTime = request.getMinTravelTime()*1000;
                         if(dbg)LOGGER.debug("\n request time "+requestTime + ", mtt "+minTravelTime);
                        int earliestPossibleTime = requestTime + minTravelTime ;
                        int actionTime = time + minTravelTime;
                        int latestPossibleTime = earliestPossibleTime + maxProlongation;
                         if(dbg)LOGGER.debug("\n ept "+earliestPossibleTime + ", lpt "+latestPossibleTime);
                         if(dbg)LOGGER.debug("\n action time "+actionTime);
                        String[] entry = {
                            String.valueOf(car.id), 
                            String.valueOf(request.getId()), 
                            actionType,
                            String.valueOf(earliestPossibleTime), 
                            String.valueOf(actionTime),
                            String.valueOf(latestPossibleTime), 
                            String.valueOf(actionTime - earliestPossibleTime),
                            String.valueOf(latestPossibleTime - actionTime),
                            String.valueOf(request.getTo().id),
                            String.valueOf(request.getTo().getLatitude()),
                            String.valueOf(request.getTo().getLongitude())};
                        node = request.getTo();
                        time = actionTime;
                        result.add(entry);
                    }
                    plansBysize[planSize]++;
                }//plan actions
            }//car plans
        }//cars
        int tripCount = 0;
        for (int i = 1; i< plansBysize.length; i++){

            LOGGER.debug(plansBysize[i] +" plans of size " + i);
            tripCount += i*plansBysize[i];
        }
        LOGGER.debug("Total trips " + tripCount);

        return result;
    }
    
    
    private List<String[]> prepareGroupResults2(GroupNormalDemand groupDemand){
        
        NormalDemand normalDemand = groupDemand.getNormalDemand(); 
        int timeToStart = config.ridesharing.offline.timeToStart;
        int maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        Set<Integer> seenPickups = new HashSet<>();
        
        int[] plansBysize = new int[config.ridesharing.vehicleCapacity];
        for(int i = 0; i < plansBysize.length; i++){
            plansBysize[i]=0;
        }
        
        List<String[]> result = new ArrayList<>();
        result.add(0, new String[]{"car_id", "demand_id", "action", "ept", 
            "time", "lpt", "time-ept","lpt-time", "node_id", "lat",  "lon"});
        
        for(Car car : cars){
//           LOGGER.debug("\nCar " + car.id + " with "+car.getTripCount() + " trips");
           String carId = String.valueOf(car.id);
           int[] plans = car.getAllTrips();
//           LOGGER.debug("\nplans "+ Arrays.toString(plans));
           SimulationNode carNode = groupDemand.getStartNode(plans[0]);
           long carTime = groupDemand.getStartTime(plans[0]);
//            LOGGER.debug("\nstart node "+ carNode.id + ", start time "+carTime);
             
            // vehicle's plans
           for(int planId :plans){
              
                int[] plan = (int[]) demand.getTripByIndex(planId);
                
                int planSize = (plan.length-1)/2;
//                LOGGER.debug("\n" + planId +" plan size "+ planSize + " : "+Arrays.toString(plan));
                plansBysize[planSize]++;
                // plan actions
                for (int a = 1; a < plan.length; a++){
//                    LOGGER.debug("\n"+a+"th action #" + plan[a]+";Node "+carNode.id+ ", time "+ carTime);
                    int action = plan[a];
//                     LOGGER.debug("\naction " + action);
                   //pickup
                    if(action >= 0){ 
                        int tripInd = action; 
//                         LOGGER.debug("\npickup " + tripInd);
                        //debug
                        if( seenPickups.contains(tripInd)){
                           LOGGER.error("Duplicate pickup trip "+tripInd);
                            continue;
                        }
                        seenPickups.add(tripInd);
                        
                        SimulationNode nextNode = normalDemand.getStartNode(tripInd); 
                        String actionType = "Pickup"; 
                        long travelTime = travelTimeProvider.getExpectedTravelTime(carNode, nextNode);
                        long actionTime = carTime + travelTime;
                        int ept = normalDemand.getStartTime(tripInd);
                        int lpt = ept + maxProlongation;
//                        LOGGER.debug("\ntravel time" + travelTime + ", action time " + actionTime
//                            + ", ept "+ept +", lpt "+lpt);
                        if(a == 1){
//                             LOGGER.debug("\n1st action in plan, gd start" + groupDemand.getStartTime(planId));
                            actionTime = ept + timeToStart;
                        
                        }
                        actionTime = ept > actionTime ? ept : actionTime;
//                         LOGGER.debug("\naction time " + actionTime);
                        String[] entry = {
                            carId, 
                            String.valueOf(tripInd),
                            actionType, 
                            String.valueOf(ept),
                            String.valueOf(actionTime),
                            String.valueOf(lpt),
                            String.valueOf(actionTime - ept),
                            String.valueOf(lpt - actionTime), 
                            String.valueOf(nextNode.id), 
                            String.valueOf(nextNode.getLatitude()),  
                            String.valueOf(nextNode.getLongitude())
                        };
                        result.add(entry);
                        carTime = actionTime;
                        carNode = nextNode;
                    }
                    //dropoff
                    else {
                        int tripInd = -action - 1;
//                          LOGGER.debug("\ndropoff " + tripInd);
                        SimulationNode nextNode = normalDemand.getEndNode(tripInd);
                        long travelTime = travelTimeProvider.getExpectedTravelTime(carNode, nextNode);
                        long actionTime = carTime + travelTime;
                        int ept = normalDemand.getEndTime(tripInd);
                        int lpt = ept + maxProlongation;
//                        LOGGER.debug("\ntravel time" + travelTime + ", action time " + actionTime
//                            + ", ept "+ept +", lpt "+lpt);
                        String actionType = "Dropoff";
                        String[] entry = {
                            carId,  
                            String.valueOf(tripInd), 
                            actionType, 
                            String.valueOf(ept),
                            String.valueOf(actionTime), 
                            String.valueOf(lpt),
                            String.valueOf(actionTime - ept),
                            String.valueOf(lpt - actionTime), 
                            String.valueOf(nextNode.id),
                            String.valueOf(nextNode.getLatitude()),  
                            String.valueOf(nextNode.getLongitude())
                        };
                        result.add(entry);
                        carTime = actionTime;
                        carNode = nextNode;
                    }
                    
                }//plan actions
            }//car plans
        }//cars
        
        int tripCount = 0;
        for (int i = 1; i< plansBysize.length; i++){
            LOGGER.debug(plansBysize[i] +" plans of size " + i);
            tripCount += i*plansBysize[i];
        }
        LOGGER.debug("Total trips " + tripCount);

        return result;
    }
    
    private void writeCsv(String dir, String name, List<String[]> result){
        Date timeStamp = Calendar.getInstance().getTime();
     
        String filename = makeFilename(dir, name , timeStamp);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filename))) {
            csvWriter.writeAll(result);
        }
		catch(Exception ex){
			ex.printStackTrace();
		}
    }
    
    public void writeResults(String dir){
        String name = "eval_result";
        List<String[]> result = new ArrayList<>();
        if(demand instanceof GroupNormalDemand){
//            result = prepareGroupResults2((GroupNormalDemand) demand);
        } else if (demand instanceof GroupDemand){
             result = prepareGroupResults();
        }else if (demand instanceof NormalDemand){
             result = prepareResults();
        }
        if(! result.isEmpty()){
            writeCsv(dir, name, result);
        } else {
            LOGGER.debug("Empty result.");
        }
        
        result = prepareOccupancyStatistics(cars);
        if(! result.isEmpty()){
             writeCsv(dir, "occupancy", result);
        } else {
            LOGGER.debug("Empty result.");
        }
       
    }
    
      
    private static List<String[]> prepareOccupancyStatistics(List<Car> cars){
   
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"car_id", "time with passenger, s", "empty time, s", "with passenger, % from total"});
        long totalWithPassenger = 0;
        long totalEmpty = 0;
        for(Car car : cars){
            int busyTime = car.getTripTravelTime();
            int emptyTime = car.getEmptyTravelTime();
            String[] entry = new String[]{
                String.valueOf(car.getId()), String.valueOf(busyTime/1000), String.valueOf(emptyTime/1000),
                String.valueOf(100*busyTime/(busyTime+emptyTime))};
            result.add(entry);
            totalEmpty += emptyTime;
            totalWithPassenger += busyTime;
        }
        
        result.add(new String[]{"total", String.valueOf(totalWithPassenger),
            String.valueOf(totalEmpty), String.valueOf(100*totalWithPassenger/(totalEmpty + totalWithPassenger))});
        
        double avgWithPassenger = totalWithPassenger/cars.size();
        double avgEmpty = totalEmpty/cars.size();
        result.add(new String[]{"average", String.valueOf(avgWithPassenger), String.valueOf(avgEmpty),
            String.valueOf(100*avgWithPassenger/(avgEmpty+avgWithPassenger))});
        LOGGER.debug("Average time with passenger "+avgWithPassenger/1000);
        LOGGER.debug("Average empty time  "+ avgEmpty/1000);
        LOGGER.debug("Ratio " + (avgWithPassenger/(avgEmpty+avgWithPassenger)));
        return result;
    }
    
    private static String makeFilename(String dir, String name, Date timeStamp){
         
        //dir = dir.endsWith("/") ? dir : dir + "/";
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
        name = name + "_"+ timeString + ".csv";
        return FilenameUtils.concat(dir, name);
    }
    
    private static double tripDurationInSeconds(int startTime, int endTime){
        return (endTime - startTime)/1000;
    }

    private static String timeToString(int timeInMillis, String formatPrefix){
        int millis = timeInMillis % 1000;
        int second = (timeInMillis / 1000) % 60;
        int minute = (timeInMillis / (1000 * 60)) % 60;
        int hour = (timeInMillis / (1000 * 60 * 60)) % 24;
        long days = (timeInMillis / (1000 * 60 * 60 * 24)) + 1;
        String format = formatPrefix+"%02d %02d:%02d:%02d.%d";
        return String.format(format, days, hour, minute, second, millis);
    }
}

