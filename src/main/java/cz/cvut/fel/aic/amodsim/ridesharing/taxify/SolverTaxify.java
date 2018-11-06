package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

/**
 * @author F.I.D.O.
 */
public class SolverTaxify extends DARPSolver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolverTaxify.class);
    private final AmodsimConfig config;
//    private final double maxDistance;
    Graph<SimulationNode,SimulationEdge> graph;
//    private final double maxDistanceSquared;
//    private final int maxDelayTime;
    private final TripTransform tripTransform;
   // private long callCount = 0;
    //private long totalTime = 0;
   // private long iterationTime = 0;
   // private long canServeRequestCallCount = 0;
   // private long vehiclePlanningAllCallCount = 0;
    //private Map<Integer, Double> tripLengths;
    
    @Inject public SolverTaxify(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider,
        TripTransform tripTransform) {

        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
       
//        maxDistance = (double ) config.amodsim.ridesharing.maxWaitTime 
//            * config.amodsim.ridesharing.maxSpeedEstimation / 3.6; 
//        maxDistanceSquared = maxDistance * maxDistance;
//        maxDelayTime = config.amodsim.ridesharing.maxWaitTime * 1000;
        //tripLengths = new HashMap<>();
        LOGGER.warn("");
    }

    @Override public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
//        for(SimulationNode node: graph.getAllNodes()){
//            System.out.println(node.id);
//        }
        List<TimeTripWithValue<GPSLocation>> rawDemand = tripTransform.loadTripsFromTxt(new File(config.amodsim.tripsPath));
        Demand demand = new Demand(travelTimeProvider, config, 
           rawDemand.size(), rawDemand.get(rawDemand.size()-1).id+1);
        demand.prepareDemand(rawDemand);
       //LOGGER.info("Number of  paths " + paths.length);
        StationCentral central = new StationCentral(tripTransform, config, travelTimeProvider,graph);
        //int[][] paths = demand.buildPaths(5, central);
        Solution solution = new Solution(demand, travelTimeProvider, central, config);
        solution.buildPaths();
        try {
            writeCsv(solution.getAllCars(), demand);
        } catch (IOException ex) {
            LOGGER.error("FIlE IO error: "+ex);
        }
        return new  HashMap<>();
    }
   
    private void writeCsv(List<Car> cars, Demand demand) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(config.amodsimExperimentDir +"result.csv"))) {
            csvWriter.writeNext(new String[]{"car_id", "trip_id", "start_time","end_time",
                "start_lat", "start_lon", "end_lat", "end_lon", "charge_left [min]"});
            for(Car car : cars){
                csvWriter.writeAll(pathToCsv(car.getPathStats(), demand));
            }
        }
    }

    private List<String[]> pathToCsv(List<int[]> path, Demand demand){
        //"car_id", "trip_id", "start_time","end_time", 
        //"start_lat", "start_lon", "end_lat", "end_lon"
        List<String[]> newPaths = new ArrayList<>();
        for(int[] node: path){
            String[] str = new String[node.length];
            str[0] = String.valueOf(node[0]);
            // station
            if(node[1] < 0){
                SimulationNode simNode = graph.getNode(-node[1]);
                str[1] = "DEPO "+String.valueOf(-node[1]);
                str[4] = String.valueOf(simNode.getLatitude());
                str[5] = String.valueOf(simNode.getLongitude());
                str[6] = String.valueOf(simNode.getLatitude());
                str[7] = String.valueOf(simNode.getLongitude());
           // trip
            }else{
                int tripId = demand.ind2id(node[1]);
                str[1] = String.valueOf(tripId);
                int[] startNodes = demand.getStartNodes(node[1]);
                int[] endNodes = demand.getEndNodes(node[1]);
                SimulationNode start = graph.getNode(startNodes[0]);
                SimulationNode target = graph.getNode(endNodes[0]);
                str[4] = String.valueOf(start.getLatitude());
                str[5] = String.valueOf(start.getLongitude());
                str[6] = String.valueOf(target.getLatitude());
                str[7] = String.valueOf(target.getLongitude());
            }
            str[2] = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(node[2]),
                                            TimeUnit.MILLISECONDS.toMinutes(node[2]) % TimeUnit.HOURS.toMinutes(1),
                                            TimeUnit.MILLISECONDS.toSeconds(node[2]) % TimeUnit.MINUTES.toSeconds(1));
            str[3] = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(node[3]),
                                            TimeUnit.MILLISECONDS.toMinutes(node[3]) % TimeUnit.HOURS.toMinutes(1),
                                            TimeUnit.MILLISECONDS.toSeconds(node[3]) % TimeUnit.MINUTES.toSeconds(1));
            str[8] = node[8] == 0? " ": String.valueOf(node[8]/60000);
            newPaths.add(str);
        }
        return newPaths;
    }  
    

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

 }





//  System.out.println("Solver:" + paths.length);
//
//        try {
//            OutputStreamWriter writer = new OutputStreamWriter(
//                                             new FileOutputStream(new File(config.amodsimExperimentDir+"/paths")));
//        
//        for(int[] path :paths){
//            if(path.length == 0){
//                System.out.println("Empty path ");
//                continue;
//            }
//            System.out.println(path.length +": "+Arrays.toString(path));
//            SimulationNode node = graph.getNode(-path[0]);
//            if(node == null){
//                System.out.println(" invalid node id");
//            }
//            System.out.println(node.id);
//            StringBuilder sb = new StringBuilder(node.getLatitude() +" "+node.getLongitude()+", ");
//            for(int i=1;i<path.length-1;i++){
//                int tripInd = path[i];
//                TimeTripWithValue trip = rawDemand.get(tripInd);
//                if(trip.id != demand.ind2id(tripInd)){
//                    System.out.println("Index matching error, id in raw demand "+trip.id+", id in demand"+demand.ind2id(tripInd));
//                }
//                GPSLocation start = (GPSLocation) trip.getLocations().get(0);
//                GPSLocation target = (GPSLocation) trip.getLocations().get(trip.getLocations().size()-1);
//                sb.append(start.getLatitude()).append(" ").append(start.getLongitude()).append(", ");
//                sb.append(target.getLatitude()).append(" ").append(target.getLongitude()).append(", ");
//            }
//            node = graph.getNode(-path[path.length-1]);
//            sb.append(node.getLatitude()).append(" ").append(node.getLongitude()).append("\n");
//            writer.write(sb.toString());
//           }
//            } catch (IOException ex) {
//            LOGGER.error("File error "+ex);
//        }
