
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.Rtree;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.HopcroftKarp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
//import static cz.cvut.fel.aic.amodsim.ridesharing.taxify.Utils.cosine;
/**
  * @author olga
  * 
 */
public abstract class Demand<D> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Demand.class);
    TravelTimeProvider travelTimeProvider;
    ConfigTaxify config;
    final  int[] revIndex;
    final  int[][] startNodes;
    final int[][] endNodes;
    final  int[] startTimes;
    final  int[] bestTimes;
    private final int N;
    int lastInd;
    //private final String startTime;
    private final Graph<SimulationNode, SimulationEdge> graph;
    //private final int timeBuffer;
    
    double[] values;
    private double[][] coordinates;
    double[][] gpsCoordinates;
//    double[][] projStart;
//    double[][] vectors;
    Rtree rtree;

   
    public Demand(TravelTimeProvider travelTimeProvider, ConfigTaxify config, List<D> demand,
        Graph<SimulationNode, SimulationEdge> graph){
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        this.graph = graph;
       
        N = demand.size();
        revIndex = new int[N];
        startTimes = new int[N];
        bestTimes = new int[N];
        startNodes = new int[N][];
        endNodes = new int[N][];
        values = new double[N];
        coordinates = new double[N][4];
        gpsCoordinates = new double[N][4];
       
        //projStart = new double[N][2];
//        vectors =  new double[N][2];
        lastInd = 0;
        prepareDemand(demand);
    }
    public int getTime(int ind1, int ind2){
        int[] starts = ind1 < N ? getStartNodes(ind1) : getEndNodes(ind1 - N);
        int[] ends = ind2 < N ? getStartNodes(ind2) : getEndNodes(ind2 - N);
        int time = travelTimeProvider.getTravelTimeInMillis(starts, ends);
        return time;
    }
    public int size(){
        return N;
    }
//    public double[] getProjStart(int ind) {
//        return projStart[ind];
//    }
//    public double[] getVector(int ind) {
//        return vectors[ind];
//    }
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
    public double getValue(int ind){
        return values[ind];
    }
    
    /**
     * compares value earned by two cars, descending
     * 
     * @param car1 list of trips served by the car
     * @param car2
     * @return 
     */
    public int compareByValue(int[] car1, int[] car2){
        int ret = getValue(car1) <= getValue(car2) ? 1 : -1;
        return ret;
    }
    /**
     * Returns total values for trips in array.
     * 
     * @param trips array of trip indices
     * @return 
     */
    public double getValue(int[] trips){
        return Arrays.stream(trips).mapToDouble(ind->getValue(ind)).sum();
    }
    
    abstract void prepareDemand(List<D> demand);
    
    void addCoordinatesToIndex(List<GPSLocation> nodes, int ind){
        GPSLocation start = nodes.get(0);
        GPSLocation end = nodes.get(nodes.size()-1);
        coordinates[ind][0] = start.getLatitude();
        coordinates[ind][1] = start.getLongitude();
        coordinates[ind][2] = end.getLatitude();
        coordinates[ind][3] = end.getLongitude();
    }
    
    void addNodesToIndex(Map<Integer,Double> nodeToDistMap, int[][] nodeList, int ind){
        double speed = config.speed;
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
            nodeList[ind][i+1] = (int) (Math.round(1000*(nodeToDistMap.get(nodeId)/speed)));
            i+=2;
        }
    }
