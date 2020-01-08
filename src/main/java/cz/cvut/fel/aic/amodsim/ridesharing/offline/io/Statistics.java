/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.io;


import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
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
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.Graph;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
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
    
    List<double[]> occupancy;
       
        
    public Statistics(Demand demand, List<Car> cars,  TravelTimeProvider travelTimeProvider, 
         AmodsimConfig config){
        this.demand = demand;
        this.cars = cars;
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        
    }
    
    private  List<String[]> prepareResults(){
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"car_id", "demand_id", "action", 
            "time", "node_id", "lat",  "lon"});
        for(Car car : cars){
            int[] trips = car.getAllDemandNodes();
            for(int tripInd : trips){
                int tripId = demand.indToTripId(tripInd);

                result.add(new String[]{String.valueOf(car.id), 
                    String.valueOf(tripId), "Pick-up", 
                    String.valueOf(demand.getStartTime(tripInd)), 
                    String.valueOf(demand.getStartNodeId(tripInd)),
                    String.valueOf(demand.getStartNode(tripInd).getLatitude()),
                    String.valueOf(demand.getStartNode(tripInd).getLongitude()) });
                
                result.add(new String[] {String.valueOf(car.id), 
                    String.valueOf(tripId), "Drop-off", 
                    String.valueOf(demand.getEndTime(tripInd)),
                    String.valueOf(demand.getEndNodeId(tripInd)),
                    String.valueOf(demand.getEndNode(tripInd).getLatitude()),
                    String.valueOf(demand.getEndNode(tripInd).getLongitude())});
            }
        }
        return result;
    }
    

    private List<String[]> prepareGroupResults(GroupDemand gd){
        occupancy = new ArrayList<>();
        int timeToStart = config.ridesharing.offline.timeToStart;
        
        int[] plansBysize = new int[config.ridesharing.vga.maxGroupSize+1];
        for(int i =0; i < plansBysize.length; i++)         plansBysize[i]=0;
        List<String[]> result = new ArrayList<>();
        result.add(0, new String[]{"car_id", "demand_id", "action", 
            "ept",  "action time",  "lpt",  "time-ept", "lpt-time" , "node_id", "lat",  "lon"});
        
        for(Car car : cars){
            int passengerCount = 0;
            double[] timeWitnNPassengers = new double[config.ridesharing.vehicleCapacity + 1];
            Arrays.fill(timeWitnNPassengers, 0);
            timeWitnNPassengers[0]+= 60;
                      
           int[] groups = car.getAllDemandNodes();
         
           int firstPlanInd = car.getFirstDemandNode();
           int firstActionTimeMs = gd.getStartTime(firstPlanInd) + timeToStart;
           SimulationNode initialNode = gd.getStartNode(firstPlanInd);
           SimulationNode node = initialNode;
           int timeMs = firstActionTimeMs;

           for(int i = 0; i < car.getSize(); i++){

                int planIndex = groups[i];
                DriverPlan plan = gd.getPlanByIndex(planIndex);
                int planSize = plan.size()/2;
                plansBysize[planSize]++;

                for (PlanAction action : plan){

                     if( action instanceof PlanActionPickup ){
                        PlanComputationRequest request = ((PlanActionPickup) action).request;
                        SimulationNode nextNode = request.getFrom();
                        int earliestPossibleTimeS = request.getOriginTime();
                        int latestPossibleTimeS = request.getMaxPickupTime();
                        int travelTime = (int)travelTimeProvider.getExpectedTravelTime(node, nextNode);
                        int actionTimeMs = timeMs + travelTime;
                        actionTimeMs = actionTimeMs > earliestPossibleTimeS*1000 ?
                            actionTimeMs : earliestPossibleTimeS*1000;
                          
                        timeWitnNPassengers[passengerCount]+= (travelTime/1000.0);
                        passengerCount++;
                   
                        result.add(makeResultEntry(car.id, request.getId(),  "Pickup",
                            earliestPossibleTimeS, actionTimeMs, latestPossibleTimeS, nextNode));
             
                        node = nextNode;
                        timeMs = actionTimeMs;
                       
                    } else if (action instanceof PlanActionDropoff ){
                        PlanComputationRequest request = ((PlanActionDropoff) action).request;
                        SimulationNode nextNode = request.getTo();
                        int latestPossibleTimeS = request.getMaxDropoffTime();
                        int earliestPossibleTimeS = request.getOriginTime() + request.getMinTravelTime();
                        int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, nextNode);
                        int actionTimeMs = timeMs + travelTime;
                      
                        timeWitnNPassengers[passengerCount]+= (travelTime/1000.0);
                        passengerCount--;
            
                        result.add(makeResultEntry(car.id, request.getId(), "Dropoff",
                            earliestPossibleTimeS, actionTimeMs, latestPossibleTimeS, nextNode));
                        
                        node = nextNode;
                        timeMs = actionTimeMs;
                    }
                }//plan actions
            }//car plans
           occupancy.add(timeWitnNPassengers);
        }//cars
        int tripCount = 0;
        for (int i = 1; i< plansBysize.length; i++){
            LOGGER.debug(plansBysize[i] +" plans of size " + i);
            tripCount += i*plansBysize[i];
        }
        LOGGER.debug("Total trips " + tripCount);
        occupancyResults();
        return result;
    }
    
    private String[] makeResultEntry(int carId, int requestId, String actionType, int earliestPossibleTime, 
        int actionTimeMs,  int latestPossibleTime, SimulationNode node){
        
        int actionTime = Math.round(actionTimeMs/1000);
        return new String[]{String.valueOf(carId), String.valueOf(requestId), actionType,
            String.valueOf(earliestPossibleTime), String.valueOf(actionTime), String.valueOf(latestPossibleTime), 
            String.valueOf(actionTime - earliestPossibleTime), String.valueOf(latestPossibleTime - actionTime),
            String.valueOf(node.id), String.valueOf(node.getLatitude()), String.valueOf(node.getLongitude())};
    }
    
    private void occupancyResults(){
        int capacities = config.ridesharing.vga.maxGroupSize + 1;
        int columns = capacities + 1;
        int[] total = new int[columns];
        Arrays.fill(total, 0);
        
        List<String[]> result = new ArrayList<>();
        String[] header = new String[columns];
        header[0] = "car_id";
        for(int i = 1; i < columns; i++){
            header[i] = String.format("onboard_%s", (i-1)); 
        }
        result.add(header);

        for(int carId = 0; carId < occupancy.size(); carId++){
            double car[] = occupancy.get(carId);
            String[] entry = new String[columns];
            entry[0] = String.valueOf(carId);
            for(int count = 0; count < capacities; count++){
                int col = count+1;
                total[count]+=car[count];
                entry[col] = String.valueOf(car[count]);
            }
            result.add(entry);
        }

        writeCsv(config.amodsimExperimentDir, "passenger_stats", result);
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
    
    /**
     * Saves evaluation results and passenger 
     * statistics.
     * Creates 2 .csv files:
     *  1) eval_results - routes of all cars
     *  2) passenger_stats - travel time by number of passengers for all cars
     * 
     * @param dir path to directory for results
     */
    public void writeResults(String dir){
        String name = "eval_result";
        List<String[]> result = new ArrayList<>();
        if (demand instanceof GroupDemand){
             result = prepareGroupResults((GroupDemand) demand);
        }else if (demand instanceof NormalDemand){
             result = prepareResults();
        }
        if(! result.isEmpty()){
            writeCsv(dir, name, result);
        } else {
            LOGGER.debug("Empty result.");
        }
    }
    
      

    
    private static String makeFilename(String dir, String name, Date timeStamp){
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
         name = name + "_"+ timeString + ".csv";
        return FilenameUtils.concat(dir, name);
    }
    
    
    private static void writeDemandStatistics(List<TripTaxify<SimulationNode>> trips,
        TravelTimeProvider travelTimeProvider, String dir, String filename){
        
        List<String[]> result = new ArrayList<>();
        
        for (TripTaxify<SimulationNode> trip : trips){
            Long shortestPathInMs = travelTimeProvider.getExpectedTravelTime(trip.getStartNode(), trip.getEndNode());
            result.add(new String[]{String.valueOf(trip.id), 
                                    String.valueOf(trip.getStartTime()/1000), 
                                    String.valueOf(shortestPathInMs/1000.0),

                                    String.valueOf(trip.getStartNode().id),
                                    String.valueOf(trip.getStartNode().getLatitude()),
                                    String.valueOf(trip.getStartNode().getLongitude()),

                                    String.valueOf(trip.getEndNode().id),
                                    String.valueOf(trip.getEndNode().getLatitude()),
                                    String.valueOf(trip.getEndNode().getLongitude())   });
        }
        
        result.add(0, new String[]{"tripId", "startTime", "lengthSec",
            "startNode",  "startLat", "startLon", "endNode", "endLat", "endLon"});
        String filepath  =  FilenameUtils.concat(dir, filename);
        
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filepath))) {
                csvWriter.writeAll(result);
        }catch(Exception ex){
                ex.printStackTrace();
        }
    }
    
    
    public static void writeGraphStatisticss(Graph<SimulationNode, SimulationEdge> graph,
        String dir, String filename){
      
//        double totalLengthM = graph.getAllEdges().stream().mapToDouble((e) -> e.getLengthCm()).sum()/100.0;
//        double totalTimeS = graph.getAllEdges().stream().mapToDouble((e) -> 
//            e.getLengthCm()/e.getAllowedMaxSpeedInCmPerSecond()).sum();
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"id", "lengthM", "timeS"});
        for(SimulationEdge edge: graph.getAllEdges()){
            double lengthM = edge.getLengthCm()/100.0;
            double timeS = edge.getLengthCm()/edge.getAllowedMaxSpeedInCmPerSecond();
            result.add(new String[]{String.valueOf(edge.getStaticId()),
                                    String.valueOf(lengthM),
                                    String.valueOf(timeS)});
        }
         try (CSVWriter csvWriter = new CSVWriter(new FileWriter(
             FilenameUtils.concat(dir, filename)))) {
                csvWriter.writeAll(result);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        
        
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

