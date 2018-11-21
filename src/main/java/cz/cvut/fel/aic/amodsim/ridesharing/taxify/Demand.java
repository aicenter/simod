
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
  * @author olga
  * 
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
    private final int N;
    private int lastInd;
    private final String startTime;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final int timeBuffer;
    
    private  double[] values;
    private  double[][] coordinates;
    private  double[][] gpsCoordinates;
   
    
    double[][] projStart;
    double[][] vectors;
    Rtree rtree;
    int[] nodeToCluster;
    int[][] clusterStartNodes;
    int[][] clusterEndNodes;
    int[][] clusterTimes;
    int NC;
    
    
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
        projStart = new double[N][2];
        vectors =  new double[N][2];
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
        return values[ind];
    }
    public int getTimeBuffer() {
        return timeBuffer;
    }
    
    private void prepareDemand(List<TripTaxify<GPSLocation>> demand) {
        for (TripTaxify<GPSLocation> trip : demand) {
            int bestTime = travelTimeProvider.getTravelTimeInMillis(trip);
                addTripToIndex(trip, bestTime);
        }
        //rtree = new Rtree(projStart); // ??create new rtree for each period
        //LOGGER.debug("Rtree size " + rtree.size());
    }

    // helpers for prepareDemand
    private void addTripToIndex(TripTaxify<GPSLocation> trip, int bestTime){
        int ind = lastInd;
        index[trip.id] = ind;
        revIndex[ind] = trip.id;
        startTimes[ind] = (int) trip.getStartTime() + timeBuffer;
        bestTimes[ind] = bestTime;
        gpsCoordinates[ind] = trip.getGpsCoordinates();
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
        
        //save start coordinates and vectors for rtree
        projStart[ind][0] = start.getLatitudeProjected();
        projStart[ind][1] = start.getLongitudeProjected();
        double norm = Math.sqrt(Math.pow(end.getLatitudeProjected() - start.getLatitudeProjected(), 2) +
                                Math.pow(end.getLongitudeProjected() - start.getLongitudeProjected(), 2));
        vectors[ind][1] = (end.getLatitudeProjected() - start.getLatitudeProjected())/norm;
        vectors[ind][0] = (end.getLongitudeProjected() - start.getLongitudeProjected())/norm;
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
        int maxWaitTime = config.amodsim.ridesharing.maxWaitTime * 300;
        int sigmaMs = sigma * 60000;
        LOGGER.debug("sigma in millis " + sigmaMs);
        LOGGER.debug("timeLine length: " + startTimes.length);
        int[][] adjacency = new int[N][];
        int C = 0;
        
        for (int tripInd = 0; tripInd < N; tripInd++) {
            List<Integer> neighbors = new ArrayList<>();
           // LOGGER.debug("trip = "+ind2id(tripInd) +"; start "+getStartTime(tripInd)+"; end "+getEndTime(tripInd));
            int timeLimit = getEndTime(tripInd) + sigmaMs;
           // LOGGER.debug("timeLimit = "+timeLimit);
            int lastTripInd = getIndexByTime(timeLimit);
           // LOGGER.debug("returned index = "+lastTripInd+", starts at "+getStartTime(lastTripInd));
            for (int nextTripInd = tripInd + 1; nextTripInd < lastTripInd; nextTripInd++) {
              //  LOGGER.debug("  nextTrip = "+ind2id(nextTripInd) +"; start "+getStartTime(nextTripInd));
                if(getStartTime(nextTripInd) < getEndTime(tripInd)){
                   // LOGGER.error("Next trip starts before the previous ends"+ind2id(tripInd)+" "+ind2id(nextTripInd));
                    continue;
                }
                int bestTravelTimeMs = travelTimeProvider.getTravelTimeInMillis(getEndNodes(tripInd), getStartNodes(nextTripInd));
                int travelTime = bestTimes[tripInd] + bestTravelTimeMs + timeBuffer;
               // LOGGER.debug("  travel time = "+travelTime +"; start "+getStartTime(nextTripInd));
                if (getEndTime(tripInd) + travelTime <= getStartTime(nextTripInd) + maxWaitTime) {
                   // LOGGER.debug("  prev end +tt "+ (getEndTime(tripInd)+travelTime)+" next start "+startTimes[nextTripInd]);
                    neighbors.add(nextTripInd);
                    C++;
                }
            }
            adjacency[tripInd] = neighbors.stream().mapToInt(Integer::intValue).sorted().toArray();
           // System.out.println("Processed="+tripInd+", total="+C);
        }
        LOGGER.debug("Done");
        double avg = Arrays.stream(adjacency).map((int[] ns) -> ns.length).mapToInt(Integer::intValue).sum() / N;
        LOGGER.debug("average edges per node " + avg);
        return adjacency;
    }

    // group nodes for ride-sharing
    public void cluster(){
        initClusterArrays();
        
        int EOT = 48*60*60*1000;
        int INC = 10*60*1000; // 10 min
        int start = 0;
        int firstInd = 0;
        
        while(start < EOT){
            int endTime = start + INC;
           // LOGGER.debug("timeLimit = "+timeLimit);
            int lim = getIndexByTime(endTime);
            Map<Integer, List<Integer>> clusters = new HashMap<>();
            for(int tripInd = firstInd; tripInd < lim; tripInd++){
                if(nodeToCluster[tripInd] != -1){
                    continue;
                }
                for(Integer clusterInd: clusters.keySet()){
                    List<Integer> cluster = clusters.get(clusterInd);
                    if(addIndToCluster(tripInd, cluster, clusterInd)){
                        nodeToCluster[tripInd] = clusterInd;
                        
                        break;
                    }
                }
                List<Integer> newCluster = new ArrayList<>();
                newCluster.add(tripInd);
                clusters.put(NC, newCluster);
                clusterStartNodes[NC] = getStartNodes(tripInd);
                clusterEndNodes[NC] = getEndNodes(tripInd);
                clusterTimes[NC][0] = startTimes[tripInd];
                clusterTimes[NC][1] = getEndTime(tripInd);
                NC++;
            }
            start = endTime;
       
            }
      
    }
    
    //TODO 
    private boolean addIndToCluster(int nextTripInd, List<Integer> cluster, int clusterInd){
        if(cluster.size() == 4){ // it already has 4 passengers
            return false;
        }
       
        int tripInd = cluster.get(cluster.size()-1);
        if(getEndTime(tripInd) > getStartTime(nextTripInd)){
               return false; //last trip ends later than the inserted one starts
        }
        int bestTravelTime = travelTimeProvider.getTravelTimeInMillis(getStartNodes(tripInd),
                                                                      getStartNodes(nextTripInd)); 
        if (getEndTime(tripInd) + bestTravelTime > getStartTime(nextTripInd)){ //+maxWaitTime) {
            return false; // not enough time to get to the inserted node start
        }
        if(!isFeasible(cluster, nextTripInd, bestTravelTime, clusterInd)){
            return false; 
        }
        cluster.add(nextTripInd);
        clusterEndNodes[clusterInd] = getEndNodes(nextTripInd);
        return true;
    }
    
    
    private boolean isFeasible(List<Integer> cluster, int nextTripInd, int travelTime, int clusterInd){
        // pickup insertion
        //travelTime is time from last pickup in the cluster to pickup we're trying to add
        int firstTripInd = cluster.get(0);
        int maxTripDuration = config.amodsim.ridesharing.maxRideTime*60*1000;
        // next, we need time from inserted pickup to the first dropoff
        int newPickup2FirstDropoff = travelTimeProvider.getTravelTimeInMillis(getStartNodes(nextTripInd),
                                                                              getEndNodes(firstTripInd));
        int timeIncrease = travelTime + newPickup2FirstDropoff;
        for(Integer nodeInd : cluster){
            if(getEndTime(nodeInd)+timeIncrease > getStartTime(nodeInd) + maxTripDuration){
                return false;
            }
        }
        int lastTripInd = cluster.get(cluster.size()-1);
        int lastDropoff2Inserted = travelTimeProvider.getTravelTimeInMillis(getEndNodes(lastTripInd),
                                                                            getEndNodes(nextTripInd));
        //time from inserted node pick up to drop off;
        int insertedDropoffTime = getEndTime(lastTripInd)+timeIncrease + lastDropoff2Inserted;
        if(getStartTime(nextTripInd) + maxTripDuration > insertedDropoffTime){
            return false;
        }
     
        clusterTimes[clusterInd][1] = insertedDropoffTime;
        //TODO update start and best time for all trips in the cluster
        
        return true;
    }
    
    
    private void initClusterArrays(){
        NC = 0;
        nodeToCluster = new int[N];
        for(int i = 0; i< N; i++){
            nodeToCluster[i] = -1;
        }
        clusterStartNodes = new int[1000][];
        clusterEndNodes = new int[1000][];
        clusterTimes = new int[1000][2];
    }
    
    
    private int getIndexByTime(int time){
        int ind = Arrays.binarySearch(startTimes, time);
        ind = ind >= 0 ? ind : -(ind + 1);
        ind =  ind <= N ? ind : N;
        return ind;
    }
    
    // save and load unsessarry data 
    // (gps coordinates of demand and pickup-dropoff points, and values are not used in solution process)
    // they can be saved after demand is initialized, and the loaded to write results.
    public void dumpData(){
        dumpArray(coordinates, "coordinates");
        dumpArray(gpsCoordinates, "gps_coordinates");
        dumpArray(values, "values");
        values = null;
        coordinates = null;
        gpsCoordinates = null;
        
        LOGGER.debug("Coordinates and values dumped");
    }
    
    private void dumpArray(double[][] arr, String filename){
          try {
            FileOutputStream fos = new FileOutputStream(config.amodsimDataDir + "/"+filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(arr);
        } catch (IOException ex) {
            LOGGER.error("File with   not found: "+ex);
        }
    }
    
    private void dumpArray(double[] arr, String filename){
          try {
            FileOutputStream fos = new FileOutputStream(config.amodsimDataDir +"/"+ filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(arr);
        } catch (IOException ex) {
           LOGGER.error("File with   not found: "+ex);
        }
    }
    
    public void loadData(){
        coordinates = loadArray2d("coordinates");
        gpsCoordinates = loadArray2d("gps_coordinates");
        values = loadArray1d("values");
        LOGGER.debug("Coordinates and values loaded");
        
    }
    
    private double[][] loadArray2d(String filename){
        double[][] arr = null;
         try{
            FileInputStream fis = new FileInputStream(config.amodsimDataDir+"/"+ filename);
            ObjectInputStream iis = new ObjectInputStream(fis);
            arr = (double[][]) iis.readObject();
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with   not found: "+ex);
        }
        return arr;
    }
    private double[] loadArray1d(String filename){
        double[]arr = null;
        try{
            FileInputStream fis = new FileInputStream(config.amodsimDataDir+"/"+ filename);
            ObjectInputStream iis = new ObjectInputStream(fis);
            arr = (double[]) iis.readObject();
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with   not found: "+ex);
        }
       return arr;
    }
 }