//-----------------------------------

    /**
     * Finds map cover for graph there each node is complete trip.
     * It's a bipartite graph with one subset made by end nodes of the trip, and another by start nodes.
     * Edges are build from the first to the last with two constraints:
     *  arrival to the next start not later than max waiting time
     *  maximum travel distance is sigma.
     * @param sigma time in millis, limit for driving time between the end of one trip and beginning of the next.
     * @return
     */
    protected int[] findMapCover(int sigma){
        HopcroftKarp hp = new HopcroftKarp(N);
        int[] pair_u = hp.findMapCover(buildAdjacency(sigma));
        return pair_u;
    }
   
    private int[][] buildAdjacency(int sigma) {
        int maxWaitTime = (int) (config.maxWaitTime * 0.3);
        LOGGER.debug("sigma in millis " + sigma);
        LOGGER.debug("timeLine length: " + startTimes.length);
        int[][] adjacency = new int[N][];
        
        for (int tripInd = 0; tripInd < N; tripInd++) {
            List<Integer> neighbors = new ArrayList<>();
           // LOGGER.debug("trip = "+ind2id(tripInd) +"; start "+getStartTime(tripInd)+"; end "+getEndTime(tripInd));
            int timeLimit = getEndTime(tripInd) + sigma;
           // LOGGER.debug("timeLimit = "+timeLimit);
            int lastTripInd = getIndexByStartTime(timeLimit);
           // LOGGER.debug("returned index = "+lastTripInd+", starts at "+getStartTime(lastTripInd));
            for (int nextTripInd = tripInd + 1; nextTripInd < lastTripInd; nextTripInd++) {
              //  LOGGER.debug("  nextTrip = "+ind2id(nextTripInd) +"; start "+getStartTime(nextTripInd));
                if(getStartTime(nextTripInd) < getEndTime(tripInd)){
                   // LOGGER.error("Next trip starts before the previous ends"+ind2id(tripInd)+" "+ind2id(nextTripInd));
                    continue;
                }
                int bestTravelTimeMs = travelTimeProvider.getTravelTimeInMillis(getEndNodes(tripInd), getStartNodes(nextTripInd));
                int travelTime = bestTimes[tripInd] + bestTravelTimeMs + config.timeBuffer;
               // LOGGER.debug("  travel time = "+travelTime +"; start "+getStartTime(nextTripInd));
                if (getEndTime(tripInd) + travelTime <= getStartTime(nextTripInd) + maxWaitTime) {
                   // LOGGER.debug("  prev end +tt "+ (getEndTime(tripInd)+travelTime)+" next start "+startTimes[nextTripInd]);
                    neighbors.add(nextTripInd);
                }
            }
            adjacency[tripInd] = neighbors.stream().mapToInt(Integer::intValue).sorted().limit(50).toArray();
           // System.out.println("Processed="+tripInd+", total="+C);
        }
        double avg = Arrays.stream(adjacency).map((int[] ns) -> ns.length).mapToInt(Integer::intValue).sum() / N;
        LOGGER.debug("average edges per node " + avg);
        return adjacency;
    }
    
     
    protected int getIndexByStartTime(int time){
        int ind = Arrays.binarySearch(startTimes, time);
        ind = ind >= 0 ? ind : -(ind + 1);
        ind =  ind <= N ? ind : N;
        return ind;
    }
    
    protected int getIndexByEndTime(int[] endTimes, int time){
        int ind = Arrays.binarySearch(endTimes, time);
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
            FileOutputStream fos = new FileOutputStream(config.dir + filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(arr);
        } catch (IOException ex) {
            LOGGER.error("File with   not found: "+ex);
        }
    }
    
    private void dumpArray(double[] arr, String filename){
          try {
            FileOutputStream fos = new FileOutputStream(config.dir + filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(arr);
        } catch (IOException ex) {
           LOGGER.error("File with not found: "+ex);
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
            FileInputStream fis = new FileInputStream(config.dir + filename);
            ObjectInputStream iis = new ObjectInputStream(fis);
            arr = (double[][]) iis.readObject();
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with not found: "+ ex);
        }
        return arr;
    }
    private double[] loadArray1d(String filename){
        double[]arr = null;
        try{
            FileInputStream fis = new FileInputStream(config.dir + filename);
            ObjectInputStream iis = new ObjectInputStream(fis);
            arr = (double[]) iis.readObject();
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with not found: "+ ex);
        }
       return arr;
    }
 }

