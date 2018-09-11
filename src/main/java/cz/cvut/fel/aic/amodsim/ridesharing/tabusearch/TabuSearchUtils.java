/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.io.TimeValueTrip;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class TabuSearchUtils {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TabuSearchUtils.class);
    private static  boolean verbose = false;
           
    /**
     * Wrapping TripTransform.loadTripsFromTxt.
     * Reads trip data to list, prints some stats.
     * @param config
     * @param tripTransform
     * @return 
     */
    public static List<TimeValueTrip<SimulationNode>> loadTrips(AmodsimConfig config,
        TripTransform tripTransform){

        String tripsPath = config.amodsim.tripsPath;
        List<TimeValueTrip<SimulationNode>> list = tripTransform.loadTripsFromTxt(new File(tripsPath));
        StringBuilder sb = new StringBuilder();
        sb.append("Total number of demands: ").append(tripTransform.getTotalTrips()).append("\n");
        sb.append("Same start and target location: ").append(tripTransform.getSameStartAndTargetInDataCount()).append("\n");
        sb.append("Zero length: ").append(tripTransform.getZeroLenghtTripsCount()).append("\n");
        sb.append("Number of valid demands: ").append(list.size()).append("\n");
        sb.append("Checksum:").append(tripTransform.getSameStartAndTargetInDataCount() +
                                      tripTransform.getZeroLenghtTripsCount()+
                                      list.size()).append(" == ").append(tripTransform.getTotalTrips()).append(" \n");
        return list;       
    }
    
     /**
     * Adds paths to TimeValueTrip instances.
     * If file with paths exists, reads paths from file, 
     * otherwise computes them.
     * 
     * @param tripList list with trips 
     * @param pathsFile path to the file with precomputed paths
     * @param tripsUtil
     * @param graph
     * @return 
        */
    public static List<TimeValueTrip<SimulationNode>> addPaths(List<TimeValueTrip<SimulationNode>> tripList, 
        String pathsFile, TripsUtil tripsUtil, Graph<SimulationNode,SimulationEdge> graph){
           
        try{
            Map<Integer, List<Integer>> pathMap = readPathsFromFile(pathsFile);
            LOGGER.info("Reading paths from file");
            for(TimeValueTrip trip: tripList){
                if(pathMap.containsKey(trip.id)){
                    trip.setPath(pathMap.get(trip.id).stream().mapToInt(Integer::intValue).toArray());
                }
            }
        }catch (FileNotFoundException ex){
            LOGGER.error(null, ex);
        }
        
        StringBuilder sb = new StringBuilder();
        int sumNumberOfNodes = 0;
        int minNumberOfNodes = Integer.MAX_VALUE;
        int maxNumberOfNodes = 0;
        long sumLengths = 0;
        long minLength = Integer.MAX_VALUE;
        long maxLength = 0;
        int tooLong = 0;
        int tooShort = 0;
        int sameNode = 0;
        
        long start = System.currentTimeMillis();
        for(TimeValueTrip trip: tripList){
            int len = 0;
            if(!trip.hasPath()){
                List<SimulationNode> locations = trip.getLocations();
                SimulationNode startNode = locations.get(0);
                SimulationNode targetNode = locations.get(1);
                if(startNode == targetNode){
                    sameNode++;
                    //LOGGER.info("Trip with start and end at the same node found {}",  sameNode);
                    continue;
                }
                if (getDistanceSquared(startNode,targetNode) <= 50*50){
                    tooShort++;
                    //LOGGER.info("Too short trip found {}", tooShort);
                    continue;
                }
                LinkedList<SimulationNode> path = tripsUtil.createTrip(startNode.id, targetNode.id).getLocations();
                len = findExactLength(path, graph);
                if(len >= 25000){
                    tooLong++;
                    //LOGGER.info("Too long trip found {}", tooLong);
                    continue;
                }
                trip.setPath(path.stream().map(node->node.id).mapToInt(Integer::intValue).toArray());
            }
                        
            if(verbose){
                int numNodes = trip.getPath().length;
                sumNumberOfNodes += numNodes;
                minNumberOfNodes = minNumberOfNodes <= numNodes ? minNumberOfNodes : numNodes;
                maxNumberOfNodes = maxNumberOfNodes >= numNodes ? maxNumberOfNodes : numNodes;
                len = len != 0 ? len : findExactLength(trip.getPath(), graph);
                sumLengths += len;
                minLength = minLength <= len ? minLength : len;
                maxLength = maxLength >= len ? maxLength : len;
            }
        }
            
        long end = System.currentTimeMillis();
        List<TimeValueTrip<SimulationNode>> tripPaths = tripList.stream().filter(trip->trip.hasPath()).
                collect(Collectors.toList());

        sb.append("Paths build in ").append((end - start )/1000.0).append(" seconds\n");
        sb.append("Path stats for valid trips: \n");
        if(verbose){
            sb.append("Min number of nodes: ").append(minNumberOfNodes).append(", longest: ").append(maxNumberOfNodes).
                append("\n");
            sb.append("Average: ").append(sumNumberOfNodes/tripPaths.size()).append(" nodes per path \n");
            sb.append("Shortest: ").append(minLength).append(" meters, longest: ").append(maxLength).append(" meters \n");
            sb.append("Average: ").append(sumLengths/tripPaths.size()).append(" meters \n");
        }
        sb.append("Found ").append(sameNode).append(" trips starting and ending at the same node \n");
        sb.append("Found ").append(tooShort).append(" trips shorter than 50 meters along the straight line \n");
        sb.append("Found ").append(tooLong).append(" trips longer than 25000 m along the shortest path \n");
        sb.append("Number of remaining trips: ").append(tripPaths.size()).append("\n");
        sb.append("Number of discarded trips: ").append(tooLong + tooShort+sameNode).append("\n");
        sb.append("Checksum: ").append( tooLong + tooShort+sameNode + tripPaths.size()).append(" = ").
            append(tripList.size()).append("\n");
        System.out.println(sb.toString());
        return tripPaths;

    }
    /**
     * Returns path length in meters.
     * (sum of length of all edges in the path)
     * @param path list of nodes
     * @param graph
     */
    public static int findExactLength(List<SimulationNode> path, Graph<SimulationNode, SimulationEdge> graph){
        int totalLength = 0;
		Iterator<SimulationNode> nodeIterator = path.iterator();
		Node fromNode = nodeIterator.next();
		while (nodeIterator.hasNext()) {
			Node toNode = nodeIterator.next();
			SimulationEdge edge = graph.getEdge(fromNode, toNode);
			totalLength += edge.length;
			fromNode = toNode;
		}
        return totalLength;
    }
    
    /**
     * Returns path length in meters.
     * (sum of length of all edges in the path)
     * @param path array of node ids
     */
    public static int findExactLength(int[] path, Graph<SimulationNode, SimulationEdge> graph){
        List<SimulationNode> nodeList = Arrays.stream(path).mapToObj(id->graph.getNode(id)).
            collect(Collectors.toList());
        return findExactLength(nodeList, graph);
    }
    /**
     * Returns square of distance between two nodes in meters.
     * 
     */    
    public static double getDistanceSquared(SimulationNode node1, SimulationNode node2){
        double dist_x = node2.getLatitudeProjected() - node1.getLatitudeProjected();
        double dist_y = node2.getLongitudeProjected() - node1.getLongitudeProjected();
        return dist_x * dist_x + dist_y * dist_y;
    }
    /**
      * Writes shortest paths for trips to .txt file.
      * Each trip starts from the new line, and consists of trip id
      * and path nodes ids separated by space.
      * tripId nodeId0 nodeId1 ... nodeIdn
      */     
    public static void writePathsToFile(String fileName, List<TimeValueTrip> tripList){
  
        File file = new File(fileName);
        try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
            for(TimeValueTrip trip: tripList){
                pw.print(trip.id);
                pw.print(" ");
                pw.println(locationsToString(trip));
            }
            
       }catch (IOException ex){
           LOGGER.error(null, ex);          
       }
    }
    

    /**
     * Reads trips paths from .txt file, written by writePathsFromFile().
     *  Returns map, there key is trip id, value array of nodes 
     * @param fileName
     * @return 
     * @throws java.io.FileNotFoundException
    */
    public static Map<Integer, List<Integer>> readPathsFromFile(String fileName) throws FileNotFoundException{

        Map<Integer, List<Integer>> tripPaths = new HashMap<>();
        File file = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                List<Integer> ids = Arrays.stream(line.split(" ")).
                    map(x-> Integer.parseInt(x)).collect(Collectors.toList());
                tripPaths.put(ids.remove(0), ids);
            }
        } catch (IOException ex) {
            LOGGER.error(null, ex); 
        }
        return tripPaths;
    }
    
    private static String locationsToString(TimeValueTrip<SimulationNode> trip){
		return Arrays.stream(trip.getLoacationIds()).mapToObj(String::valueOf).collect(Collectors.joining(" "));
    }
    
    /**
     * Return sum of ride_values for all trips in the list.
     * @param tripList
     * @return 
     */
    public static double getTripListValue(List<TimeValueTrip<SimulationNode>> tripList){
        double value = tripList.stream().map(trip->trip.getRideValue()).mapToDouble(Double::doubleValue).sum();
        return value;
    }
    
    /**
     * Calculates how many orders arrive during given perion of time.
     * @param tripList 
     * @param period  seconds
     * @return 
     */
    public static double avgTimeBtwTrips(List<TimeValueTrip<SimulationNode>> tripList,
        int period){
        double sumFrequency = 0;
        long maxFrequency = 0;
        long minFrequency = Integer.MAX_VALUE;
        long periodMS = period * 1000;
        
        long prevTime = 0;
        int frequency = 0;
        int numOfPeriods = 1;
        for(int i = 0; i < tripList.size(); i++){
            TimeValueTrip trip = tripList.get(i);
            long delta = trip.getStartTime() - prevTime;
            if(delta < periodMS){
                frequency++;
            }else{
                i--;
                prevTime += periodMS;
                maxFrequency = maxFrequency >= frequency ? maxFrequency : frequency;
                minFrequency = minFrequency <= frequency ? minFrequency : frequency;
                sumFrequency += frequency;
                numOfPeriods ++;
                frequency = 0;
            }
        }

        StringBuilder sb = new StringBuilder("Times between trips: \n");
        sb.append("Max density: ").append(maxFrequency).append(" orders per ").append(period).append(" sec \n");
        sb.append("Min density: ").append(minFrequency).append(" orders per ").append(period).append(" sec \n");
        sb.append("Average: ").append(sumFrequency/numOfPeriods).append(" trips per minute (")
            .append(60*sumFrequency/numOfPeriods).append(" per hour)\n");
        
        System.out.println(sb.toString());
        return sumFrequency/numOfPeriods;
    }
    
    
    
    /**
     * Creates shortest paths from each station to all nodes in the graph.
     * All paths are written to .txt file in the given directory.
     * Calculates average time for finding paths for one stations.
     * @param stations
     * @param graph
     * @param tripsUtil
     * @param dirName 
     */
    public static void stationsPaths(List<OnDemandVehicleStation> stations, 
        Graph<SimulationNode, SimulationEdge> graph, TripsUtil tripsUtil,
        String dirName){
        double sumTimes = 0;
        for(OnDemandVehicleStation station : stations){
            SimulationNode node = station.getPosition();
            sumTimes += allPaths(node, graph, tripsUtil, dirName + "/"+node.id+".txt");
        }
        StringBuilder sb = new StringBuilder("Shortest paths for stations:\n");
        sb.append("Average duration: ").append(sumTimes/stations.size()).append("\n");
        
        System.out.println(sb.toString());
    }
