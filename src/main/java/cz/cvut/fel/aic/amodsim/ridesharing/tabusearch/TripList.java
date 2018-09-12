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
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private final TravelTimeProvider travelTimeProvider;
    //Set<Integer>[][] pMatrix;
    //Set<Integer>[][] dMatrix;
    //
    private final int numOfTrips;
    private final int numOfDepos;
    
    double[] bbox = {59.3, 59.52, 24.5, 24.955};    
    int[] times;
    double[] values;
    SearchNode[] searchNodes;
   
    int furtherThanPath = 0;
    int outOfBox = 0;
    Map<Integer, double[]> badTrips;
    Map<Integer, double[]> badPaths;
    Map<Integer, double[]>byBox ;
    /**
     * Converts list of TimeValueTrip to TSTrip (class used for search).
     * 
     * @param config
     * @param nearestElementUtils
     * @param tripsUtil
     * @param graph
     */
    
    public TripList(AmodsimConfig config, NearestElementUtils nearestElementUtils, TripsUtil tripsUtil,
        Graph<SimulationNode,SimulationEdge> graph, TravelTimeProvider travelTimeProvider){
        LOGGER.info("Trip list initialization started");
        this.config = config;
        this.nearestElementUtils = nearestElementUtils;
        this.tripsUtil = tripsUtil;
        this.graph = graph;
        this.travelTimeProvider = travelTimeProvider;
        numOfDepos = config.stations.regions;
        
        List<TimeValueTrip<GPSLocation>> sourceList = loadTrips();
        numOfTrips = sourceList.get(sourceList.size()-1).id + 1;
        
        badTrips = new HashMap<>();
        badPaths = new HashMap<>();
        byBox = new HashMap<>();
        
        LOGGER.info(" array length {}", numOfTrips);
        times = new int[numOfTrips];
        values = new double[numOfTrips];
        searchNodes = new SearchNode[numOfTrips*2+numOfDepos];
        
        int zeroLenghtTripsCount = 0;
        for (TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(sourceList, "Process GPS trip: ")) {
                zeroLenghtTripsCount += gpsTripToSearchNode(trip);
        }
        if(DBG){
            LOGGER.info("Euclidean >= path {}", furtherThanPath); 
            LOGGER.info("Out of boundaries {}", outOfBox);
        }
        LOGGER.info("Zero length trips discarded {}", zeroLenghtTripsCount);
        LOGGER.info("Trips added");
        LOGGER.info("TripList initialization finished");
  
        if(DBG){
            LOGGER.info("Writing bad trips to file");
            String tripFileName = config.amodsimDataDir + "/strange_trips.txt";
            File file = new File(tripFileName);
            try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
                for(Integer id : badTrips.keySet()){
                    double[] coord = badTrips.get(id);
                    pw.println(String.format("%d %f %f", id, coord[0], coord[1]));
                }
            }catch (IOException ex){
               LOGGER.error(null, ex);          
           }
            String pathFileName = config.amodsimDataDir + "/strange_paths.txt";
            file = new File(pathFileName);
            try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
                for(Integer id : badPaths.keySet()){
                    pw.print(id);
                    pw.print(" ");
                    double[] pathCoord = badPaths.get(id);
                    for(int i = 0; i < pathCoord.length; i+=2){
                        pw.print(String.format("%f %f ", pathCoord[i], pathCoord[i+1]));
                    }
                    pw.print("\n");
                }
            }catch (FileNotFoundException ex){
               LOGGER.error(null, ex);          
           }
            String boxFileName = config.amodsimDataDir + "/out_of_box.txt";
            file = new File(boxFileName);
            try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
                for(Integer id : byBox.keySet()){
                    double[] coord = byBox.get(id);
                    pw.println(String.format("%d %f %f", id, coord[0], coord[1]));
                }
            }catch (IOException ex){
               LOGGER.error(null, ex);          
           }
        }
        
        
        
    }
    
    protected void addDepoNodesToList(List<OnDemandVehicleStation> depos){
        if(numOfDepos != depos.size()){
            LOGGER.error("Depo list length differs from numOfDepos");
            return;
        }
        for(int i = 0; i < numOfDepos; i++){
            OnDemandVehicleStation depo = depos.get(i);
            int id = numOfTrips*2 + i;
            searchNodes[id] = new SearchNode(id, depo.getPosition().id);
        }
        LOGGER.info("Depos added");
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
    
    private int gpsTripToSearchNode(TimeValueTrip<GPSLocation> trip) {
        int zeroLenghtTripsCount = 0;
        List<GPSLocation> locations = trip.getLocations();
        StringBuilder sb = new StringBuilder();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);
        double euclideanDist = DistUtils.getApproximateDist(startLocation, targetLocation);
        //double haversineDist = DistUtils.getHaversineDist(startLocation, targetLocation);
        double lat1 = startLocation.getLatitude();
        double lon1 = startLocation.getLongitude();
        double lat2 = startLocation.getLatitude();
        double lon2 = startLocation.getLongitude();
        //System.out.println(String.format(" (%f  %f) (%f  %f)", lat1, lon1, lat2, lon2));
        if(lat1 < bbox[0] || lat1 > bbox[1] || lon1 < bbox[2] || lon1 > bbox[3]){
            double[] coord = {lat1, lon1};
            byBox.put(trip.id, coord);
           // System.out.println("Out of boundaries");
            outOfBox++;
        }
        if(lat2 < bbox[0] || lat2 > bbox[1] || lon2 < bbox[2] || lon2 > bbox[3]){
            double[] coord = {lat2, lon2};
            byBox.put(trip.id, coord);
            //System.out.println("Out of boundaries");
            outOfBox++;
        }
            
        //double euclideanDist = getEuclideanDist(startLocation, targetLocation);
        SimulationNode startNode = nearestElementUtils.getNearestElement(locations.get(0), EGraphType.HIGHWAY);
        SimulationNode targetNode = nearestElementUtils.getNearestElement(locations.get(locations.size() - 1), EGraphType.HIGHWAY);
        
        int pathDist = 0;
        if(startNode != targetNode){
            times[trip.id] = (int) trip.getStartTime();
            values[trip.id] = trip.getRideValue();
            searchNodes[trip.id] = new SearchNode(trip.id, startNode.id);
            int endId = trip.id + numOfTrips;
            searchNodes[endId] = new SearchNode(endId, targetNode.id);
            pathDist = getShortesPathDist(startNode, targetNode);
        }else{
            zeroLenghtTripsCount++;
       }
        if(DBG){
            if(pathDist > 0){
     //           double time = travelTimeProvider.getTravelTime(startNode, targetNode);
     //           System.out.println(String.format(" ETTP = %.4f, approx = %.4f , path = %d, haversine = %.5f", 
     //                time, euclideanDist, pathDist, haversineDist));
                 if(euclideanDist >= pathDist){
                     //System.out.println(String.format("Approx = %.4f , path = %d, haversine = %.5f", 
                     //euclideanDist,  pathDist, haversineDist)); 
                     furtherThanPath++;
                     double[] coord = {startLocation.getLatitude(), startLocation.getLongitude(),
                     targetLocation.getLatitude(), targetLocation.getLongitude()};
                     badTrips.put(trip.id, coord);
                     LinkedList<SimulationNode> path = tripsUtil.createTrip(startNode.id, targetNode.id).getLocations();
                     double[] pathCoord = DistUtils.pathToCoordinates(path);
                     badPaths.put(trip.id, pathCoord);

                 }
            }
        }
        
        return zeroLenghtTripsCount;
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
            SearchNode trip = searchNodes[ind];
            if(trip != null){
                result.add(trip);
                startTime = times[++ind];
                if(ind == numOfTrips){
                    break;
                }
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
        for(int i = 0; i < numOfTrips; i++){
            if(searchNodes[i] != null){
                value += values[i];
            }
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
        for (int i = 0; i < numOfTrips; i++) {
            if(searchNodes[i] == null){
                continue;
            }
            long delta = times[i] - prevTime;
            if (delta < periodMS) {
                frequency++;
            } else {
                i--;
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
}





//
//    public static Set[][] buildMatrix(double[] bbox, double step, Graph<SimulationNode, SimulationEdge> graph) {
//        StringBuilder sb = new StringBuilder();
//        int numRows = (int) ((bbox[1] - bbox[0]) / step);
//        int numCols = (int) ((bbox[3] - bbox[2]) / step);
//        sb.append("Matrix  ").append(numRows).append(" x ").append(numCols).append(" \n");
//        Set[][] nodeMatrix = new Set[numRows][numCols];
//        for (int r = 0; r < numRows; r++) {
//            for (int c = 0; c < numCols; c++) {
//                nodeMatrix[r][c] = new HashSet<>();
//            }
//        }
//        for (SimulationNode node : graph.getAllNodes()) {
//            int[] ind = findCell(node, bbox[0], bbox[2], step);
//            int row = ind[0];
//            int col = ind[1];
//            try {
//                nodeMatrix[row][col].add(node);
//            } catch (ArrayIndexOutOfBoundsException ex) {
//                TabuSearchUtils.LOGGER.error("Array index is out of bound, row {}, col {}", row, col);
//                row = row >= numRows ? row : numRows - 1;
//                col = col >= numCols ? col : numCols - 1;
//                nodeMatrix[row][col].add(node);
//            }
//        } //for
//        sb.append("Done.  \n");
//        matrixRowDist(nodeMatrix);
//        matrixColumnDist(nodeMatrix);
//        matrixDensity(nodeMatrix);
//        System.out.println(sb.toString());
//        return nodeMatrix;
//    }
//    public void matrixDensity(Set[][] nodeMatrix) {
//        StringBuilder sb = new StringBuilder();
//        int numRows = nodeMatrix.length;
//        int numCols = nodeMatrix[0].length;
//        int maxDensity = 0;
//        int minDensity = Integer.MAX_VALUE;
//        int sumDensity = 0;
//        for (Set[] row : nodeMatrix) {
//            // sb.append("\n* New row: \n");
//            for (Set nodes : row) {
//                //  sb.append(nodes.size()).append(", ");
//                sumDensity += nodes.size();
//                minDensity = minDensity <= nodes.size() ? minDensity : nodes.size();
//                maxDensity = maxDensity >= nodes.size() ? maxDensity : nodes.size();
//            }
//        }
//        //    sb.append("\n");
//        sb.append("Density: min = ").append(minDensity).append(", max = ").append(maxDensity).append(", avg = ").append(sumDensity / (numRows * numCols)).append("\n");
//        System.out.println(sb.toString());
//    }
//    private static int[] findCell(SimulationNode node, double minLat, double minLon, double step) {
//        int rowInd = (int) ((node.getLatitude() - minLat) / step);
//        int colInd = (int) ((node.getLongitude() - minLon) / step);
//        int[] ind = {rowInd, colInd};
//        return ind;
//    }
//    private static void matrixRowDist(Set[][] nodeMatrix) {
//        StringBuilder sb = new StringBuilder();
//        int numRows = nodeMatrix.length;
//        int numCols = nodeMatrix[0].length;
//        sb.append("Row distance estimates: \n");
//        int furtherThanPath = 0;
//        double sumDeltaDist = 0;
//        double maxDelta = 0;
//        double minDelta = 100000;
//        for (int r = 0; r < numRows; r++) {
//            SimulationNode node1 = null;
//            int node1Col = 0;
//            for (int c = 0; c < numRows; c++) {
//                Set nodes1 = nodeMatrix[r][c];
//                if (nodes1.isEmpty()) {
//                } else {
//                    if (node1 == null) {
//                        node1 = (SimulationNode) nodes1.iterator().next();
//                        node1Col = c;
//                    } else {
//                        SimulationNode node2 = (SimulationNode) nodes1.iterator().next();
//                        double dist = Math.sqrt(TabuSearchUtils.getDistanceSquared(node1, node2));
//                        int deltaCol = c - node1Col;
//                        double deltaDist = dist / deltaCol;
//                        //                        sb.append("Row difference = ").append(deltaRow).append(", distance = ").append(dist).
//                        //                            append("m, delta = ").append(deltaDist).append("m \n");
//                        furtherThanPath++;
//                        sumDeltaDist += deltaDist;
//                        maxDelta = maxDelta >= deltaDist ? maxDelta : deltaDist;
//                        minDelta = minDelta <= deltaDist ? minDelta : deltaDist;
//                        node1 = node2;
//                        node1Col = c;
//                    }
//                }
//            } //for
//        }
//        sb.append("Average distance delta along the row, per cell: ").append(sumDeltaDist / furtherThanPath).append("m \n");
//        sb.append("Delta ranging from ").append(minDelta).append(" to ").append(maxDelta).append(" \n");
//        System.out.println(sb.toString());
//    }
//    private static void matrixColumnDist(Set[][] nodeMatrix) {
//        StringBuilder sb = new StringBuilder();
//        int numRows = nodeMatrix.length;
//        int numCols = nodeMatrix[0].length;
//        sb.append("Column distance estimates: \n");
//        int furtherThanPath = 0;
//        double sumDeltaDist = 0;
//        double maxDelta = 0;
//        double minDelta = 100000;
//        for (int c = 0; c < numCols; c++) {
//            SimulationNode node1 = null;
//            int node1Row = 0;
//            for (int r = 0; r < numRows; r++) {
//                Set nodes1 = nodeMatrix[r][c];
//                if (nodes1.isEmpty()) {
//                } else {
//                    if (node1 == null) {
//                        node1 = (SimulationNode) nodes1.iterator().next();
//                        node1Row = r;
//                    } else {
//                        SimulationNode node2 = (SimulationNode) nodes1.iterator().next();
//                        double dist = Math.sqrt(TabuSearchUtils.getDistanceSquared(node1, node2));
//                        int deltaRow = r - node1Row;
//                        double deltaDist = dist / deltaRow;
//                        //                        sb.append("Row difference = ").append(deltaRow).append(", distance = ").append(dist).
//                        //                            append("m, delta = ").append(deltaDist).append("m \n");
//                        furtherThanPath++;
//                        sumDeltaDist += deltaDist;
//                        maxDelta = maxDelta >= deltaDist ? maxDelta : deltaDist;
//                        minDelta = minDelta <= deltaDist ? minDelta : deltaDist;
//                        node1 = node2;
//                        node1Row = r;
//                    }
//                }
//            }
//        }
//        sb.append("Average distance delta along the column,  per cell: ").append(sumDeltaDist / furtherThanPath).append("m \n");
//        sb.append("Delta ranging from ").append(minDelta).append(" to ").append(maxDelta).append(" \n");
//        System.out.println(sb.toString());
//    }