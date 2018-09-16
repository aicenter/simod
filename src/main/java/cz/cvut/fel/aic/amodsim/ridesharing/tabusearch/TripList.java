/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import com.github.davidmoten.rtree.Entry;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.io.TimeValueTrip;
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.QuadTree;
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.QuadTree.CoordHolder;
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.Segment;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;


import com.github.davidmoten.rtree.RTree;
import static com.github.davidmoten.rtree.geometry.Geometries.*;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.Geometry;
import rx.Observable;
/**
 *
 * @author olga
 * 
 */
public class TripList{
    private boolean DBG = true;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripList.class);
        
    private final AmodsimConfig config;
    private final NearestElementUtils nearestElementUtils;
    private final TripsUtil tripsUtil;
    private final Graph<SimulationNode,SimulationEdge> graph;
    private final int numOfTrips;
    private final int numOfDepos;
    
   // double[] bbox = {59.3, 59.52, 24.5, 24.955};  
    double[] bbox = {59, 60, 24, 25}; 
    double step = 0.05;
    int[] times;
    double[] values;

    
    //SearchNode[] searchNodes;
    Map<Integer, SearchNode> searchNodes;
    List<OnDemandVehicleStation> depos;
    
    Map<Integer, List<Integer>> filteredTrips;
    double LAT = 59.41;
    double LON =  24.725;
    
    double targetToNode[] = {0, Double.MAX_VALUE, 0};
    private QuadTree<SimulationEdge> quadTree;
    int qtcounter = 0;
    
    private RTree<SimulationEdge, Line> rtree;
    
    private RTree<SimulationEdge, Line> buildRtree(){
        RTree<SimulationEdge, Line> tree = RTree.star().create();
        for(SimulationEdge edge:graph.getAllEdges()){
            double[] from = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
            double[] to = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
            tree = tree.add(edge, line(from[0],from[1], to[0], to[1]));
        }
        
        //tree = tree.add(item, Geometries.point(10,20));
        return tree;
    }
    
    private QuadTree<SimulationEdge> buildQtree(){
        QuadTree<SimulationEdge>tree = new QuadTree<>();
        graph.getAllEdges().forEach((edge) -> {
//                int fromLat = edge.fromNode.getLatE6();
//                int fromLon = edge.fromNode.getLonE6();
//                int toLat = edge.toNode.getLatE6();
//                int toLon = edge.toNode.getLonE6();
                  double[] fromProj = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
                  double[] toProj = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
  //              quadTree.place((fromLat + toLat)/2, (fromLon + toLon)/2, edge); 
                  tree.place((fromProj[0] + toProj[0])/2, (fromProj[1] + toProj[1])/2, edge); 
            });
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
        this.nearestElementUtils = nearestElementUtils;
        this.tripsUtil = tripsUtil;
        this.graph = graph;
 //       this.edgeMap = new HashMap<>();
        
        //quadTree = buildQtree();
        //System.out.println("Tree size: "+quadTree.size());
        rtree = buildRtree();
        System.out.println("Tree size: "+rtree.size());
        numOfDepos = config.stations.regions;
        
        if(DBG){
            filteredTrips = new HashMap<>();
            for(int i = 0; i < 9; i++ ){
                filteredTrips.put(i, new ArrayList<>());
            }
        }
        
        searchNodes = new TreeMap<>();
        depos = new ArrayList<>();
        List<TimeValueTrip<GPSLocation>> sourceList = loadTrips();
        
        numOfTrips = sourceList.get(sourceList.size()-1).id + 1;
        LOGGER.info("Number of trips loaded {}", numOfTrips);
        times = new int[numOfTrips];
        values = new double[numOfTrips];

        
        //make small sample of the list
      //  sourceList.removeIf(trip -> trip.id % 10000 != 0);
        
        for (TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(sourceList, "Process GPS trip: ")) {
                gpsTripToSearchNode(trip);
        }
       
        LOGGER.info("Number of trips in the list {}", searchNodes.size());
        if(DBG){
            LOGGER.info("Start point is out of city boundaries {}",
                filteredTrips.get(0).size());
            LOGGER.info("Trips shorter than 50m {}", 
                filteredTrips.get(1).size());
            LOGGER.info("Trips longer than 26km {}", 
                filteredTrips.get(2).size());
            LOGGER.info("Start and target at the same edge segment {}",
                filteredTrips.get(3).size());

            LOGGER.info("Haversine longer than shortest path {}",
                filteredTrips.get(4).size());
            LOGGER.info("Location  too far from node {}",
                filteredTrips.get(6).size());
            LOGGER.info("Not found node closer than 50 by bf {}",
                filteredTrips.get(8).size());
        }
        System.out.println("QT counter " + qtcounter);
   }
    
    private void bruteForceSearch(List<TimeValueTrip>  lst){
        System.out.println("Brute force, list size "+ lst.size());
        //for(TimeValueTrip trip: lst){      
        for(int i = 0; i<lst.size();i++){
            TimeValueTrip trip = lst.remove(i);
            List<GPSLocation> locations = trip.getLocations();
            GPSLocation startLocation = locations.get(0);
            GPSLocation targetLocation = locations.get(locations.size() - 1);
            double bestDist = Double.MAX_VALUE;
            for(SimulationEdge edge : graph.getAllEdges()){
                Object[] result = getNearestPointAndDistance(startLocation, edge);
                Double dist = (Double) result[1];
                //System.out.println("distance ="+ dist);
                if(dist <= 50){
                    break;
                }
                result = getNearestPointAndDistance(targetLocation, edge);
                dist = (Double) result[1];
                if(dist <= 50){
                    break;
                }   
                bestDist = bestDist <= dist ? bestDist : dist;
            }
            if(bestDist > 50){
                 filteredTrips.get(8).add(trip.id);
            }
            System.out.println(filteredTrips.get(8).size());
        }
        
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
    
    /**
     * Wrapping TripTransform.loadTripsFromTxt.
     * Reads trip data to list, prints some stats.
     * @param config
     * @param tripTransform
     * @return
     */
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
        double lat2 = startLocation.getLatitude();
        double lon2 = startLocation.getLongitude();
        
        //check if it's in the city or at least somewhere near
        if(lat1 < bbox[0] || lat1 > bbox[1] || lon1 < bbox[2] || lon1 > bbox[3]){
            if (DBG){
                filteredTrips.get(0).add(trip.id);
            }
            return;
        }
        
        // haversine  and spherical gives the same result
        //double dist1 = DistUtils.getEquirectangularDist(startLocation, targetLocation);
        double dist0 = DistUtils.getEuclideanDist(startLocation, targetLocation);
        //double dist2 = DistUtils.getHaversineDist(startLocation, targetLocation);
        //System.out.println("Euclidean projected: "+dist0 + ", haversine "+dist2);
        //double dist3 = DistUtils.getSphericalCosineDist(startLocation, targetLocation);
        
        // the length of the trip is less than pickup radius
        if(dist0 < 50){
            if (DBG){
                filteredTrips.get(1).add(trip.id);
               // LOGGER.warn("trip is shorter than 50m {}", dist2);
             }
            return;
        }
        if(dist0 > 26000){
            if (DBG){
                filteredTrips.get(2).add(trip.id);
              //  LOGGER.warn("trip is longer than 26km {}", dist2);
            }
            return;
        }
         SimulationNode startNode = getNearestNodeRtree(startLocation, trip.id);
         SimulationNode targetNode = getNearestNodeRtree(targetLocation, trip.id);
         if(startNode == null || targetNode == null){
             return;
         }

        //finally adding the node to the list
        times[trip.id] = (int) trip.getStartTime();
        values[trip.id] = trip.getRideValue();
        searchNodes.put(trip.id, new SearchNode(trip.id, startNode.id));
    
    }
    
  
    private SimulationNode getNearestNodeRtree(GPSLocation loc, int tripId){

        double radius = 100;
        double[] point = DistUtils.degreeToUtm(loc.getLatitude(), loc.getLongitude());

        
        SimulationNode closestOfAll = null;
        double minDist = Double.MAX_VALUE;

        // First we need to calculate an enclosing lat long rectangle for this
        // distance then we refine on the exact distance
        Rectangle bounds = rectangle(point[0]-radius, point[1]-radius, point[0] + radius, point[1]+radius);

        Iterator<Entry<SimulationEdge, Line>> results = rtree.search(bounds).toBlocking().getIterator();
        while(results.hasNext()){
            Entry<SimulationEdge, Line> entry = results.next();
            SimulationEdge edge = entry.value();
            double dist =  getNearestPointAndDistance(point, edge);
            if(dist <= radius){
                return edge.fromNode;
            }
            if(dist<minDist){
                minDist = dist;
                closestOfAll = edge.fromNode;
            }
        }
        if(minDist>50){
            //System.out.println("Nearest node at "+minDist+" m from the point");
            filteredTrips.get(6).add(tripId);
        }
        return closestOfAll;
    }


    private SimulationNode getNearestNodeQtree(GPSLocation loc, int tripId){
        ///double deltaLat = 0.0001*1E6;
        //double deltaLon =  0.001*1E6;
        double radius = 50;
        double[] point = DistUtils.degreeToUtm(loc.getLatitude(), loc.getLongitude());
//        int lat = loc.latE6;
//        int lon = loc.lonE6;
        List<QuadTree<SimulationEdge>.CoordHolder> neighbors = new ArrayList<>();
        
        SimulationNode closestOfAll = null;
        double minDist = Double.MAX_VALUE;
        
        while(neighbors.isEmpty()){
                //System.out.println("empty neighborhood "+deltaLat+" "+deltaLon);
                //deltaLat *= 2;
                radius *= 2;
                //deltaLon *= 2;
                qtcounter++;
//                neighbors = quadTree.findAll(lat-deltaLat, lon-deltaLat,
//                                             lat+deltaLat, lon+deltaLat);
                neighbors = quadTree.findAll(point[0] - radius, point[1] - radius, 
                                             point[0] + radius, point[1] + radius);
        }
        for(QuadTree<SimulationEdge>.CoordHolder holder : neighbors){
                //System.out.println("found an item: " + holder.o);
                SimulationEdge edge = holder.o;
                // returns distance and coordinates of the nearest point
                Object[] result = getNearestPointAndDistance(loc, edge);
                Double dist = (Double) result[1];
                if(dist <=50 ){
                    return (SimulationNode) result[0];
                }
                if(dist < minDist){
                    minDist = dist;
                    closestOfAll = (SimulationNode) result[0];
                }
            }//for
        neighbors.clear();
        //System.out.println("Nearest node at "+minDist+" m from the point");
        filteredTrips.get(6).add(tripId);
        return closestOfAll;
    }

    
    private double getNearestPointAndDistance(double[] locProjected, SimulationEdge edge){
        double[] fromProjected = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
        double[] toProjected = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
        double[] v = {toProjected[0] - fromProjected[0], toProjected[1] - fromProjected[1]};
        double[] w = {locProjected[0] - fromProjected[0], locProjected[1] - fromProjected[1]};

        double c1 = v[0]*w[0]+v[1]*w[1];
        if (c1 <= 0 ){
            double dist = DistUtils.getDistProjected(locProjected, fromProjected);
            //return new Object[]{edge.fromNode, dist};
            return dist;
        }
        double c2 = v[0]*v[0]+v[1]*v[1];
        if ( c2 <= c1 ){
            double dist = DistUtils.getDistProjected(locProjected, toProjected);
            //return new Object[]{edge.toNode, dist};   
             return dist;
        }
        double b = c1 / c2;
        double[] Pb = {fromProjected[0] + b * v[0], fromProjected[1] + b * v[1]};
        double dist = DistUtils.getDistProjected(locProjected, Pb);
        //return new Object[]{edge.fromNode, dist, Pb};
        return dist;
       
    }
    
    
        
    private Object[] getNearestPointAndDistance(GPSLocation loc, Segment segment){
        double[] locProjected = DistUtils.degreeToUtm(loc.getLatitude(), loc.getLongitude());
        double[] v = {segment.p1[0] - segment.p0[0], segment.p1[1] - segment.p0[1]};
        double[] w = {locProjected[0] - segment.p0[0], locProjected[1] - segment.p0[1]};
        
        double c1 = v[0]*w[0]+v[1]*w[1];
        if (c1 <= 0 ){
            double dist = DistUtils.getDistProjected(locProjected, segment.p0);
            return new Object[]{segment, dist};

        }
        double c2 = v[0]*v[0]+v[1]*v[1];
        if ( c2 <= c1 ){
            double dist = DistUtils.getDistProjected(locProjected, segment.p1);
            return new Object[]{segment, dist};
        }
        double b = c1 / c2;
        double[] Pb = {segment.p0[0] + b * v[0], segment.p0[1] + b * v[1]};
        double dist = DistUtils.getDistProjected(locProjected, Pb);
        return new Object[]{segment, dist};
           
    }
    
    private Object[] getNearestPointAndDistance(GPSLocation loc, SimulationEdge edge){
        double[] locProjected = DistUtils.degreeToUtm(loc.getLatitude(), loc.getLongitude());
        double[] fromProjected = DistUtils.degreeToUtm(edge.fromNode.getLatitude(), edge.fromNode.getLongitude());
        double[] toProjected = DistUtils.degreeToUtm(edge.toNode.getLatitude(), edge.toNode.getLongitude());
        double[] v = {toProjected[0] - fromProjected[0], toProjected[1] - fromProjected[1]};
        double[] w = {locProjected[0] - fromProjected[0], locProjected[1] - fromProjected[1]};

        double c1 = v[0]*w[0]+v[1]*w[1];
        if (c1 <= 0 ){
            double dist = DistUtils.getDistProjected(locProjected, fromProjected);
            return new Object[]{edge.fromNode, dist};
        }
        double c2 = v[0]*v[0]+v[1]*v[1];
        if ( c2 <= c1 ){
            double dist = DistUtils.getDistProjected(locProjected, toProjected);
            return new Object[]{edge.toNode, dist};
        }
        double b = c1 / c2;
        double[] Pb = {fromProjected[0] + b * v[0], fromProjected[1] + b * v[1]};
        double dist = DistUtils.getDistProjected(locProjected, Pb);
        return new Object[]{edge.fromNode, dist, Pb};
       
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
   
    
    /**
     * Return sum of ride_values for all trips in the list.
     * @return
     */
    public double getTripListValue() {
        double value = 0;
        for (Integer tripId : searchNodes.keySet()) {
            value += values[tripId];
        }
        LOGGER.info("finished {}", value);
        return value;
    }
}
    

