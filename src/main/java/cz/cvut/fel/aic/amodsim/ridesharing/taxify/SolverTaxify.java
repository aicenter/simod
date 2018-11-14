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
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



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
    private final TripTransformTaxify tripTransform;
   // private long callCount = 0;
    //private long totalTime = 0;
   // private long iterationTime = 0;
   // private long canServeRequestCallCount = 0;
   // private long vehiclePlanningAllCallCount = 0;
    //private Map<Integer, Double> tripLengths;
    
    @Inject public SolverTaxify(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider,
        TripTransformTaxify tripTransform) {

        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
    };

    @Override public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
        try {
            List<TripTaxify<GPSLocation>>    rawDemand = tripTransform.loadTripsFromCsv(new File(config.amodsim.tripsPath));
            Demand demand = new Demand(travelTimeProvider, config, rawDemand, graph);
            StationCentral central = new StationCentral(tripTransform, config, travelTimeProvider,graph);
            Solution solution = new Solution(demand, travelTimeProvider,  central, config);
            solution.buildPaths();
            Stats.writeCsv(solution.getAllCars(), demand, graph, config.amodsimExperimentDir+"result_1511.csv");
            Stats.writeEvaluationCsv(solution.getAllCars(), demand, config.amodsimExperimentDir+"eval_result_1511.csv");
        } catch (IOException ex) {
            LOGGER.error("FIlE IO error: "+ex);
        } 
        return new  HashMap<>();
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
