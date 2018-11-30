/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.io;

import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.StationCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.Demand;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author olga
 */
public class Stats {
    
    /**
     * Writes results for evaluation script.

     * @param cars 
     * @param demand
     * @param config
     * @param central
     * @param timeStamp
     * @throws IOException 
     * 
     */
    public static void writeEvaluationCsv(List<Car> cars, Demand demand, ConfigTaxify config, StationCentral central,
        Date timeStamp) throws IOException{
        String name = "eval_result";
        String filename = makeFilename(config.dir, name , timeStamp);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filename))) {
            csvWriter.writeNext(new String[]{"car_id", "passenger_id",
                                             "ride_start_time", "ride_end_time",
                                             "ride_value",
                                             "pickup_lat", "pickup_lng",  "dropoff_lat", "dropoff_lng", 
                                             "distance_driven_since_charging", "times_charged", 
                                             "last_charge_location"});
            for(Car car : cars){
                csvWriter.writeAll(pathToEval(car.getPathStats(), demand, central, config.speed, config.startYM));
                //pathToEval(car.getPathStats(), demand, central, config.speed, config.startYM);
            }
        }
		catch(Exception ex){
			ex.printStackTrace();
		}
    }
    /**
     *  Data for evaluation script: 
     *  car_id      passenger_id    ride_start_time     ride_end_time ride_value,
     *  pickup_lat  pickup_lng      dropoff_lat         dropoff_lng
     *  distance_driven_since_charging (in km)       times_charged          last_charge_location (depot id)
     * 
     * @param path
     * @param demand
     * @return 
     */
    private static List<String[]> pathToEval(List<int[]> path, Demand demand, StationCentral central, 
                        double speed, String start){
        // Car returns int[] with data in following order:
        // 0: car_id, 1: passenger_id, 2: start_time, 3:end_time, 4:time_since_charge(millis)  
        // 5: times_charged, 6:last_charge_location(-SimulationNode.id)
        List<String[]> newPaths = new ArrayList<>();
        for(int[] node: path){
            int nodeInd = (int) node[1]; //trip index in demand
            if(nodeInd >= 0){
                String[] str = new String[12];
                str[0] = String.valueOf(node[0]);//car id
                str[2] = timeToString((int)node[2], start);//start time
                str[3] = timeToString((int)node[3], start);//end time
                str[1] = String.valueOf(demand.ind2id(nodeInd));//passenger id
                str[4] = String.valueOf(demand.getValue(nodeInd)); //value
                double[] gps = demand.getGpsCoordinates(nodeInd);
                str[5] = String.valueOf(gps[0]);//pickup lat
                str[6] = String.valueOf(gps[1]);//pickup lng
                str[7] = String.valueOf(gps[2]);//dropoff lat
                str[8] =  String.valueOf(gps[3]);//dropoff lng
                str[9] = String.valueOf(millisToKm(node[4], speed));//distance_driven_since_charging
                str[10] = String.valueOf(node[5]);//times_charged
                str[11] = String.valueOf(central.getDepoId((int)node[6]));//last_charge_location
                newPaths.add(str);
            }
        }
        return newPaths;
   }
    
    private static double millisToKm(int timeInMillis, double speedMs){
        return ((timeInMillis/1000)*speedMs)/1000;
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
    
    private static String makeFilename(String dir, String name, Date timeStamp){
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
        return dir + name + "_" + timeString+".csv";
    }
}

    /**
     * Writes detailed results to .csv file.
     * 
     * @param cars, list of all cars used in solution
     * @param demand 
     * @param graph
     * @param config
     * @param timeStamp
     * @throws IOException 
     */
//    public static void writeCsv(List<Car> cars, Demand demand, 
//        Graph<SimulationNode,SimulationEdge> graph, ConfigTaxify config, Date timeStamp) throws IOException {
//        String filename = makeFilename(config.dir,"result" , timeStamp);
//        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filename))) {
//            csvWriter.writeNext(new String[]{"car_id",     "trip_id",    "call_time",   "start_time",  "end_time",
//                                             "pickup_lat", "pickup_lon", "dropoff_lat", "dropoff_lon", "charge_left [min]",
//                                              "start_lat", "start_lon",  "end_lat",     "end_lon",     "value",
//                                               "pickup_dist","dropoff_dist" });
//            for(Car car : cars){
//                csvWriter.writeAll(pathToCsv(car.getPathStats(), demand, graph));
//            }
//        }
//    }

//  private static List<String[]> pathToCsv(List<int[]> path, Demand demand, Graph<SimulationNode,SimulationEdge> graph){
////      "car_id",     "trip_id",    "call_time",   "start_time",  "end_time",
////      "pickup_lat", "pickup_lon", "dropoff_lat", "dropoff_lon", "charge_left [min]",
////      "start_lat", "start_lon",  "end_lat",     "end_lon",     "value"
//        List<String[]> newPaths = new ArrayList<>();
//        for(int[] node: path){
//            String[] str = new String[node.length+7];
//            str[0] = String.valueOf(node[0]);
//            // station
//            if(node[1] < 0){
//                SimulationNode simNode = graph.getNode(-node[1]);
//                str[1] = "DEPO "+String.valueOf(-node[1]);
//                
//                str[5] = String.valueOf(simNode.getLatitude());
//                str[6] = String.valueOf(simNode.getLongitude());
//                str[7] = String.valueOf(simNode.getLatitude());
//                str[8] = String.valueOf(simNode.getLongitude());
//           // trip
//            }else{
//                int nodeInd = node[1];
//                str[1] = String.valueOf(demand.ind2id(nodeInd));
//                //System.out.println("Trip "+tripId+" start time "+demand.getStartTime(node[]));
//                str[2] = timeToString(demand.getStartTime(nodeInd));
//
//                double[] gpsCoord = demand.getGpsCoordinates(nodeInd);
//                //System.out.println(Arrays.toString(gpsCoord));
//                str[5] = String.valueOf(gpsCoord[0]);
//                str[6] = String.valueOf(gpsCoord[1]);
//                str[7] = String.valueOf(gpsCoord[2]);
//                str[8] = String.valueOf(gpsCoord[3]);
//                
//                str[10] = String.valueOf(demand.getStartLatitude(nodeInd));
//                str[11] = String.valueOf(demand.getStartLongitude(nodeInd));
//                str[12] = String.valueOf(demand.getTargetLatitude(nodeInd));
//                str[13] = String.valueOf(demand.getTargetLongitude(nodeInd));
//                str[14] = String.valueOf(demand.getValue(nodeInd));
//                str[15] =  String.valueOf(demand.getStartNodes(nodeInd)[1]);
//                str[16] = String.valueOf(demand.getEndNodes(nodeInd)[1]);
//            }
//            str[3] = timeToString(node[3]);
//            str[4] = timeToString(node[4]);
//            str[9] = node[9] == 0? " ": String.valueOf(TimeUnit.MILLISECONDS.toMinutes(node[9]));
//            newPaths.add(str);
//        }
//        return newPaths;
//    } 
