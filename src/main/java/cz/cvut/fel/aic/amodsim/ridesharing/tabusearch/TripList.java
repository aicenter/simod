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
import cz.cvut.fel.aic.geographtools.Node;

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
import com.github.davidmoten.rtree.geometry.Rectangle;

import static  cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.SearchNode.createNode;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;


/**
 *
 * @author olga
 * 
 */
public class TripList{
    private boolean DBG = true;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripList.class);
        
    private final AmodsimConfig config;
    private  NearestElementUtils nearestElementUtils;
    private final TripsUtil tripsUtil;
    private final Graph<SimulationNode,SimulationEdge> graph;
    private final int numOfTrips;
    private final int numOfDepos;
    
   // double[] bbox = {59.3, 59.52, 24.5, 24.955};  
    double[] bbox = {59, 60, 24, 25}; 
    int[] times;
    double[] values;
    Map<Integer, SearchNode> searchNodes;

    
    List<OnDemandVehicleStation> depos;
    Map<Integer, List<Integer>> filteredTrips;
    
    private RTree<SimulationEdge, Geometry> rtree;
    
    private RTree<SimulationEdge, Geometry> buildRtree(){
        RTree<SimulationEdge, Geometry> tree = RTree.star().create();
        for(SimulationEdge edge:graph.getAllEdges()){
            double[] from = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
            double[] to = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
            Rectangle bb = rectangle(Math.min(from[0],to[0]), Math.min(from[1],to[1]),
                                    Math.max(from[0],to[0]), Math.max(from[1],to[1]));
            tree = tree.add(edge, bb);
        }
        return tree;
    }
       
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
        this.config = config;
        this.tripsUtil = tripsUtil;
        this.graph = graph;
        rtree = buildRtree();
        System.out.println("Tree size: "+rtree.size());
        
        numOfDepos = config.stations.regions;
        depos = new ArrayList<>();
        searchNodes = new TreeMap<>();
        List<TimeValueTrip<GPSLocation>> sourceList = loadTrips();
        
        numOfTrips = sourceList.get(sourceList.size()-1).id + 1;
        LOGGER.info("Number of trips loaded {}", numOfTrips);
        times = new int[numOfTrips];
        values = new double[numOfTrips];
      
        //make small sample of the list
        //  sourceList.removeIf(trip -> trip.id % 10000 != 0);
        for(TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(sourceList, "Process GPS trip: ")) {
                gpsTripToSearchNode(trip);
        }
        rtree = null;
       
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
     
    public   Map<Integer, double[][]> getDepoMap(){
        Map<Integer, double[][]> depoMap = new HashMap<>();
        int id = 0;
        for(OnDemandVehicleStation d : depos){
            double[] coord = new double[]{d.getPosition().getLatitude(),d.getPosition().getLongitude()};
            double[] proj = DistUtils.degreeToUtm(coord);
            depoMap.put(id, new double[][]{coord, proj});
        }
        return depoMap;
    }
    
    private List<TimeValueTrip<GPSLocation>> loadTrips() {
        String tripsPath = config.amodsim.tripsPath;
        List<TimeValueTrip<GPSLocation>> gpsTrips = IO.loadTripsFromTxt(new File(tripsPath));
        return gpsTrips;
    }
    
    private void gpsTripToSearchNode(TimeValueTrip<GPSLocation> trip) {
        List<GPSLocation> locations = trip.getLocations();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);
        double lat1 = startLocation.getLatitude();
        double lon1 = startLocation.getLongitude();
        
        //check if it's in the city or at least somewhere near
        if(lat1 < bbox[0] || lat1 > bbox[1] || lon1 < bbox[2] || lon1 > bbox[3]){
            return;
        }
        double dist0 = DistUtils.getEuclideanDist(startLocation, targetLocation);
          
        // the length of the trip is less than pickup radius
        if(dist0 < 50 || dist0 > 26000){
             return;
        }
         Object[] startNode = getNearestNodeRtree(startLocation, trip.id);
         Object[] targetNode = getNearestNodeRtree(targetLocation, trip.id);
         if(startNode == null || targetNode == null){
             return;
         }
        //finally adding the node to the list
        times[trip.id] = (int) trip.getStartTime();
        values[trip.id] = trip.getRideValue();
        searchNodes.put(trip.id, createNode(trip.id, startNode, 
            trip.getStartTime()));
        searchNodes.put(trip.id+numOfTrips, createNode(trip.id,targetNode,
            trip.getStartTime()+1800000));
    }
    
  
    private Object[] getNearestNodeRtree(GPSLocation loc, int tripId){
        double radius = 60;
        double[] point = DistUtils.degreeToUtm(loc.getLatitude(), loc.getLongitude());

        Rectangle bounds = rectangle(point[0] - radius, point[1] - radius, 
                                     point[0] + radius, point[1] + radius);
        
        SimulationEdge closestOfAll = null;
        double minDist = Double.MAX_VALUE;
        double[] bestResult = null;
        
        Iterator<Entry<SimulationEdge, Geometry>> results = rtree.search(bounds).toBlocking().getIterator();
        while(!results.hasNext()){
            radius *= 2;
            bounds = rectangle(point[0] - radius, point[1] - radius,
                               point[0] + radius, point[1] + radius);
            results = rtree.search(bounds).toBlocking().getIterator();
        }
        
        while(results.hasNext()){
            SimulationEdge edge = results.next().value();
            double[] result =  getNearestPointAndDistance(point, edge);
            double dist = result[1];
            if(dist <= 50){
                if(result[0] == 0)
                    return new Object[]{edge.fromNode};
                if(result[0] == 1)
                    return new Object[]{edge.toNode};
                if(result[0] == 2)
                    return new Object[]{edge, new double[]{result[2], result[3]}};
                }
            if(dist<minDist){
                minDist = dist;
                bestResult = result;
                closestOfAll = edge;
            }
        }
        if(minDist > 50){
            //System.out.println("Nearest node at "+minDist+" m from the point");
        }
        if(bestResult[0] == 0)
            return new Object[]{closestOfAll.fromNode};
        if(bestResult[0] == 1)
            return new Object[]{closestOfAll.toNode};
        else
            return new Object[]{closestOfAll, new double[]{bestResult[2], bestResult[3]}};
    }
   
    private double[] getNearestPointAndDistance(double[] locProjected, SimulationEdge edge){
        double[] fromProjected = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
        double[] toProjected = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
        double[] v = {toProjected[0] - fromProjected[0], toProjected[1] - fromProjected[1]};
        double[] w = {locProjected[0] - fromProjected[0], locProjected[1] - fromProjected[1]};

        double c1 = v[0]*w[0]+v[1]*w[1];
        if (c1 <= 0 ){
            double dist = DistUtils.getDistProjected(locProjected, fromProjected);
            return new double[]{0 ,dist};
        }
        double c2 = v[0]*v[0]+v[1]*v[1];
        if ( c2 <= c1 ){
            double dist = DistUtils.getDistProjected(locProjected, toProjected);
             return new double[]{1 ,dist};
        }
        double b = c1 / c2;
        double[] point = {fromProjected[0] + b * v[0], fromProjected[1] + b * v[1]};
        double dist = DistUtils.getDistProjected(locProjected, point);
        return new double[]{1 ,dist, point[0], point[1]};
    }
  
    public Iterator<SimulationNode> getShortesPath(int startId, int targetId){
        LinkedList<SimulationNode> path = tripsUtil.createTrip(startId, targetId).getLocations();
        return  path.iterator();
    }
    
    public int getShortesPathDist(int startId, int targetId){
        int dist = 0;
        Iterator<SimulationNode> nodeIterator = getShortesPath(startId, targetId);
        Node fromNode = nodeIterator.next();
        while (nodeIterator.hasNext()) {
            Node toNode = nodeIterator.next();
            dist += graph.getEdge(fromNode, toNode).length;
            fromNode = toNode;
        }
        return dist;
    }
        
    private int getShortesPathDist(SimulationNode start, SimulationNode target){
        LinkedList<SimulationNode> path = tripsUtil.createTrip(start.id, target.id).getLocations();
        int dist = 0;
        Iterator<SimulationNode> nodeIterator = path.iterator();
        Node fromNode = nodeIterator.next();
        while (nodeIterator.hasNext()) {
            Node toNode = nodeIterator.next();
            SimulationEdge edge = graph.getEdge(fromNode, toNode);
            dist += edge.length;
            fromNode = toNode;
        }
        return dist;
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
