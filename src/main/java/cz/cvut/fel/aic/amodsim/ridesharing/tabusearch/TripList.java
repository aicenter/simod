/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.io.TimeValueTrip;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Circle;
import static com.github.davidmoten.rtree.geometry.Geometries.*;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.EdgeShape;
import java.io.FileNotFoundException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 *
 * @author olga
 * 
 */
public class TripList{
    private boolean DBG = true;
    private final static int SRID = 32633;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripList.class);
        
    private final AmodsimConfig config;
    private  NearestElementUtils nearestElementUtils;
    final TripsUtil tripsUtil;
    final Graph<SimulationNode,SimulationEdge> graph;
    private final int numOfTrips;
    private final int numOfDepos;
    
   // double[] bbox = {59.3, 59.52, 24.5, 24.955};  
    double[] bbox = {59, 60, 24, 25}; 
    int[] times;
    double[] values;
    Map<Integer, SearchNode> searchNodes;
    private RTree<SimulationEdge, Geometry> rtree;
    
    Set<Integer> foundByRtree;
    Set<Integer> foundByBFOnly;
    Set<Integer> notFoundByBF;
    
    int[] counters = new int[]{0,0,0,0};
        
    List<OnDemandVehicleStation> depos;
    Map<Integer, List<Integer>> filteredTrips;
    
    int dummyNodeId;
 
    List<SimulationNode> dummyNodes;
    List<SimulationEdge> dummyEdges;
    /**
     * Converts list of TimeValueTrip to TSTrip (class used for search).
     * 
     * @param config
     * @param nearestElementUtils
     * @param tripsUtil
     * @param graph
     */
    
    public TripList(AmodsimConfig config, NearestElementUtils nearestElementUtils, TripsUtil tripsUtil,
        Graph<SimulationNode, SimulationEdge> graph){
      //  System.out.println("TripList");
        this.config = config;
        this.tripsUtil = tripsUtil;
        this.nearestElementUtils = nearestElementUtils;
        this.graph = graph;
        rtree = buildRtree();

        
        numOfDepos = config.stations.regions;
        depos = new ArrayList<>();
        searchNodes = new TreeMap<>();
        List<TimeValueTrip<GPSLocation>> sourceList = loadTrips();
        
        numOfTrips = sourceList.get(sourceList.size()-1).id + 1;
        LOGGER.info("Number of trips loaded {}", numOfTrips);
        times = new int[numOfTrips];
        values = new double[numOfTrips];
        
        dummyNodeId = graph.numberOfNodes();
//        dummyEdgeId = -1;
        
        foundByRtree = new HashSet<>();
        foundByBFOnly = new HashSet<>();
        notFoundByBF = new HashSet<>();
        dummyEdges = new ArrayList<>();
        dummyNodes = new ArrayList<>();
        //make small sample of the list
        //  sourceList.removeIf(trip -> trip.i % 10000 != 0);
        for(TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(sourceList, "Process GPS trip: ")) {
                gpsTripToSearchNode(trip);
        }
        //destroy rtree, we don't need it any longer;
        rtree = null;

        // graph with dummy nodes, representing points on the  edge of the road graph,
        // which are origin or target points for trips
        // with dummy edges connecting them to the nodes of the graph incident to that edge;
                
      //  System.out.println("Start location out of borders "+counters[0]);
      //  System.out.println("Too short or too long trip "+counters[1]);
    //    System.out.println("Same graph element for start and target "+counters[2]);
        //System.out.println("minDistans  < 50 outside while cycle "+counters[3]);
        System.out.println("Trips with edge of point found by rtree "+foundByRtree.size());
        System.out.println("Trips with edge found by BF only "+foundByBFOnly.size());
        System.out.println("Trips with edge not found at all "+notFoundByBF.size());
        
        System.out.println("Points assigned by rtree "+counters[0]);
        System.out.println("Points assigned by BF "+counters[1]);
        System.out.println("Ponts not assigned "+counters[2]);
        System.out.println("Number of additional nodes "+dummyNodes.size());
        System.out.println("Number of additional edges "+dummyEdges.size());
        System.out.println("Total graph: " + (graph.numberOfNodes() + dummyNodes.size()) 
                            + " nodes and " + (graph.numberOfEdges() + dummyEdges.size()) + " edges");
    //    System.out.println(Arrays.toString(radiusCounter));
//        try(PrintWriter pw = new PrintWriter(
//            new FileOutputStream(new File(config.amodsimDataDir + "/notFoundByRtree_eesti.txt")))){
//                foundByBFOnly.forEach((t) -> {
//                    pw.println(t);
//                });
//                      
//        }catch (IOException ex){
//            LOGGER.error(null, ex);          
//        }
//        try(PrintWriter pw = new PrintWriter(
//            new FileOutputStream(new File(config.amodsimDataDir + "/notFoundByBF_eesti.txt")))){
//                notFoundByBF.forEach((t) -> {
//                    pw.println(t);
//                });
//        }catch (IOException ex){
//            LOGGER.error(null, ex);          
//        }
    }

    
    private void gpsTripToSearchNode(TimeValueTrip<GPSLocation> trip) {
        List<GPSLocation> locations = trip.getLocations();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);
        double lat1 = startLocation.getLatitude();
        double lon1 = startLocation.getLongitude();
        
        //check if it's in the city or at least somewhere near
        if(lat1 < bbox[0] || lat1 > bbox[1] || lon1 < bbox[2] || lon1 > bbox[3]){
            //counters[0]++;
            return;
        }
        double[] fromProj = new double[]{startLocation.getLongitudeProjected(),
                                         startLocation.getLatitudeProjected()};
        double[] toProj = new double[]{targetLocation.getLongitudeProjected(),
                                       targetLocation.getLatitudeProjected()};  
        double dist0 = DistUtils.getDistProjected(fromProj, toProj);
        //System.out.println(Arrays.toString(fromProj));
        //System.out.println(Arrays.toString(toProj));
        // the length of the trip is less than pickup radius
        if(dist0 < 50 || dist0 > 26000){
            //counters[1]++;
            return;
        }
        SimulationNode startNode = getNearestNodeRtree(fromProj, trip.id);
        SimulationNode targetNode = getNearestNodeRtree(toProj, trip.id);
        if(startNode == targetNode){
             //counters[2]++;
             return;
         }
         if(startNode == null || targetNode == null){
            return;
         }
        //finally adding the vi to the list
        times[trip.id] = (int) trip.getStartTime();
        values[trip.id] = trip.getRideValue();
        searchNodes.put(trip.id, new SearchNode(trip.id, startNode));
        searchNodes.put(trip.id+numOfTrips, new SearchNode(trip.id+numOfTrips, targetNode));
    }
    
  
    private SimulationNode getNearestNodeRtree(double[] point, int tripId){
        double radius = 50;
        Rectangle bounds = rectangle(point[0] - radius, point[1] - radius, 
                                     point[0] + radius, point[1] + radius);
        
        Iterator<Entry<SimulationEdge, Geometry>> results = rtree.search(bounds).toBlocking().getIterator();
        while(!results.hasNext()){
            radius *= 2;
            bounds = rectangle(point[0] - radius, point[1] - radius,
                               point[0] + radius, point[1] + radius);
            results = rtree.search(bounds).toBlocking().getIterator();
        }
       
        while(results.hasNext()){
            SimulationEdge edge = results.next().value();
            SimulationNode node =  getNearestNode(point, edge);
            if(node != null){
                counters[0]++;
                foundByRtree.add(tripId);
                return node;
            }
        }
//        SimulationNode node = bruteForceSearch(point);
//        if(node != null){
//            counters[1]++;
//            foundByBFOnly.add(tripId);
//            return node;
//        }else{
//            counters[2]++;
//            notFoundByBF.add(tripId);
//            return null;
//        }
    return null;
    }
  
    
    private SimulationNode bruteForceSearch(double[] point){
        for(SimulationEdge edge : graph.getAllEdges()){
            SimulationNode node = getNearestNode(point, edge);
            if(node != null){
                return node;
            }
        }
        return null;
    }
   
    private SimulationNode getNearestNode(double[] locProjected, SimulationEdge edge){
        double[] fromProjected = new double[]{edge.fromNode.getLongitudeProjected(),
                                              edge.fromNode.getLatitudeProjected()};

        double[] toProjected = new double[]{edge.toNode.getLongitudeProjected(),
                                            edge.toNode.getLatitudeProjected()};
        
        double[] v = {toProjected[0] - fromProjected[0], toProjected[1] - fromProjected[1]};
        double[] w = {locProjected[0] - fromProjected[0], locProjected[1] - fromProjected[1]};
        
        double c1 = v[0]*w[0]+v[1]*w[1];
        //fromNode is the nearest point
        if (c1 <= 0 ){
            double dist = DistUtils.getDistProjected(locProjected, fromProjected);
            if(dist <= 50){
                return edge.fromNode;
            }else{
                return null;
            }
        }
        double c2 = v[0]*v[0]+v[1]*v[1];
        // toNode is the nearest point
        if ( c2 <= c1 ){
            double dist = DistUtils.getDistProjected(locProjected, toProjected);
            if(dist <= 50){
                return  edge.toNode;
            }else{
                return null;
            }
        }
        //nearest point is somewhere on the edge
        double b = c1 / c2;
        double[] point = {fromProjected[0] + b * v[0], fromProjected[1] + b * v[1]};

        double dist = DistUtils.getDistProjected(locProjected, point);
        if(dist <= 50){
            
            // lengths of two segments, from the nodes to the point on the edge
            int fromLength = (int) Math.round(DistUtils.getDistProjected(fromProjected, point));
            int toLength = (int) Math.round(DistUtils.getDistProjected(toProjected, point));
            
            //dummy node and edges from that node to real nodes
            GPSLocation newLocation = GPSLocationTools.createGPSLocationFromProjected(
                (int) Math.round(point[1] * 1E2),(int) Math.round(point[0] * 1E2), 0, SRID);
            SimulationNode dummyNode = new SimulationNode(dummyNodeId++, 0,  newLocation); 
            SimulationEdge dummyFromEdge = new SimulationEdge(dummyNode, edge.fromNode, 0, 0, 0, fromLength, 50, 1, 
                                                              new EdgeShape(Arrays.asList(dummyNode, edge.fromNode)));
            SimulationEdge dummyToEdge = new SimulationEdge(dummyNode, edge.toNode, 0, 0, 0, toLength, 50, 1, 
                                                              new EdgeShape(Arrays.asList(dummyNode, edge.toNode)));
            dummyNodes.add(dummyNode);
            dummyEdges.add(dummyFromEdge);
            dummyEdges.add(dummyToEdge);

            return  dummyNode;
            }else{
                return null;
         }
    }

    private RTree<SimulationEdge, Geometry> buildRtree(){
        long start = System.currentTimeMillis();
        double[] box = {Double.MAX_VALUE, 0, Double.MAX_VALUE, 0};
        
        RTree<SimulationEdge, Geometry> tree = RTree.star().create();
        for(SimulationEdge edge:graph.getAllEdges()){
            double fromY = edge.fromNode.getLatitudeProjected();
            double fromX = edge.fromNode.getLongitudeProjected();
            double toY = edge.toNode.getLatitudeProjected();
            double toX = edge.toNode.getLongitudeProjected();
            double minY = Math.min(fromY, toY);
            double maxY = Math.max(fromY, toY);
            double minX = Math.min(fromX, toX);
            double maxX = Math.max(fromX, toX);
            Rectangle bb = rectangle(minX, minY, maxX, maxY);
            tree = tree.add(edge, bb);
        }
        long end = System.currentTimeMillis();
        
        System.out.println("Tree size: "+tree.size());
        System.out.println("Rtree depth: "+ tree.calculateDepth());
        System.out.println("Time: "+ (end - start) + " ms");
//        System.out.println("Box: "+Arrays.toString(box));
//        double xdist = (box[1] - box[0]);
//        double ydist = box[3] - box[2];
//        System.out.println("Distance x = "+xdist+"m, y = "+ydist+"m");
        return tree;
    }
    
    public RTree<Integer, Geometry> buildRtree(char type){
        RTree<Integer, Geometry> tree = RTree.star().create();
        if(type == 'n'){
            for(Integer tripId: searchNodes.keySet()){
                SearchNode searchNode = searchNodes.get(tripId);
                int simNodeId = searchNode.vi;
                SimulationNode simNode = graph.getNode(simNodeId);
                Point point = point(simNode.getLongitudeProjected(),simNode.getLatitudeProjected());
                tree = tree.add(tripId, point);
            }
        } else if (type == 'd'){
            int depoId = 0;
            for(OnDemandVehicleStation depo: depos){
                SimulationNode node = depo.getPosition();
                Point point = point(node.getLongitudeProjected(), node.getLatitudeProjected());
                tree = tree.add(depoId, point);
                depoId++;
            }
        }
        return tree;
    }
    
    public Graph<SimulationNode, SimulationEdge> buildTripGraph(){
        GraphBuilder<SimulationNode, SimulationEdge> graphBuilder = new GraphBuilder<>();
        graphBuilder.addNodes(graph.getAllNodes());
        graphBuilder.addNodes(dummyNodes);
        graphBuilder.addEdges(graph.getAllEdges());
        graphBuilder.addEdges(dummyEdges);
        
        String fn = config.amodsimDataDir + "/new_edges.txt";
        try(PrintWriter pw = new PrintWriter(
            new FileOutputStream(new File(fn)))){
                dummyEdges.forEach((e) -> {
                    pw.println(e.fromNode.getLatitude() +" "+ e.fromNode.getLongitude()
                                +" "+e.toNode.getLatitude()+" "+e.toNode.getLongitude());
                });
        }catch (IOException ex){
            LOGGER.error(null, ex);          
        }
        return graphBuilder.createGraph();
    }
    
    public int getNumOfTrips() {
        return numOfTrips;
    }

    public int getNumOfDepos() {
        return numOfDepos;
    }
    
    protected void addDepoNodesToList(List<OnDemandVehicleStation> depos){
        if(numOfDepos != depos.size()){
            LOGGER.error("Depo list length differs from numOfDepos");
            return;
        }
       this.depos = depos;
        LOGGER.info("{} depos added", this.depos.size());
    }
    
    private List<TimeValueTrip<GPSLocation>> loadTrips() {
        String tripsPath = config.amodsim.tripsPath;
        List<TimeValueTrip<GPSLocation>> gpsTrips = IO.loadTripsFromTxt(new File(tripsPath));
        return gpsTrips;
    }
       
    /**
     * Return sum of ride_values for all trips in the list.
     * @return
     */
    public double getTripListValue() {
        double value = 0;
        value = searchNodes.keySet().stream()
            .filter(x -> x < numOfTrips)
            .map((tripId) -> values[tripId])
            .reduce(value, (accumulator, _item) -> accumulator + _item);
        return value;
    }
    
    
    /**
     * Calculates how many orders arrive during given period of time.

     * @param period  seconds
     * @return
     */
    public double avgTimeBtwTrips(int period) {
        double sumFrequency = 0;
        long maxFrequency = 0;
        long minFrequency = Integer.MAX_VALUE;
        long periodMS = period * 1000;
        long prevTime = 0;
        int frequency = 0;
        int numOfPeriods = 1;
        for(Integer tripId : searchNodes.keySet()){
            long delta = times[tripId] - prevTime;
            if (delta < periodMS) {
                frequency++;
            } else {
                prevTime += periodMS;
                maxFrequency = maxFrequency >= frequency ? maxFrequency : frequency;
                minFrequency = minFrequency <= frequency ? minFrequency : frequency;
                sumFrequency += frequency;
                numOfPeriods++;
                frequency = 0;
            }
        }
        StringBuilder sb = new StringBuilder("Times between trips: \n");
        sb.append("Max density: ").append(maxFrequency).append(" orders per ").append(period).append(" sec \n");
        sb.append("Min density: ").append(minFrequency).append(" orders per ").append(period).append(" sec \n");
        sb.append("Average: ").append(sumFrequency / numOfPeriods).append(", trips per minute (")
            .append(60 * sumFrequency / numOfPeriods).append("\n");
        System.out.println(sb.toString());
        
        return sumFrequency / numOfPeriods;
    }
    
    /**
     * Returns index of the trip beginning at start time.
     * If trip with exactly this time is not present in the list,
     * returns -insertion point -1, there insertion point is the
     * index in the list for element with the given value.
     * 
     * @param startTime
     * @return 
     */
    public int getTripByTime(long startTime){
        LOGGER.info("Start");
        return Arrays.binarySearch(times, (int) startTime);
    }
   
    /**
     * Return sublist of trips starting not earlier than startTime and no later than startTime+timeWindow
     * @param startTime milliseconds
     * @param timeWindow  seconds
     * @return 
     */
    public List<SearchNode> getTripsInsideTW(int startTime, int timeWindow){
        LOGGER.info("{}", startTime);
        List<SearchNode> result = new ArrayList<>();
        if(timeWindow == 0){
            LOGGER.warn("Interval has length 0");
            return result;
        }
        int ind = getTripByTime(startTime);
        ind = ind >= 0 ? ind : -ind;
        if(ind >= numOfTrips){
            LOGGER.info("Start time is out of range");
            return result;
        }
        int endTime = startTime + timeWindow*1000;
        while(startTime <= endTime){
            SearchNode trip = searchNodes.get(ind);
            result.add(trip);
            startTime = times[++ind];
            if(ind == numOfTrips){
                break;
            }
        }
        LOGGER.info("Done. Found {} trips", result.size());
        return result;
    }
   
 
}
