/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.io;

import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class SolutionStatistics {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolutionStatistics.class);

    private final Map<Integer, int[]>  carPlans;
    
    private final int vehicleCapacity;
    
    public SolutionStatistics(AmodsimConfig config){
        carPlans = new HashMap<>();
        vehicleCapacity = config.ridesharing.vehicleCapacity;
    }
    
    public void addAction(int carId, int actionPositionInPlan, int actionTime){
        if(!carPlans.keySet().contains(carId)){
            carPlans.put(carId, new int[2*vehicleCapacity]);
        }
    }
   
    
//    private List<String[]> prepareGroupResults(){
//        
//        int maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
//        int timeToStart = config.ridesharing.offline.timeToStart;
//        
//        Set<Integer> seenPickups = new HashSet<>();
//        int[] plansBysize = new int[config.ridesharing.vehicleCapacity+1];
//        for(int i =0; i < plansBysize.length; i++){
//            plansBysize[i]=0;
//        }
//        List<String[]> result = new ArrayList<>();
//        result.add(0, new String[]{"car_id", "demand_id", "action", 
//            "ept",  "action time",  "lpt",  "time-ept", "lpt-time" , "node_id", "lat",  "lon"});
//        for(Car car : cars){
//           int[] groups = car.getAllTrips();
//           LOGGER.debug("\n"+car.id +" car with "+groups.length + " plans");
//           SimulationNode node = null;
//           int time = 0;
//           for(int i = 0; i < car.getTripCount(); i++){
//                LOGGER.debug("\nnode  "+ (node == null? "null":node.id) + ", time "+time);
//                int groupInd = groups[i];
//                DriverPlan plan = (DriverPlan) demand.getTripByIndex(groupInd);
//                int planSize = plan.size()/2;
//                LOGGER.debug("\nPlan size "+planSize + ", cost "+plan.cost+", totalTime "+plan.totalTime);
//                for (PlanAction action : plan){
//                     if( action instanceof PlanActionPickup ){
//                           LOGGER.debug("\nPickup");  
//                        PlanComputationRequest request = ((PlanActionPickup) action).request;
//                        if(seenPickups.contains(request.getId())){
//                            LOGGER.error("Duplicate pickup trip "+request.getId());
//                            //continue;
//                        }
//                        seenPickups.add(request.getId());
//                        
//                        int requestTime = request.getOriginTime()*1000;
//                        LOGGER.debug("\n request time "+requestTime);
//                        String actionType = "Pickup";
//                        int earliestPossibleTime = requestTime;
//                        int actionTime = node == null ? requestTime + timeToStart : 
//                            time + (int)travelTimeProvider.getExpectedTravelTime(node, request.getFrom());
//                        LOGGER.debug("\n action time "+actionTime);
//                        int latestPossibleTime = earliestPossibleTime + maxProlongation;
//                        LOGGER.debug("\n ept "+earliestPossibleTime + ", lpt "+latestPossibleTime);
//                        actionTime = actionTime > earliestPossibleTime ? actionTime : earliestPossibleTime;
//                        LOGGER.debug("\n action time "+actionTime);
//                        String[] entry = { 
//                            String.valueOf(car.id), 
//                            String.valueOf(request.getId()), 
//                            actionType,
//                            String.valueOf(earliestPossibleTime), 
//                            String.valueOf(actionTime), 
//                            String.valueOf(latestPossibleTime),
//                            String.valueOf(actionTime - earliestPossibleTime),
//                            String.valueOf(latestPossibleTime - actionTime),
//                            String.valueOf(request.getFrom().id),
//                            String.valueOf(request.getFrom().getLatitude()), 
//                            String.valueOf(request.getFrom().getLongitude())
//                        };
//                        node = request.getFrom();
//                        time = actionTime;
//                        result.add(entry);
//                            
//                    }else if (action instanceof PlanActionDropoff ){
//                        LOGGER.debug("\nDropoff");  
//                        PlanComputationRequest request = ((PlanActionDropoff) action).request;
//                        int requestTime = request.getOriginTime()*1000;
//                        String actionType = "Dropoff";
//                        int minTravelTime = request.getMinTravelTime()*1000;
//                        LOGGER.debug("\n request time "+requestTime + ", mtt "+minTravelTime);
//                        int earliestPossibleTime = requestTime + minTravelTime ;
//                        int actionTime = time + minTravelTime;
//                        int latestPossibleTime = earliestPossibleTime + maxProlongation;
//                        LOGGER.debug("\n ept "+earliestPossibleTime + ", lpt "+latestPossibleTime);
//                        LOGGER.debug("\n action time "+actionTime);
//                        String[] entry = {
//                            String.valueOf(car.id), 
//                            String.valueOf(request.getId()), 
//                            actionType,
//                            String.valueOf(earliestPossibleTime), 
//                            String.valueOf(actionTime),
//                            String.valueOf(latestPossibleTime), 
//                            String.valueOf(actionTime - earliestPossibleTime),
//                            String.valueOf(latestPossibleTime - actionTime),
//                            String.valueOf(request.getTo().id),
//                            String.valueOf(request.getTo().getLatitude()),
//                            String.valueOf(request.getTo().getLongitude())};
//                        node = request.getTo();
//                        time = actionTime;
//                        result.add(entry);
//                    }
//                    plansBysize[planSize]++;
//                }//plan actions
//            }//car plans
//        }//cars
//        int tripCount = 0;
//        for (int i = 1; i< plansBysize.length; i++){
//
//            LOGGER.debug(plansBysize[i] +" plans of size " + i);
//            tripCount += i*plansBysize[i];
//        }
//        LOGGER.debug("Total trips " + tripCount);
//
//        return result;
//    }
 
    
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
    
//    public void writeResults(String dir){
//        String name = "eval_result";
//        List<String[]> result = new ArrayList<>();
//        if(demand instanceof GroupNormalDemand){
//            result = prepareGroupResults2((GroupNormalDemand) demand);
//        } else if (demand instanceof GroupDemand){
//             result = prepareGroupResults();
//        }else if (demand instanceof NormalDemand){
//             result = prepareResults();
//        }
//        if(! result.isEmpty()){
//            writeCsv(dir, name, result);
//        } else {
//            LOGGER.debug("Empty result.");
//        }
//        
//        result = prepareOccupancyStatistics(cars);
//        if(! result.isEmpty()){
//             writeCsv(dir, "occupancy", result);
//        } else {
//            LOGGER.debug("Empty result.");
//        }
//       
//    }
    
      
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

