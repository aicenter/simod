/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class Demand {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Demand.class);
   private static final int SRID = 32633;
    int maxRideDistanceSquared;
    TravelTimeProvider travelTimeProvider;
    AmodsimConfig config;
    private  int[] index;
    private  int[] revIndex;
    private  int[][] startNodes;
    private int[][] endNodes;
    private  int[] startTimes;
    private  int[] bestTimes;
    private final double[] values;
    private final double[][] coordinates;
    private final int N;
    private int lastInd;
    private String startTime = "2018-09-09 00:00:00.00000000";
    private Rtree rtree;
    private Graph<SimulationNode, SimulationEdge> graph;
    
    
    public Demand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, List<TimeTripWithValue<GPSLocation>> demand,
        Graph<SimulationNode, SimulationEdge> graph){
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        maxRideDistanceSquared = 25000*25000;
        index  = new int[demand.get(demand.size()-1).id+1];
        LOGGER.debug("size of demand "+demand.size()+", last index "+demand.get(demand.size()-1).id);

        List<TimeTripWithValue<GPSLocation>> filteredDemand = demand.stream()
            .filter(trip->travelTimeProvider.getTravelTimeInMillis(trip) < 1800000).collect(Collectors.toList());
        N = filteredDemand.size();
        LOGGER.debug("filtered demand size "+N);
        revIndex = new int[N];
        startTimes = new int[N];
        bestTimes = new int[N];
        startNodes = new int[N][];
        endNodes = new int[N][];
        values = new double[N];
        coordinates = new double[N][4];
        lastInd = 0;
        this.graph = graph;
        prepareDemand(filteredDemand);
        
    }
    public Demand(TravelTimeProvider travelTimeProvider, AmodsimConfig config,
        Graph<SimulationNode, SimulationEdge> graph, String filename) throws ParseException{

            this.travelTimeProvider = travelTimeProvider;
            this.config = config;
            this.graph = graph;
            maxRideDistanceSquared = 25000*25000;
            //index  = new int[demand.get(demand.size()-1).id+1];
            List<Object[]> rawDemand = readCsv(filename);
            N = rawDemand.size();
            LOGGER.debug("size of demand, N "+rawDemand.size()+", last index "+index.length);
            revIndex = new int[N];
            startTimes = new int[N];
            bestTimes = new int[N];
            startNodes = new int[N][];
            endNodes = new int[N][];
            values = new double[N];
            coordinates = new double[N][4];
            lastInd = 0;

            prepareDemandCsv(rawDemand);
    }
    
    public int id2ind(int id){
        return index[id];
    }
    
    public int ind2id(int ind){
        return revIndex[ind];
    }
    
    public int getStartTime(int ind){
        return startTimes[ind];
    }
    public int getBestTime(int ind){
        return bestTimes[ind];
    }
    public int getEndTime(int ind){
        return getStartTime(ind) + getBestTime(ind);
    }
    public int[] getStartNodes(int ind){
        return startNodes[ind];
    }
    public int[] getEndNodes(int ind){
        return endNodes[ind];
    }
    public double getStartLatitude(int ind){
        return coordinates[ind][0];
    }
    public double getStartLongitude(int ind){
        return coordinates[ind][1];
    }
    public double getTargetLatitude(int ind){
        return coordinates[ind][2];
    }
    public double getTargetLongitude(int ind){
        return coordinates[ind][3];
    }
    
    public double getRideValue(int ind){
        return values[ind];
    }
        
    public int[][] convertPathsToIds(int[][] paths){
        //int[][] coordinates = new int[paths.length][];
        for(int[] path: paths){
            for(int i = 1; i < path.length - 1; i++){
                path[i] = ind2id(path[i]);
            }
        }
        return paths;
    }
    
    private   List<Object[]> readCsv(String filename) throws ParseException{
        int count = -1;
        int pickupRadius = 50;
      
        rtree = new Rtree(graph.getAllNodes(), graph.getAllEdges());
                                                     // 2022-03-01 00:01:57.515870999
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date startDate = format.parse(startTime);
        Long zeroMillis = startDate.getTime();
        //filereader = new FileReader(filename);
        List<Object[]> rawDemand = new ArrayList<>();
        try{
            FileInputStream fis = new FileInputStream(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String buffer = br.readLine();
            while((buffer = br.readLine()) != null ){
                count++;
                System.out.println(buffer);
                String[] str = buffer.split(",");
                System.out.println(Arrays.toString(str));
                System.out.println(str.length);
                //double[][] trip = new double[3][];
                //trip[0] = new double[5];
                int id = count;
                //trip[0][0] = id;   
                int time = 0;
                try {
                    Date date   = format.parse (str[0]);
                    time = (int) (date.getTime() - zeroMillis + 500);
                    //trip[0][1] = date.getTime() - zeroMillis; // time
                } catch (ParseException ex) {
                   
                    LOGGER.error("Date parse error "+ex);
                     continue;
                }
                double startLat =  Double.parseDouble(str[1]); // star lat
                double startLon =  Double.parseDouble(str[2]); // start lon
                double endLat =  Double.parseDouble(str[3]);  // end lat 
                double endLon =  Double.parseDouble(str[4]);  // end lon
                double value = Double.parseDouble(str[5]);    // value
                                
                GPSLocation startLocation
                    = GPSLocationTools.createGPSLocation(startLat, startLon, 0, SRID);
                GPSLocation targetLocation
                 = GPSLocationTools.createGPSLocation(endLat, endLon, 0, SRID);
                
                double x = startLocation.getLongitudeProjected() - targetLocation.getLongitudeProjected();
                double y = startLocation.getLatitudeProjected() - targetLocation.getLatitudeProjected();
                if((x*x + y*y) > maxRideDistanceSquared){
                    continue;
                }
                Object[] result = rtree.findNode(startLocation, pickupRadius);
                int startNodes[];
                int endNodes[];
                if (result == null){
                    //startTooFarCount++;
                   // unmappedTrips[0].add(gpsTrip.id);
                    continue;
                }else if(result.length == 1){
                    startNodes = new int[]{(int) result[0], 0};
                }else{
                    startNodes = new int[]{(int) result[0], (int) Math.round((double) result[2])+1,
                                           (int) result[1], (int) Math.round((double)result[3])+1};
                }
                result = rtree.findNode(targetLocation, pickupRadius);
                if (result == null){
                    //targetTooFarCount++;
                   // unmappedTrips[1].add(gpsTrip.id);
                      continue;
                }else if(result.length == 1){
                    endNodes = new int[]{(int) result[0], 0};
                }else{
                     endNodes = new int[]{(int) result[0], (int) Math.round((double) result[2])+1,
                                          (int) result[1], (int) Math.round((double)result[3])+1};
                }
                int bestTravelTime = travelTimeProvider.getTravelTimeInMillis(startNodes, endNodes);
                if(bestTravelTime > 1800000){
                    continue;
                }
                rawDemand.add(new Object[]{id, time, startNodes, endNodes, bestTravelTime,
                                            startLat, startLon, endLat, endLon,value});
            }
        }catch (IOException ex){
            LOGGER.error("Date parse exception "+ex);
        }
        index = new int[count+1];
        rtree = null;
        return rawDemand;
    }
    
    private void prepareDemandCsv(List<Object[]> rawDemand) throws ParseException{
        //0i    1i     2i[]       3i[]       4i             5d        6d        7d      8d     9d
        //id, time, startNodes, endNodes, bestTravelTime, startLat, startLon, endLat, endLon,value
        for(int ind = 0; ind < N; ind++){
            Object[] trip = rawDemand.get(ind);
            int tripId = (int) trip[0];
            index[tripId] = ind;
            revIndex[ind] = tripId;
            startTimes[ind] = (int) trip[1];
            bestTimes[ind] = (int) trip[4];
            //LOGGER.debug(ind+" best time="+bestTime);
            values[ind] = (double) trip[9];
            startNodes[ind] = (int[]) trip[2];
            endNodes[ind] = (int[]) trip[3];
            coordinates[ind] = new double[]{(double) trip[5], (double) trip[6], 
                                            (double) trip[7], (double) trip[8]};

        }
        
      
    }
    
    
    private void prepareDemand(List<TimeTripWithValue<GPSLocation>> demand) {
        int count = 0;
        for (TimeTripWithValue<GPSLocation> trip : demand) {
            int bestTime = travelTimeProvider.getTravelTimeInMillis(trip);
//            if (bestTime > 1800000) {
//                LOGGER.error("Not filterd too long trip "+bestTime);
//            } else {
                addTripToIndex(trip, bestTime);
 //           }
        }
    }
    
    
    // helpers for prepareDemand
    private void addTripToIndex(TimeTripWithValue<GPSLocation> trip, int bestTime){
        int ind = lastInd;
        index[trip.id] = ind;
        revIndex[ind] = trip.id;
        startTimes[ind] = (int) trip.getStartTime();
        bestTimes[ind] = bestTime;
        //LOGGER.debug(ind+" best time="+bestTime);
        values[ind] = trip.getRideValue();
        Map<Integer,Double> nodeMap = (Map<Integer,Double>)  trip.nodes.get(0);
        addNodesToIndex(nodeMap, startNodes, ind);
        nodeMap = (Map<Integer,Double>)  trip.nodes.get(1);
        addNodesToIndex(nodeMap, endNodes, ind);
        addCoordinatesToIndex(trip.getLocations(), ind);
        lastInd++;
    }
    
    private void addCoordinatesToIndex(List<GPSLocation> nodes, int ind){
        GPSLocation start = nodes.get(0);
        GPSLocation end = nodes.get(nodes.size()-1);
        coordinates[ind][0] = start.getLatitude();
        coordinates[ind][1] = start.getLongitude();
        coordinates[ind][2] = end.getLatitude();
        coordinates[ind][3] = end.getLongitude();
    }
    
    private void addNodesToIndex(Map<Integer,Double> nodeToDistMap, int[][] nodeList, int ind){
        int n = nodeToDistMap.size();
        if (n == 0){
            LOGGER.error("No nodes assigned");
            return;
        }else if( n > 2){
            LOGGER.error("More than two nodes assigned");
            return;
        }
        nodeList[ind] = new int[n*2];
        int i = 0;
        for(Integer nodeId : nodeToDistMap.keySet()){
            nodeList[ind][i] = nodeId;
            nodeList[ind][i+1] = (int) (Math.round(1000*(nodeToDistMap.get(nodeId)/13.88)));
            i+=2;
        }
    }
//-----------------------------------
    public int[] findMapCover(int sigma){
        HopcroftKarp hp = new HopcroftKarp(N);
        int[] pair_u = hp.findMapCover(buildAdjacency(sigma));
        return pair_u;
    }
   
    private int[][] buildAdjacency(int sigma) {
        //long maxWaitTimeMs = config.amodsim.ridesharing.maxWaitTime * 1000;
        //double speed = config.amodsim.ridesharing.maxSpeedEstimation;
        int sigmaMs = sigma * 60000;
        LOGGER.debug("sigma in millis " + sigmaMs);
        LOGGER.debug("timeLine length: " + startTimes.length);
        int[][] adjacency = new int[N][];
        for (int tripInd = 0; tripInd < N; tripInd++) {
            List<Integer> neighbors = new ArrayList<>();
            //LOGGER.debug("trip = "+ind2id(tripInd) +"; start "+getStartTime(tripInd)+"; end "+getEndTime(tripInd));
            int timeLimit = getEndTime(tripInd) + sigmaMs;
            //LOGGER.debug("timeLimit = "+timeLimit);
            int lastTripInd = Arrays.binarySearch(startTimes, timeLimit);
            
            lastTripInd = lastTripInd >= 0 ? lastTripInd : -(lastTripInd + 1);
            //LOGGER.debug("returned index = "+lastTripInd+", starts at "+getStartTime(lastTripInd));
            //LOGGER.debug("last index = "+lastId);
            for (int nextTripInd = tripInd + 1; nextTripInd < lastTripInd; nextTripInd++) {
                //LOGGER.debug("  nextTrip = "+trip.id +"; start "+trip.getStartTime());
                if(getStartTime(nextTripInd) <= getEndTime(tripInd)){
                    //LOGGER.error("Next trip starts before the previous ends"+ind2id(tripInd)+" "+ind2id(nextTripInd));
                    continue;
                }
                int bestTravelTimeMs = travelTimeProvider.getTravelTimeInMillis(endNodes[tripInd], startNodes[nextTripInd]);
                int travelTime = bestTimes[tripInd] + bestTravelTimeMs;
                //long latestValidArrival = nextTrip.getStartTime() + maxWaitTimeMs;
                //if(earliestPossibleArrival <= latestValidArrival){
                if (getEndTime(tripInd)+travelTime <= startTimes[nextTripInd]) {
//                    if(nextTripInd - tripInd > Short.MAX_VALUE){
//                        System.out.println("Index difference exceeds short max value");
//                    }
                    //LOGGER.info("adding new edge");
                    neighbors.add(nextTripInd);
                }
            }
            adjacency[tripInd] = neighbors.stream().mapToInt(Integer::intValue).sorted().toArray();
        }
        double avg = Arrays.stream(adjacency).map((int[] ns) -> ns.length).mapToInt(Integer::intValue).sum() / N;
        LOGGER.debug("average edges per node " + avg);
        return adjacency;
    }
  
}




