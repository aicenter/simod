
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.search.Rtree;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.search.HopcroftKarp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Container for demand data.
 * 
 * @author Olga Kholkovskaia
 * @param <D>

 * 
 */
public class Demand<D> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Demand.class);
    TravelTimeProvider travelTimeProvider;
    AmodsimConfig config;
    Map<Integer, Integer> tripToIndexMap;
    int[] index;
    final int[] startNodes;
    final int[] endNodes;
    final int[] startTimes;
    final int[] bestTimes;
    final int N;
    int lastInd;
    private final Graph<SimulationNode, SimulationEdge> graph;
    Rtree rtree;
    List<D> demand;


    /**
     *
     * @param travelTimeProvider {@link cz.cvut.fel.aic.amodsim.ridesharing.offline.search.TravelTimeProviderTaxify}
     * @param config {@link cz.cvut.fel.aic.amodsim.config.AmodsimConfig}
     * @param demand list of {@link cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTaxify}
     * @param graph {@link cz.cvut.fel.aic.geographtools.Graph}
     */  
    public Demand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, List<D> demand,
        Graph<SimulationNode, SimulationEdge> graph){
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        this.graph = graph;
        LOGGER.debug("size of demand "+demand.size());//", last index "+index.length);
        N = demand.size();
        tripToIndexMap = new HashMap<>();
        index = new int[N];
        startTimes = new int[N];
        bestTimes = new int[N];
        startNodes = new int[N];
        endNodes = new int[N];
        
        lastInd = 0;
        this.demand = demand;

    }
    
    protected void updateIndex(int tripId, int ind){
        tripToIndexMap.put(tripId,  ind);
        index[ind] = tripId;
    }
    
    protected void updateTime(int ind, int startTime, int bestTime){
        startTimes[ind] =  startTime;
        bestTimes[ind] = bestTime;
    }
    
    protected void updateNodes(int ind, int startNodeId, int endNodeId){
        startNodes[ind] =  startNodeId;
        endNodes[ind] = endNodeId;
    }
    
    /**
     *
     * @param ind
     * @return
     */
    public D getTripByIndex(int ind){
        return demand.get(ind);
    }
    /**
     * @return total number of trips in Demand
     */
    public int size(){
        return N;
    }
 
    public int indToTripId(int ind) {
        return index[ind];
    }
    
    /**
     * Converts trip's id from input file to index.
     * @param tripId
     * @return trip's index in Demand.
     */ 
    public int tripIdToInd(int tripId){
        return tripToIndexMap.get(tripId);
    }
    /**
     * @param ind trip's index in Demand.
     * @return trip's start time.
     */
    public int getStartTime(int ind){
        return startTimes[ind];
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's best possible duration.
     */
    public int getBestTime(int ind){
        return bestTimes[ind];
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's end time.
     */
    public int getEndTime(int ind){
        return getStartTime(ind) + getBestTime(ind);
    }
    
    public SimulationNode getNodeById(int id){
//         LOGGER.debug("node id "+id + ", node "+ graph.getNode(id));
        return graph.getNode(id);
    }
    /**
     * @param ind trip's index in Demand.
     * @return trip's start nodes.
     */
    public int getStartNodeId(int ind){
//        LOGGER.debug("ind "+ind + ", start node id "+ startNodes[ind]);
        return startNodes[ind];
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination nodes.
     */
    public int getEndNodeId(int ind){
//        LOGGER.debug("ind "+ind + ", end node id "+ endNodes[ind]);
        return endNodes[ind];
    }
    
       /**
     * @param ind trip's index in Demand.
     * @return trip's start nodes.
     */
    public SimulationNode getStartNode(int ind){
//        LOGGER.debug("ind "+ind + ", start node "+ getStartNodeId(ind));
        return getNodeById(getStartNodeId(ind));
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination nodes.
     */
    public SimulationNode getEndNode(int ind){
//         LOGGER.debug("ind "+ind + ", end node "+ getEndNodeId(ind));
        return getNodeById(getEndNodeId(ind));
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's start latitude, projected.
     */
    public double getStartLatitudeProj(int ind){
        return getStartNode(ind).getLatitudeProjected();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's start longitude, projected.
     */
    public double getStartLongitudeProj(int ind){
        return  getStartNode(ind).getLongitudeProjected();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination latitude, projected.
     */
    public double getTargetLatitudeProj(int ind){
        return getEndNode(ind).getLatitudeProjected();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination longitude, projected.
     */
    public double getTargetLongitudeProj(int ind){
        return getEndNode(ind).getLatitudeProjected();
    }
    
    /**
     * @param ind trip's index in Demand.
     * @return trip's coordinates, projected;
     * start_lat, start_lon, target_lat, target_lon
     */
    public double[] getGpsCoordinatesProj(int ind) {
        return new double[] {getStartLatitudeProj(ind),
                            getStartLongitudeProj(ind),
                            getTargetLatitudeProj(ind),
                            getTargetLongitudeProj(ind)};
    }
    /**
     * @param ind trip's index in Demand.
     * @return trip's start latitude.
     */
    public double getStartLatitude(int ind){
        return getStartNode(ind).getLatitude();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's start longitude.
     */
    public double getStartLongitude(int ind){
        return  getStartNode(ind).getLongitude();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination latitude.
     */
    public double getTargetLatitude(int ind){
        return getEndNode(ind).getLatitude();
    }

    /**
     * @param ind trip's index in Demand.
     * @return trip's destination longitude.
     */
    public double getTargetLongitude(int ind){
        return getEndNode(ind).getLongitude();
    }
    /**
     * @param ind trip's index in Demand.
     * @return trip's coordinates, unprojected.
     */
    public double[] getGpsCoordinates(int ind) {
//        LOGGER.debug("trip "+ind);
        return new double[] {getStartLatitude(ind),
                             getStartLongitude(ind),
                             getTargetLatitude(ind),
                             getTargetLongitude(ind)};
    }

//-----------------------------------

    /**
     * Finds map cover for graph there each node is complete trip.
     * It's a bipartite graph with one subset made by end nodes of the trip, and another by start nodes.
     * Edges are build from the first to the last with two constraints:
     *  arrival to the next start not later than max waiting time
     *  maximum travel distance is sigma.
     * @param sigma time in milliseconds, limit for driving time between the end of one trip and beginning of the next.
     * @return 
     */
    protected int[] findMapCover(int sigma){
        HopcroftKarp hp = new HopcroftKarp(N);
        int[] pair_u = hp.findMapCover(buildAdjacency(sigma));
        return pair_u;
    }
   
    private int[][] buildAdjacency(int sigma) {
        int maxWaitTime = config.ridesharing.maxProlongationInSeconds * 1000;
        LOGGER.debug("sigma in millis " + sigma);
        LOGGER.debug("timeLine length: " + startTimes.length);
        LOGGER.debug("N = " + N);
//        for(int t:startTimes){ System.out.println(t);}
        int[][] adjacency = new int[N][];
        
        for (int tripInd = 0; tripInd < N; tripInd++) {
            
            List<Integer> neighbors = new ArrayList<>();
          //  LOGGER.debug("NEW TRIP. #"+ tripInd + "; start " + getStartTime(tripInd) + "; end " + getEndTime(tripInd));
            int timeLimit = getEndTime(tripInd) + sigma;
          //  LOGGER.debug("timeLimit = " + timeLimit);
            int lastTripInd = getIndexByStartTime(timeLimit);
        //    LOGGER.debug("  returned index = " + lastTripInd+", starts at " + getStartTime(lastTripInd));
            for (int nextTripInd = lastTripInd + 1; nextTripInd < N; nextTripInd++) {
               // LOGGER.debug("nextTrip = " + indToTripId(nextTripInd) + "; start " + getStartTime(nextTripInd));
                if(getStartTime(nextTripInd) <= getEndTime(tripInd)){
                 //   LOGGER.error("Failed.Next trip starts before the previous ends" + indToTripId(tripInd));
                    continue;
                }
                int timeToNextTrip = (int) travelTimeProvider.getExpectedTravelTime(
                    getEndNode(tripInd), getStartNode(nextTripInd));
              //  int travelTime = bestTravelTimeMs + config.timeBuffer;
                int earliestPossibleArrival = getEndTime(tripInd) + timeToNextTrip;
               // LOGGER.debug("  Added.Travel time = " + timeToNextTrip +"; start "+getStartTime(nextTripInd));
                if (earliestPossibleArrival <= getStartTime(nextTripInd) + maxWaitTime) {
                 //   LOGGER.debug("  Added.Travel time = " + timeToNextTrip +"; start "+getStartTime(nextTripInd));
                   // LOGGER.debug("Ok. Arrival to next start at "+ earliestPossibleArrival +"\n latest start at "+startTimes[nextTripInd]);
                    neighbors.add(nextTripInd);
                }
            }
            //TODO limit for neighbors
            adjacency[tripInd] = neighbors.stream().mapToInt(Integer::intValue).sorted().limit(50).toArray();
        }
        double avg = Arrays.stream(adjacency).map((int[] ns) -> ns.length).mapToInt(Integer::intValue).sum() / N;
        LOGGER.debug("AVERAGE  edges per node " + avg);
        return adjacency;
    }
    
    /**
     * Find index of 1st demand item 
     * starting at given time or later.
     * 
     * @param time  milliseconds from start
     * @return 
     */
    public int getIndexByStartTime(int time){
        
        int ind = Arrays.binarySearch(startTimes, time);
//        LOGGER.debug("Bsearch " + time + " returned "+ ind);
        ind = ind >= 0 ? ind : -(ind + 1);
        ind =  ind < N ? ind : N - 1 ;
//        LOGGER.debug(" final index " + ind);
        return ind;
    }
    
    
    /**
     * Returns slice of array with demand ids between two positions.
     *
     * @param startInd 
     * @param endInd
     * @return 
     */    
    public int[] getTripsSlice(int startInd, int endInd){
        if(startInd >= N || startInd >= endInd){
            return new int[]{};
        }
        endInd = endInd < N ? endInd : N-1;
        return Arrays.copyOfRange(index, startInd, endInd);
    }
    
 }

