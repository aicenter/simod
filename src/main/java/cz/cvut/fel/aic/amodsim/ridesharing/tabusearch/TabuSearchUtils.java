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
    private static  boolean verbose = true;
           
    
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
     * Returns list of shortest paths to the nodes, that can be reached in the given time.
     * 
     * @param start node
     * @param graph
     * @param maxTraveTime seconds
     * @param tripsUtil
     * @param outFileName
     * @return total number of nodes in all found paths
     */
    public static int pathsNoLongerThan(SimulationNode start, Graph<SimulationNode, SimulationEdge> graph,
        int maxTravelTime, TripsUtil tripsUtil, String outFileName){
        int paths = 0;
        Set<Integer> uniquNodes = new HashSet<>();
        int nodes = 0;
        int maxNumNodes = 0;
        int minNumNodes = Integer.MAX_VALUE;
        double maxDist = maxTravelTime * 13.888;
        long startTime = System.currentTimeMillis();
        for(SimulationNode node: graph.getAllNodes()){
            if(start.id != node.id){
                int[] path = tripsUtil.createTrip(start.id, node.id).getLoacationIds();
                int dist = findExactLength(path, graph);
                if(dist <= maxDist){
                    paths++;
                    nodes += path.length;
                    uniquNodes.addAll(Arrays.stream(path).boxed().collect(Collectors.toSet()));
                    maxNumNodes = maxNumNodes >= path.length ? maxNumNodes : path.length;
                    minNumNodes = minNumNodes <= path.length ? minNumNodes : path.length;
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("Shortest paths reachable in ").append(maxTravelTime).append(" s for node ")
            .append(start.id).append(", ").append((endTime-startTime)/1000).append(" sec \n");
        sb.append("Paths found: ").append(paths).append(", unique nodes: ")
            .append(uniquNodes.size()).append("\n");
        sb.append("Nodes per path: avg ").append(nodes/paths).append(" , min ")
            .append(minNumNodes).append(", max ").append(maxNumNodes).append(" \n");
     
        System.out.println(sb.toString());
        
        return nodes;
    }
    
    public static void pathsNoLongerThan(List<SimulationNode> nodes, Graph<SimulationNode, SimulationEdge> graph,
            int maxTravelTime, TripsUtil tripsUtil, String outFileName){
        
         double sumNodes = 0;
         for(SimulationNode start: nodes){
             sumNodes += pathsNoLongerThan(start, graph, maxTravelTime, tripsUtil, outFileName);
         }
         StringBuilder sb = new StringBuilder();
         sb.append("Average ").append(sumNodes/nodes.size()).append(" nodes per path \n");
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
    
    /**
     * Finds shortest paths from the given node to all other nodes in the graph.
     * Writes result to file.
     * @param start
     * @param graph
     * @param tripsUtil
     * @param outFileName
     * @return  time needed to find all paths
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
    
     public static void edgeDistanceComparison( Graph<SimulationNode, SimulationEdge> graph ){   
            graph.getAllEdges().forEach((edge) -> {
            double lat1 = edge.fromNode.getLatitude();
            double lon1 = edge.fromNode.getLongitude();
            double lat2 = edge.toNode.getLatitude();
            double lon2 = edge.toNode.getLongitude();
            
            double dist0 = DistUtils.getEuclideanDist(lat1, lon1, lat2, lon2);
            double dist2 = DistUtils.getHaversineDist(lat1, lon1, lat2, lon2);
            System.out.println("Euclidean projected: "+dist0 + ", haversine "+dist2+", length "+edge.length);
            });
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