//     private SimulationNode findNearestNode(GPSLocation loc){
//        double deltaLat = 0.0005*1E6;
//        double deltaLon =  0.001*1E6;
//        int lat = loc.getLatE6();
//        int lon = loc.getLonE6();
//        List<QuadTree<SimulationEdge>.CoordHolder> neighbors = quadTree.findAll(lat-deltaLat, lon-deltaLon,
//                                                                                lat+deltaLat, lon+deltaLon);
//
//        while(neighbors.isEmpty()){
//            //System.out.println("empty neighborhood "+deltaLat+" "+deltaLon);
//            deltaLat *= 2;
//            deltaLon *= 2;
//            qtcounter++;
//            neighbors = quadTree.findAll(lat-deltaLat, lon-deltaLon,
//                                          lat+deltaLat, lon+deltaLon);
//        }
//        
//        SimulationNode closest = null;
//        double minDist = Double.MAX_VALUE;
//        for(QuadTree<SimulationEdge>.CoordHolder holder : neighbors){
//            //System.out.println("found an item: " + holder.o);
//            SimulationEdge edge = holder.o;
//            SimulationNode node = edge.fromNode;
//                 
//            double dist = DistUtils.getEuclideanDist(loc.getLatitude(), loc.getLongitude(),
//                                                     node.getLatitude(), node.getLongitude());
//            if(dist <=50 ){
//                return node;
//            }
//            if(dist < minDist){
//                minDist = dist;
//                closest = node;
//            }
//            node = edge.toNode;
//            dist = DistUtils.getEuclideanDist(loc.getLatitude(), loc.getLongitude(),
//                                                     node.getLatitude(), node.getLongitude());
//            if(dist <=50 ){
//                return node;
//            }
//            if(dist < minDist){
//                minDist = dist;
//                closest = node;
//            }
//        }
//        System.out.println("Nearest node at "+minDist+" m from the point");
//        return closest;
//    }      
        