//    
//    public static void pathsNoLongerThan(SimulationNode start, 
//        Graph<SimulationNode, SimulationEdge> graph, int maxLength){
//        
//        Deque<SimulationNode> queue = new ArrayDeque<>();
//        queue.push(start);
//        
//        List<List<Integer>> paths = new ArrayList<>();
//        SimulationNode node;
//        int totalDist = 0;
//        List<Integer> path = new ArrayList<>();
//        while((node = queue.pollFirst()) != null){
//            path.add(node.id);
//            for(SimulationEdge edge: graph.getOutEdges(node)){
//                if((totalDist + edge.length) > maxLength){
//                    paths.add(path);
//                }else{
//                    SimulationNode next = edge.toNode;
//          
//                
//            }
//        }
//         
//        
//    }
    
    /**
     * Finds shortest paths from the given node to all other nodes in the graph.
     * Writes result to file.
     * @param start
     * @param graph
     * @param tripsUtil
     * @param outFileName
     * @return  time nedeed to find all paths
     */
    public static double allPaths(SimulationNode start,Graph<SimulationNode, SimulationEdge> graph,
        TripsUtil tripsUtil, String outFileName){
        int pathCounter = 0;
        int emptyPathCounter = 0;
        int tooLong = 0;
        int sumLengths = 0;
        int maxLength = 0;
        int maxNumNodes = 0;
        int sumNumNodes = 0;
        List<int[]> paths = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for(SimulationNode node: graph.getAllNodes()){
            if(start.id != node.id){
                int[] path = tripsUtil.createTrip(start.id, node.id).getLoacationIds();
                pathCounter++;
                int dist = findExactLength(path, graph);
                if(path.length == 1){
                    emptyPathCounter++;
                }else if(dist > 200000){
                    tooLong++;
                }else{
                    if(verbose){
                        sumNumNodes += path.length;
                        maxNumNodes = maxNumNodes >= path.length ? maxNumNodes : path.length;
                        maxLength = maxLength >= dist ? maxLength : dist;
                        sumLengths += dist;
                    }
                    paths.add(path);
                }
            }
        }
        long endTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("Shortest paths for node ").append(start.id).append(" built in ")
            .append((endTime-startTime)/1000).append(" sec \n");
        if(verbose){
            sb.append("Target nodes checked: ").append(pathCounter).append("\n");
            sb.append("Empty: ").append(emptyPathCounter).append(", too long: ").append(tooLong).append("\n");
            sb.append("Length in meters: avg ").append(sumLengths/paths.size()).append(", longest ")
                .append(maxLength).append(" \n");;
            sb.append("Nodes per path: avg ").append(sumNumNodes/paths.size())
                .append(", max ").append(maxNumNodes).append(" \n");
        }
            System.out.println(sb.toString());
        
        File file = new File(outFileName);
        try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
            for(int[] path: paths){
                pw.println(Arrays.stream(path).mapToObj(String::valueOf).
                    collect(Collectors.joining(" ")));
            }
        }catch (IOException ex){
           LOGGER.error(null, ex);          
       }
        return (endTime-startTime)/1000;
    }
    
    public static void edgeStats( Graph<SimulationNode, SimulationEdge> graph ){
        StringBuilder sb = new StringBuilder();
        int maxLength = 0;
        int minLength = Integer.MAX_VALUE;
        int sumLengths = 0;
        for(SimulationEdge edge: graph.getAllEdges()){
            sumLengths += edge.length;
            minLength = minLength <= edge.length ? minLength : edge.length;
            maxLength = maxLength >= edge.length ? maxLength : edge.length;
        }
              
        sb.append("Edge length stats: \n");
        sb.append("Number of edges: ").append(graph.numberOfEdges()).append("\n");
        sb.append("Shortest: ").append(minLength).append(" m, longest: ").append(maxLength).append("m \n");
        sb.append("Average edge length: ").append(sumLengths/graph.numberOfEdges()).append(" m \n");
        System.out.println(sb.toString());
    }
    
    public static void nodeStats(Graph<SimulationNode, SimulationEdge> graph ){
        StringBuilder sb = new StringBuilder();
        int maxIn = 0;
        int maxOut = 0;
        int minIn = Integer.MAX_VALUE;
        int minOut = Integer.MAX_VALUE;
        int inOut = 0;
        int sumIn = 0;
        int sumOut = 0;
        
        for(SimulationNode node: graph.getAllNodes()){
            int in = graph.getInEdges(node).size();
            int out = graph.getOutEdges(node).size();
            maxIn = maxIn >= in ? maxIn : in;
            maxOut = maxOut >= out ? maxOut : out;
            minIn = minIn <= in ? minIn : in;
            minOut = minOut <= out ? minOut : out;
            sumIn += in;
            sumOut += out;
            if(in != out){
                inOut++;
            }
        }
         
         
        sb.append("Node stats: \n");
        sb.append("Number of nodes: ").append(graph.numberOfNodes()).append("\n");
        sb.append("In max: ").append(maxIn).append(" edges, in min: ").append(minIn).append("edges \n");
        sb.append("Average incoming: ").append(sumIn/graph.numberOfNodes()).append(" edges \n");
        sb.append("Out max: ").append(maxOut).append(" edges, out min: ").append(minIn).append("edges \n");
        sb.append("Average outcoming: ").append(sumOut/graph.numberOfNodes()).append(" edges \n");
        sb.append("In != out: ").append(inOut).append("\n");
        System.out.println(sb.toString());
    }
    //
    public static Set[][] buildMatrix(double[] bbox, double step,  Graph<SimulationNode, SimulationEdge> graph ){
        StringBuilder sb = new StringBuilder();
        
        int numRows = (int) ((bbox[1] - bbox[0])/step);
        int numCols = (int) ((bbox[3] - bbox[2])/step);
        sb.append("Matrix  ").append(numRows).append(" x ").append(numCols).append(" \n");
        Set[][] nodeMatrix = new Set[numRows][numCols];
        for(int r = 0; r < numRows; r++){
            for(int c = 0; c < numCols; c++) {
                nodeMatrix[r][c] = new HashSet<>();
            }
        }
        
        for(SimulationNode node: graph.getAllNodes()){
            int[] ind = findCell(node,bbox[0],bbox[2], step);
            int row = ind[0];
            int col = ind[1];
            try{
                nodeMatrix[row][col].add(node);
            }catch (ArrayIndexOutOfBoundsException ex){
                LOGGER.error("Array index is out of bound, row {}, col {}", row, col);
                row = row >= numRows ?  row : numRows - 1;                    
                col = col >= numCols ?   col : numCols - 1;
                nodeMatrix[row][col].add(node);
            }
        }//for
        sb.append("Done.  \n");
    
         matrixRowDist(nodeMatrix);
         matrixColumnDist(nodeMatrix);
         matrixDensity(nodeMatrix);
        
        
        System.out.println(sb.toString());
        return nodeMatrix;
    }
      
    private static int[] findCell(SimulationNode node, double minLat, double minLon, double step){
        int rowInd = (int) ((node.getLatitude() - minLat)/step);
        int colInd = (int) ((node.getLongitude() - minLon)/step);
        int[] ind = {rowInd, colInd};   
        return ind;
    }
    
    private static void matrixRowDist(Set[][] nodeMatrix){
        StringBuilder sb = new StringBuilder();
        int numRows = nodeMatrix.length;
        int numCols = nodeMatrix[0].length;
        
        sb.append("Row distance estimates: \n"); 
        int count = 0;
        double sumDeltaDist= 0;
        double maxDelta = 0;
        double minDelta = 100000;
        for(int r = 0; r < numRows; r++){
            SimulationNode node1 = null;
            int node1Col=0;
            for(int c = 0; c < numRows; c++){
                Set nodes1 = nodeMatrix[r][c];
                if(nodes1.isEmpty()){
                }else{
                    if(node1 == null){
                        node1 = (SimulationNode) nodes1.iterator().next();
                        node1Col = c;
                    }else{
                        SimulationNode node2 = (SimulationNode) nodes1.iterator().next();
                        double dist = Math.sqrt(getDistanceSquared(node1, node2));
                        int deltaCol = c - node1Col;
                        double deltaDist = dist/deltaCol;
//                        sb.append("Row difference = ").append(deltaRow).append(", distance = ").append(dist).
//                            append("m, delta = ").append(deltaDist).append("m \n");
                        count++;
                        sumDeltaDist += deltaDist;
                        maxDelta = maxDelta >= deltaDist ? maxDelta : deltaDist;
                        minDelta = minDelta <= deltaDist ? minDelta : deltaDist;
                        node1 = node2;
                        node1Col = c;
                    }
                }
            }//for
        }
        sb.append("Average distance delta along the row, per cell: ").append(sumDeltaDist/count).append("m \n");
        sb.append("Delta ranging from ").append(minDelta).append(" to ").append(maxDelta).append(" \n");
        System.out.println(sb.toString());
}
    
    private static void matrixColumnDist(Set[][] nodeMatrix){
        StringBuilder sb = new StringBuilder();
        
        int numRows = nodeMatrix.length;
        int numCols = nodeMatrix[0].length;
        
        sb.append("Column distance estimates: \n"); 
        int count = 0;
        double sumDeltaDist= 0;
        double maxDelta = 0;
        double minDelta = 100000;
        for(int c = 0; c < numCols; c++){
            SimulationNode node1 = null;
            int node1Row=0;
            for(int r = 0; r < numRows; r++){
                Set nodes1 = nodeMatrix[r][c];
                if(nodes1.isEmpty()){
                }else{
                    if(node1 == null){
                        node1 = (SimulationNode) nodes1.iterator().next();
                        node1Row = r;
                    }else{
                        SimulationNode node2 = (SimulationNode) nodes1.iterator().next();
                        double dist = Math.sqrt(getDistanceSquared(node1, node2));
                        int deltaRow = r - node1Row;
                        double deltaDist = dist/deltaRow;
//                        sb.append("Row difference = ").append(deltaRow).append(", distance = ").append(dist).
//                            append("m, delta = ").append(deltaDist).append("m \n");
                        count++;
                        sumDeltaDist += deltaDist;
                        maxDelta = maxDelta >= deltaDist ? maxDelta : deltaDist;
                        minDelta = minDelta <= deltaDist ? minDelta : deltaDist;
                        node1 = node2;
                        node1Row = r;
                    }
                }
            }
        }
        sb.append("Average distance delta along the column,  per cell: ").append(sumDeltaDist/count).append("m \n");
        sb.append("Delta ranging from ").append(minDelta).append(" to ").append(maxDelta).append(" \n");
        System.out.println(sb.toString());
}
    
    private static void matrixDensity(Set[][] nodeMatrix){
        StringBuilder sb = new StringBuilder();
        int numRows = nodeMatrix.length;
        int numCols = nodeMatrix[0].length;
        int maxDensity = 0;
        int minDensity = Integer.MAX_VALUE;
        int sumDensity = 0;
        for(Set[] row : nodeMatrix){
           // sb.append("\n* New row: \n");
            for (Set nodes : row){
              //  sb.append(nodes.size()).append(", ");
                sumDensity += nodes.size();
                minDensity = minDensity <= nodes.size() ? minDensity : nodes.size();
                maxDensity = maxDensity >= nodes.size() ? maxDensity : nodes.size();
            }
        }
    //    sb.append("\n");
        sb.append("Density: min = ").append(minDensity).append(", max = ").append(maxDensity).append(", avg = ").
            append(sumDensity/(numRows*numCols)).append("\n");
        System.out.println(sb.toString());
        
    }
 
}