//    private int[] indexList2idArray(List<Integer> indList){
//         return indList.stream().map(ind->ind2id(ind)).mapToInt(Integer::intValue).toArray();
//    }
//public int[][] reverseAdjacency(int[][] adjacency){
//        int[] nodes = Arrays.stream(adjacency).flatMapToInt(sublist->Arrays.stream(sublist)).distinct().toArray();
//        LOGGER.debug("end nodes size "+nodes.length);
//        int[][] rAdjacency = new int[adjacency.length][];
//        for(int endNode : nodes){
//            LOGGER.debug("end node "+endNode);
//            List<Integer> neighbors = new ArrayList<>();
//            for(int startNode = 0; startNode < endNode; startNode++){
//                if(Arrays.binarySearch(adjacency[startNode], endNode) >= 0){
//                    neighbors.add(startNode);
//                }
//            }
//            rAdjacency[endNode] = neighbors.stream().mapToInt(Integer::intValue).sorted().toArray();
//        }
//        double avg = Arrays.stream(rAdjacency).map(ns->ns.length).mapToInt(Integer::intValue).sum()/adjacency.length;
//        LOGGER.debug("reverse: average edges per node "+avg);
//        return rAdjacency;
//    }

    
//    public int[][] buildEdges(int sigma){
//        List<int[]> edges = new ArrayList<>();
//        double speed = config.amodsim.ridesharing.maxSpeedEstimation;
//        //    double sigmaDistance = (double) sigma * 60* speed;
//        int sigmaMs = sigma * 60 * 1000;
//        LOGGER.debug("sigma in millis " + sigmaMs);
//        LOGGER.debug("timeLine length: " + startTimes.length);
//        int count = 0;
//        for (int tripInd = 0; tripInd < N; tripInd++) {
//            //LOGGER.debug("trip = "+trip.id +"; start "+trip.getStartTime()+"; end "+trip.getEndTime());
//            long timeLimit = endTimes[tripInd] + sigmaMs;
//            //LOGGER.debug("timeLimit = "+timeLimit);
//            int lastTripInd = Arrays.binarySearch(startTimes, timeLimit);
//            //LOGGER.debug("returned index = "+lastId);
//            lastTripInd = lastTripInd >= 0 ? lastTripInd : -(lastTripInd + 1);
//            //LOGGER.debug("last index = "+lastId);
//            for (int nextTripInd = tripInd + 1; nextTripInd < lastTripInd; nextTripInd++) {
//                //LOGGER.debug("  nextTrip = "+trip.id +"; start "+trip.getStartTime());
//                double bestDist = travelTimeProvider.computeBestLength(endNodes[tripInd], startNodes[nextTripInd]);
//                double bestTravelTimeMs = (bestDist / speed) * 1000;
//                long earliestPossibleArrival = (long) (endTimes[tripInd] + bestTravelTimeMs);
//                //long latestValidArrival = nextTrip.getStartTime() + maxWaitTimeMs;
//                //if(earliestPossibleArrival <= latestValidArrival){
//                if (earliestPossibleArrival <= startTimes[nextTripInd]) {
//                    edges.add(new int[]{tripInd, nextTripInd});
//                    count++;
//                }
//            }
//               //LOGGER.debug("edges so far  = "+count/(tripInd+1));
//        }
//        return edges.stream().mapToInt(Integer::intValue).toArray();
//    }