//    private SimulationNode findNearestNode(GPSLocation loc, int tripId){
//        int[] ind = findCell(loc);
//        int row = ind[0];
//        int col = ind[1];
//        row = row >= nodeMatrix.length ? row : nodeMatrix.length - 1;
//        col = col >= nodeMatrix[0].length ? col : nodeMatrix[0].length - 1;
//        
//        SimulationNode nearestNode = null;
//        while(nearestNode == null){
//            nearestNode = findInCell(loc, row, col);
//            col++;
//            if(col == nodeMatrix[0].length){
//                col = 0;
//                row++;
//                if(row == nodeMatrix.length){
//                    break;
//                }
//            }
//        }
//        // nearest node is further than pickup radius from the target location 
//        if(nearestNode == null){
//            LOGGER.warn("Nearest node not found {}", tripId);
//            return nearestNode;
//        }
//        double minDist = DistUtils.getHaversineDist(loc.getLatitude(), loc.getLongitude(),
//            nearestNode.getLatitude(), nearestNode.getLongitude());
//        if(minDist > 50){
//            String output = String.format("Target: %d %f %f %f %f %f\n", tripId, 
//                loc.getLatitude(), loc.getLongitude(), nearestNode.getLatitude(),
//                nearestNode.getLongitude(), minDist);
//            System.out.print(output);
//            filteredTrips.get(7).add(tripId);
//        }
//        return nearestNode;
//    }
    
//    private SimulationNode findInCell(GPSLocation loc, int row, int col){
//        
//        Set<Integer> nearestNodes = nodeMatrix[row][col];
//        double minDist = Integer.MAX_VALUE;
//        SimulationNode nearestNode = null;
//        for(int nodeId : nearestNodes){
//            SimulationNode node = graph.getNode(nodeId);
//            double dist = DistUtils.getHaversineDist(loc.getLatitude(), loc.getLongitude(),
//                node.getLatitude(), node.getLongitude());
//            if(dist < minDist){
//                minDist = dist;
//                nearestNode = node;
//            }
//        }
//        return nearestNode;
//    }
    