//   public static List<TimeValueTrip<SimulationNode>> filterTrips(List<TimeValueTrip<SimulationNode>> list, 
//        TravelTimeProvider travelTimeProvider, long maxRideTime){
//        StringBuilder sb = new StringBuilder();
//        sb.append("Filtering \n Number of trips before: ").append(list.size()).append("\n");
//        
//        int oldSize = list.size();
//        double totalTimeOfValidDemands = 0;
//        int tooLongTripsCount = 0;
//        double value = 0;
//       
//        List<TimeValueTrip<SimulationNode>> validTrips = new ArrayList<>();
//        for(TimeValueTrip trip: list){
//            List<SimulationNode> locations = trip.getLocations();
//            SimulationNode startNode = locations.get(0);
//            SimulationNode targetNode = locations.get(locations.size()-1);
//     
//            double travelTime = travelTimeProvider.getTravelTime(startNode, targetNode);
//            if(travelTime >= maxRideTime){
//                tooLongTripsCount++;
//            }else{
//                validTrips.add(trip);
//                totalTimeOfValidDemands += travelTime;
//                value += trip.getRideValue();
//            }
//        }
//        int size = validTrips.size();
//        sb.append("Number of trips after: ").append(size).append(" ,").append(100*size/oldSize).append("%  \n");
//        sb.append("Too long trips declined: ").append(tooLongTripsCount).append(" ,").
//            append(100*tooLongTripsCount/oldSize).append("%  \n");
//        double avgDuration = totalTimeOfValidDemands/size/1000;
//        sb.append("Average trip duration: ").append(avgDuration).append("s \n");
//        sb.append("Average trip length: ").append(avgDuration*13.9).append("m \n");
//        sb.append("Price per km: ").append(13.9*1000000*value/totalTimeOfValidDemands).append("\n");
//        System.out.println(sb.toString());
//        
//        return validTrips;
//    }
