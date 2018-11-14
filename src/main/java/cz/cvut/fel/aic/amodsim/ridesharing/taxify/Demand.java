/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class Demand {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Demand.class);
    int maxRideDistanceSquared;
    TravelTimeProvider travelTimeProvider;
    AmodsimConfig config;
    private final  int[] index;
    private final  int[] revIndex;
    private final  int[][] startNodes;
    private final int[][] endNodes;
    private final  int[] startTimes;
    private final  int[] bestTimes;
    private final double[] values;
    private final double[][] coordinates;
    private final double[][] gpsCoordinates;
    private final int N;
    private int lastInd;
    private final String startTime;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final int timeBuffer;
    
    
    public Demand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, List<TripTaxify<GPSLocation>> demand,
        Graph<SimulationNode, SimulationEdge> graph){
        this.startTime = "2022-03-01 00:00:00.000";
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        maxRideDistanceSquared = 25000*25000;
        index  = new int[demand.get(demand.size()-1).id+1];
        LOGGER.debug("size of demand "+demand.size()+", last index "+demand.get(demand.size()-1).id);

//        List<TripTaxify<GPSLocation>> filteredDemand = demand.stream()
//            .filter(trip->travelTimeProvider.getTravelTimeInMillis(trip) < 1802000).collect(Collectors.toList());
        N = demand.size();
        LOGGER.debug("filtered demand size "+N);
        revIndex = new int[N];
        startTimes = new int[N];
        bestTimes = new int[N];
        startNodes = new int[N][];
        endNodes = new int[N][];
        values = new double[N];
        coordinates = new double[N][4];
        gpsCoordinates = new double[N][4];
        lastInd = 0;
        timeBuffer = 2*1000;
        this.graph = graph;
        prepareDemand(demand);
        
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
    public double[] getGpsCoordinates(int ind) {
        return gpsCoordinates[ind];
    }
    
    public double getRideValue(int ind){
         if(ind2id(ind) == 494){
             System.out.println("getter "+values[ind]);
         }
        return values[ind];
    }
    
    private void prepareDemand(List<TripTaxify<GPSLocation>> demand) {
        for (TripTaxify<GPSLocation> trip : demand) {
            int bestTime = travelTimeProvider.getTravelTimeInMillis(trip);
                addTripToIndex(trip, bestTime);
        }
    }
    
    // helpers for prepareDemand
    private void addTripToIndex(TripTaxify<GPSLocation> trip, int bestTime){
        if(trip.id == 494){
            System.out.println(trip.id+"; "+trip.getRideValue()+", ind"+lastInd);
        }
        int ind = lastInd;
        index[trip.id] = ind;
        revIndex[ind] = trip.id;
        startTimes[ind] = (int) trip.getStartTime() + timeBuffer;
        bestTimes[ind] = bestTime;
        gpsCoordinates[ind] = trip.getGpsCoordinates();
        
        //LOGGER.debug(ind+Arrays.toString(gpsCoordinates[ind]));
        values[ind] = trip.getRideValue();
         if(trip.id == 494){
            System.out.println(trip.id+" values[ind] = "+values[ind]);
        }
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
            nodeList[ind][i+1] = (int) (Math.round(1000*(nodeToDistMap.get(nodeId)/13.88))); //TODO get speed from config
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
        int maxWaitTime = config.amodsim.ridesharing.maxWaitTime * 800;
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
                if (getEndTime(tripInd)+travelTime <= startTimes[nextTripInd]+maxWaitTime) {
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