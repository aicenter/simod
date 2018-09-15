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
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.QuadTree;
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.quadtree.QuadTree.CoordHolder;
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
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

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
    private Set<Integer>[][] nodeMatrix;
    
    //SearchNode[] searchNodes;
    Map<Integer, SearchNode> searchNodes;
    List<OnDemandVehicleStation> depos;
    
    Map<Integer, List<Integer>> filteredTrips;
    double LAT = 59.41;
    double LON =  24.725;
    
    double targetToNode[] = {0, Double.MAX_VALUE, 0};
    
    
    private QuadTree<SimulationEdge> quadTree;
    int qtcounter = 0;
    
    /**
     * Converts list of TimeValueTrip to TSTrip (class used for search).
     * 
     * @param config
     * @param nearestElementUtils
     * @param tripsUtil
     * @param graph
     */
    
    public TripList(AmodsimConfig config, NearestElementUtils nearestElementUtils, TripsUtil tripsUtil,
        Graph<SimulationNode,SimulationEdge> graph){
        this.config = config;
        this.nearestElementUtils = nearestElementUtils;
        this.tripsUtil = tripsUtil;
        this.graph = graph;
        
        quadTree = new QuadTree<>();
                graph.getAllEdges().forEach((edge) -> {
            quadTree.place((edge.fromNode.getLatE6()+edge.toNode.getLatE6())/2,
                            (edge.fromNode.getLonE6()+edge.toNode.getLonE6())/2, edge);
        });
        System.out.println(quadTree.size());
        numOfDepos = config.stations.regions;
        
        if(DBG){
            filteredTrips = new HashMap<>();
            for(int i = 0; i < 8; i++ ){
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
        //searchNodes = new SearchNode[numOfTrips*2+numOfDepos];
        
//        String log = config.amodsimDataDir + "/filter_startToNode.txt";
//        File file = new File(log);
//                try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
//                    for (TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(sourceList, 
//                        "Process GPS trip: ")) {
//                         gpsTripToSearchNode(trip, pw);
//                    }    
//                }catch (IOException ex){
//                    
//                }
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
            LOGGER.info("Trips longer than 25km {}", 
                filteredTrips.get(2).size());
            LOGGER.info("Start and target at the same node {}",
                filteredTrips.get(3).size());
            LOGGER.info("Haversine longer than shortest path {}",
                filteredTrips.get(4).size());
            LOGGER.info("Start too far from node {}",
                filteredTrips.get(6).size());
            LOGGER.info("Target too far from node {}",
                filteredTrips.get(7).size());
        }
        System.out.print("QT counter " + qtcounter);
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
//        for(int i = 0; i < numOfDepos; i++){
//            OnDemandVehicleStation depo = depos.get(i);
//            stations.add(depo.getPosition().id);
//            int id = numOfTrips*2 + i;
//            searchNodes.put(id, new SearchNode(id, depo.getPosition().id));
//            //searchNodes[id] = new SearchNode(id, depo.getPosition().id);
//        }
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
        //debug, after filtering by boundig box, nothing is filtered here
//        double startFromCenter = DistUtils.getHaversineDist(LAT, LON, lat1, lon1);
//        double targetFromCenter = DistUtils.getHaversineDist(LAT, LON, lat2, lon2);
//        if(startFromCenter > 150000){
//            if (DBG){
//              //  LOGGER.warn("Start too far from center {}", startFromCenter);
//                filteredTrips.get(5).add(trip.id);
//            }
//            return;
//        }
//        if( targetFromCenter > 150000){
//            if (DBG){
//             //   LOGGER.warn("Targert too far from center {}", targetFromCenter);
//                filteredTrips.get(5).add(trip.id);
//             }
//            return;
//        }
        
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
        // the trip is too long, limit is 5km longer for now cause of the possible difference
        // in haversine and shortest path lengths
        if(dist0 > 25000){
            if (DBG){
                filteredTrips.get(2).add(trip.id);
              //  LOGGER.warn("trip is longer than 30km {}", dist2);
            }
            return;
        }
        
 //         SimulationNode startNode = nearestElementUtils.getNearestElement(locations.get(0), EGraphType.HIGHWAY);
            SimulationNode startNode = findNearestNode(startLocation);
 
           double startToNodeDist = DistUtils.getEuclideanDist(lat1, lon1, 
                                        startNode.getLatitude(), startNode.getLongitude());
        
        // If location is further than pickup radius from its nearest node:
        // nearest node is further than pickup radius from the start location 
        if(startToNodeDist > 50){
//            System.out.println(startToNodeDist);
//            String output = String.format("Start: %d %f %f %f %f %f\n", trip.id, 
//                lat1, lon1, startNode.getLatitude(), startNode.getLongitude(), startToNodeDist);
//            System.out.print(output);
            filteredTrips.get(6).add(trip.id);
            //pw.write(output);
           
                return;
       }
        
//        SimulationNode targetNode = nearestElementUtils.getNearestElement(locations.get(1), EGraphType.HIGHWAY);
        SimulationNode targetNode = findNearestNode(locations.get(1));

        double targetToNodeDist = DistUtils.getEuclideanDist(lat2, lon2, 
            targetNode.getLatitude(), targetNode.getLongitude());
//        
        // nearest node is further than pickup radius from the target location 
        if(targetToNodeDist > 50){
//            String output = String.format("Target: %d %f %f %f %f %f\n", trip.id, 
//                lat1, lon1, targetNode.getLatitude(), targetNode.getLongitude(), targetToNodeDist);
//            System.out.print(output);
            filteredTrips.get(7).add(trip.id);
           // pw.write(output);
          //  return;
        }
        // Same start and target node, zero length trips
        if(startNode == targetNode){
             filteredTrips.get(3).add(trip.id);
//            String output = String.format("Same start-target: %d %f %f %f %f (%d %f %f)\n",
//                trip.id, lat1, lon1, lat2, lon2,
//                startNode.id, startNode.getLatitude(), startNode.getLongitude());
 //           System.out.print(output);
          //  pw.write(output);
            return;
        }
        //finally adding the node to the list
        times[trip.id] = (int) trip.getStartTime();
        values[trip.id] = trip.getRideValue();
        searchNodes.put(trip.id, new SearchNode(trip.id, startNode.id));
        
        // debug, writing data for trips, for which distance returned by haversine
        // is bigger than the length of the shortest path
//        int pathDist = getShortesPathDist(startNode, targetNode);
//        if(dist2 > pathDist){
//         //   String output = String.format("Hvs > path: %d %f %f %f %f : %f %d %f\n", 
//         //       trip.id, lat1, lon1, startNode.getLatitude(), startNode.getLongitude(),
//         //       dist2, pathDist, dist2-pathDist);
//         filteredTrips.get(4).add(trip.id);
//          //  System.out.print(output);
//          //  pw.write(output);
//          }
        
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

    private SimulationNode findNearestNode(GPSLocation loc){
        double deltaLat = 0.0005*1E6;
        double deltaLon =  0.001*1E6;
        int lat = loc.getLatE6();
        int lon = loc.getLonE6();
        List<QuadTree<SimulationEdge>.CoordHolder> neighbors = quadTree.findAll(lat-deltaLat, lon-deltaLon,
                                                                                lat+deltaLat, lon+deltaLon);

        while(neighbors.isEmpty()){
            //System.out.println("empty neighborhood "+deltaLat+" "+deltaLon);
            deltaLat *= 2;
            deltaLon *= 2;
            qtcounter++;
            neighbors = quadTree.findAll(lat-deltaLat, lon-deltaLon,
                                          lat+deltaLat, lon+deltaLon);
        }
        
        SimulationNode closest = null;
        double minDist = Double.MAX_VALUE;
        for(QuadTree<SimulationEdge>.CoordHolder holder : neighbors){
            //System.out.println("found an item: " + holder.o);
            SimulationEdge edge = holder.o;
            SimulationNode node = edge.fromNode;
                 
            double dist = DistUtils.getEuclideanDist(loc.getLatitude(), loc.getLongitude(),
                                                     node.getLatitude(), node.getLongitude());
            if(dist <=50 ){
                return node;
            }
            if(dist < minDist){
                minDist = dist;
                closest = node;
            }
            node = edge.toNode;
            dist = DistUtils.getEuclideanDist(loc.getLatitude(), loc.getLongitude(),
                                                     node.getLatitude(), node.getLongitude());
            if(dist <=50 ){
                return node;
            }
            if(dist < minDist){
                minDist = dist;
                closest = node;
            }
        }
        System.out.println("Nearest node at "+minDist+" m from the point");
        return closest;
    }
}

       
        
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
    


