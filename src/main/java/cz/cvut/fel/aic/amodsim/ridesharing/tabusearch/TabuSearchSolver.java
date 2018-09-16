/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.TabuSearchUtils;
import com.google.inject.Inject;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
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
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.QuadTree;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final NearestElementUtils nearestElementUtils;
    private final Graph<SimulationNode,SimulationEdge> graph;
    private  List<OnDemandVehicleStation> stations;
    
    final double maxDistance;
    final double maxDistanceSquared;
   
    //TODO move to config
    private final long  maxRideTime = 1800000; // 30 min in ms??
    private final long maxCharge = 14400000;
    private final long fullChargeTime = 7200000;
    
    
    
//    private double maxValue;
//    private Map<Node, Set<Node>> map3min;
//    private Set<Node>[][] nodeMatrix;
    
    //double[] bbox = {59.3, 59.52, 24.5, 24.955};
    double[] bbox = {59, 60, 24, 25}; 

    double step = 0.005;
    final private String pathFile;
    
    private TripList tripList;
       
    
    @Inject
    public TabuSearchSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider, 
         TravelCostProvider travelCostProvider, AmodsimConfig config, TripTransform tripTransform,
         TripsUtil tripsUtil, NearestElementUtils nearestElementUtils){
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        this.travelTimeProvider = travelTimeProvider;
        this.travelCostProvider = travelCostProvider;
        this.vehicleStorage = vehicleStorage;
        this.tripsUtil = tripsUtil;
        this.nearestElementUtils = nearestElementUtils;
        
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

   @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
//        SpaceMatrix sm = new SpaceMatrix(bbox, step);
//        sm.add(graph.getAllNodes());
       
        //tripList = TabuSearchUtils.addPaths( tripsUtil, graph);
        //TabuSearchUtils.avgTimeBtwTrips(tripList, 60);
//        List<SimulationNode> nodes = stations.stream()
//            .map(st->st.getPosition()).collect(Collectors.toList());
//        TabuSearchUtils.pathsNoLongerThan(nodes, graph, 
//            180, tripsUtil, pathFile);
//      
        tripList = new TripList(config, nearestElementUtils, tripsUtil, graph);
        tripList.addDepoNodesToList(stations);
        System.out.println("Size of searchNodes map "+tripList.searchNodes.size());
        StringBuilder sb = new StringBuilder();
        
        
        
        
        
//        
//        TabuSearchUtils.edgeStats(graph);
//        TabuSearchUtils.nodeStats(graph);
        
//        int counter = 0;
//        List<SimulationEdge> edges = new ArrayList<>();
//        for(SimulationEdge edge: graph.getAllEdges()){
////            System.out.println(String.format("{%f, %f, %f, %f, %d}", edge.fromNode.getLatitude(),edge.fromNode.getLongitude(),
////                                                             edge.toNode.getLatitude(), edge.toNode.getLongitude(),
////                                                             edge.length));
//            edges.add(edge);
//            if(counter++ > 100)             break;
//        }
//        System.out.println("Number of edges in the list "+graph.getAllEdges().size());
//        long start = System.currentTimeMillis();
//        QuadTree<SimulationEdge> tree = new QuadTree<>();
//        for(SimulationEdge edge: graph.getAllEdges()) {
//            tree.place((edge.fromNode.getLatE6()+edge.toNode.getLatE6())/2,
//                        (edge.fromNode.getLonE6()+edge.toNode.getLonE6())/2, edge);
//        }
//        long end =  System.currentTimeMillis();
//        System.out.println("Tree size = "+ tree.size());
//        System.out.println("time " + (end-start)/1000 +" seconds ");
     //  tree.print(System.out);
     
     
 
//        double maxValue = tripList.getTripListValue();
//        
//        sb.append("Total value: ").append(maxValue).append("\n");
//        System.out.println(sb.toString());
//        
//        double tripsPerHour = tripList.avgTimeBtwTrips(3600);
//        
//        List<SearchNode> min10_11 = tripList.getTripsInsideTW(600000, 15);
//        for(SearchNode trip : min10_11){
//            sb.append(trip.id).append("  - ")
//                .append(tripList.times[trip.id]).append(" \n");
//        }
//        System.out.println(sb.toString());
        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();
        return planMap;
    }
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        return solve();
    }
}



//    StringBuilder sb = new StringBuilder("Binary search test: \n");
//        
//        List<TSTrip> tripList2 = TripList.buildList(tripList);
//        int count = 0;
//        for(int i = 0; i < tripList2.size(); i+=1013){
//            if(i % 1017 == 0){
//                sb.append("At index ").append(i);
//                TSTrip trip = tripList2.get(i);
//                long time = trip.time;
//                sb.append(" is trip ").append(trip.id).append(" starting at ").append(time).append("\n");
//                int ind = TripList.getTripByTime(tripList2, time);
//                TSTrip trip2 = tripList2.get(ind);
//                sb.append("Search returns index ").append(ind).append(". Trips are same: ")
//                    .append(trip==trip2).append("\n");
//                sb.append("Time ").append(trip2.start).append(" is same: ").append(time == trip2.time).append("\n");
//                if(count++ == 10){
//                    break;
//                }
//            }
//        }
//        long time = 1234567;
//        sb.append("Time window from ").append(time).append(" to ").append(time+10*1000).append("\n");
//        List<TSTrip> result = TripList.getTripsInsideTW(tripList2, time, 10*1000);
//        
//        for(TSTrip trip: result){
//            sb.append(trip.time).append("\n");               
//        }
//
//        
//        System.out.println(sb.toString());



//    
//    private int getTripByTime(long startTime){
//        
//        int ind = Arrays.binarySearch(tripList.stream().map(trip->trip.getStartTime())
//            .mapToLong(Long::longValue).toArray(), startTime);
//        //return tripList.get(ind);
//        System.out.print("Index returned by bs = ");
//        System.out.println(ind);
//        return ind;
//    }
   
//    /**
//     * 
//     * @param startTime milliseconds
//     * @param timeWindow  milliseconds
//     * @return 
//     */
//    private List<TimeValueTrip> getTripsInsideTW(long startTime, long timeWindow){
//        
//        int ind = Arrays.binarySearch(tripList.stream().map(trip->trip.getStartTime())
//            .mapToLong(Long::longValue).toArray(), startTime);
//        
//        
//    }

    
