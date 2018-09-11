/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.TabuSearchUtils;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.io.TimeValueTrip;
import java.util.List;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class TabuSearchSolver extends DARPSolver{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TabuSearchSolver.class);
    
    private final AmodsimConfig config;
    private final TripTransform tripTransform;
    private final TripsUtil tripsUtil;
    private final TravelTimeProvider travelTimeProvider;
    private final TravelCostProvider travelCostProvider;
    private final OnDemandVehicleStorage vehicleStorage;
    
    private final Graph<SimulationNode,SimulationEdge> graph;
    private  List<OnDemandVehicleStation> stations;
    private  List<TimeValueTrip<SimulationNode>> tripList;
    
    final double maxDistance;
    final double maxDistanceSquared;
   
    //TODO move to config
    private final long  maxRideTime = 1800000; // 30 min in ms??
    private final long maxCharge = 14400000;
    private final long fullChargeTime = 7200000;
    
    
//    private double maxValue;
//    private Map<Node, Set<Node>> map3min;
//    private Set<Node>[][] nodeMatrix;
    
    double[] bbox = {59.3, 59.52, 24.5, 24.955};
    double step = 0.005;
    final private String pathFile;
       
    
    @Inject
    public TabuSearchSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider, 
         TravelCostProvider travelCostProvider, AmodsimConfig config, TripTransform tripTransform,
         TripsUtil tripsUtil){
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        this.travelTimeProvider = travelTimeProvider;
        this.travelCostProvider = travelCostProvider;
        this.vehicleStorage = vehicleStorage;
        this.tripsUtil = tripsUtil;
        
        pathFile = config.amodsimDataDir+ "/trip_paths.txt";
        maxDistance = (double) config.amodsim.ridesharing.maxWaitTime 
				* config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
        maxDistanceSquared = maxDistance * maxDistance;
        graph = tripTransform.getHighwayGraph();
       
    }
    
    public void setStations(List<OnDemandVehicleStation> stations){
        StringBuilder sb = new StringBuilder();
        this.stations = stations;
        sb.append("Depos loaded: ").append(this.stations.size()).append("\n");
        System.out.println(sb.toString());
    }
    
    private void buildInitialSolution(){
        
        
    }
    
    private int getTripByTime(long startTime){
        
        int ind = Arrays.binarySearch(tripList.stream().map(trip->trip.getStartTime())
            .mapToLong(Long::longValue).toArray(), startTime);
        //return tripList.get(ind);
        System.out.print("Index returned by bs = ");
        System.out.println(ind);
        return ind;
    }
   
    /**
     * 
     * @param startTime milliseconds
     * @param timeWindow  milliseconds
     * @return 
     */
//    private List<TimeValueTrip> getTripsInsideTW(long startTime, long timeWindow){
//        
//        int ind = Arrays.binarySearch(tripList.stream().map(trip->trip.getStartTime())
//            .mapToLong(Long::longValue).toArray(), startTime);
//    }
//   
    
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
        //buildMatrix();
        tripList = TabuSearchUtils.loadTrips(config, tripTransform);
        tripList = TabuSearchUtils.addPaths(tripList, pathFile, tripsUtil, graph);
        //TabuSearchUtils.avgTimeBtwTrips(tripList, 60);
        StringBuilder sb = new StringBuilder("Binary search test: \n");
        
        long[] times = {123000, 34587200};//, 705123200 ,100860000,172706500
       
        for(long t: times){
            sb.append("searching for trip starting at ").append(t);
            int ind = getTripByTime(t);
            sb.append(" , index ").append(ind);

                TimeValueTrip trip = tripList.get(-ind);
                sb.append(" time of trip at -index ")
                .append(trip.getStartTime()).append("\n");
            }
        
        
        System.out.println(sb.toString());

        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();

        return planMap;
    }
    
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        //double maxValue = TabuSearchUtils.getTripListValue(tripList);
        //Set[][] matrix = TabuSearchUtils.buildMatrix(bbox, step, graph);
        return solve();
    }
  
}


//    private class Car{
//        final String vehicleId;
//        double charge;
//        List<Map<Long,SimulationNode>> plan;
//        double valueEarned;
//        private boolean active;
//
//        public Car(String vehicleId) {
//            this.vehicleId = vehicleId;
//            plan = new ArrayList<>();
//            charge = maxCharge;
//            valueEarned = 0;
//            active = true;
//            
//        }
//    
//    }
// 


    
