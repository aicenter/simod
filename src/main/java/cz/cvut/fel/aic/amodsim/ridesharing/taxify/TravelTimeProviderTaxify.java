/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.AStar;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.LoggerFactory;


@Singleton
public class TravelTimeProviderTaxify implements TravelTimeProvider{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TravelTimeProviderTaxify.class); 
	private final AmodsimConfig config;
	private long callCount = 0;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final double speedMs;
    private final String pathToMatrix;
    private int[][] timeMatrix;
    private final AStar astar;
    int n;
   
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public TravelTimeProviderTaxify(AmodsimConfig config, TransportNetworks transportNetworks) {
		this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMs = config.amodsim.ridesharing.maxSpeedEstimation;
        n = graph.numberOfNodes();
        astar = new AStar(graph);
        timeMatrix = new int[n][n];
        pathToMatrix = config.amodsimDataDir+"/tallin_times.bin";

        buildMatrix();
	}
    
    private void buildMatrix(){
        
        try{
            LOGGER.info("Reading matrix from "+pathToMatrix);
            FileInputStream fis = new FileInputStream(pathToMatrix);
            ObjectInputStream iis = new ObjectInputStream(fis);
            timeMatrix = (int[][]) iis.readObject();
//            for(int[] row: timeMatrix){
//                System.out.println(Arrays.toString(row));
//           }
            LOGGER.info("Distance matrix successfully loaded");
            return;
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with  times  not found: "+ex);
            LOGGER.error("Trying to read distance matrix. "+ex);
            try{
                FileInputStream fis = new FileInputStream(config.amodsimDataDir+"/tallin_10000.bin");
                ObjectInputStream iis = new ObjectInputStream(fis);
                timeMatrix = (int[][]) iis.readObject();
                dist2times();
                FileOutputStream fos = new FileOutputStream(pathToMatrix);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(timeMatrix);
                LOGGER.info("Time matrix saved successfully");
                return;
            }catch(IOException | ClassNotFoundException ex2){
                LOGGER.error("File with   not found: "+ex2);
            }
        }
        for(int[] r: timeMatrix){
            for(int i = 0; i <n; i++){
                r[i] = -1;
            }
        }
        LOGGER.info("Start building matrix");
        for(int i = 0; i < n; i++){
            long startTime = System.currentTimeMillis();
            for(int j = 0; j<n; j++){
                if(j == i){
                    continue;
                }
                if(timeMatrix[i][j] == -1){
                    int[] result = astar.search(i, j);
                    updateMatrix(i, result);
                }
            }
            LOGGER.info("Astar: "+i+" cycle, time "+ (System.currentTimeMillis() - startTime)); 
        }
        try {
            FileOutputStream fos = new FileOutputStream(pathToMatrix);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(timeMatrix);
            LOGGER.info("Distance matrix saved successfully");
        } catch (IOException ex) {
            LOGGER.error("Error saving matrix "+ex);
        }
    }
    
    private void dist2times(){
        for(int r=0;r<n;r++){
            for(int c = 0;c<n;c++){
                timeMatrix[r][c] = (int) Math.round(1000*(timeMatrix[r][c]/speedMs)); 
            }
        }
    }

    
    private void updateMatrix(int nodeId, int[] distArray){
        for(int target = 0; target < distArray.length; target++){
            if(distArray[target] != -1){
                timeMatrix[nodeId][target] = distArray[target];
            }
        }
    }

    @Override
    public int getTravelTimeInMillis(Integer startId, Integer targetId) {
        return timeMatrix[startId][targetId];
    }

    /**
     * Returns best possible travel time for the trip in milliseconds.
     * The total trip consists of three parts: from first location to one of the assigned nodes, from last location to one of the
     * assigned nodes, and trip itself. Trip instance has data about distance btw location and nodes in meters(!).
     * Value for shortest path from time matrix is already in milliseconds.
     * @param trip
     * @return 
     */
    @Override
    public int getTravelTimeInMillis(TripTaxify<GPSLocation> trip) {
        Map<Integer, Double> startNodes = trip.nodes.get(0);
        Map<Integer, Double> endNodes = trip.nodes.get(1);
        int bestTime = Integer.MAX_VALUE;
        for (Integer sn : startNodes.keySet()) {
            for (Integer en : endNodes.keySet()) {
                int n2n = timeMatrix[sn][en];
                int s2n = (int) Math.round(1000*(startNodes.get(sn)/speedMs));
                int e2n = (int) Math.round(1000*(endNodes.get(en)/speedMs));
                int pathLength = s2n + n2n + e2n;
                bestTime = bestTime <= pathLength ? bestTime : pathLength;
            }
        }
        return bestTime+2000;
    }
    /**
     * 
     * @param startNodes
     * @param endNodes
     * @return 
     */
    @Override
    public int  getTravelTimeInMillis(int[] startNodes, int[] endNodes) {
        int bestTime = Integer.MAX_VALUE;
        int[] nodes = new int[]{1,1};
        for (int i=0; i<startNodes.length; i+=2){
            for(int j=0; j<endNodes.length; j+=2){
                int sn = startNodes[i];
                int en = endNodes[j];
                int n2n = timeMatrix[sn][en];
                int s2n = startNodes[i+1];
                int e2n = endNodes[j+1];
                int time = n2n + s2n + e2n;
                if(time < bestTime){
                    bestTime = time;
                    nodes[0] = i;
                    nodes[1] = j;
                }
            }
        }
        if(nodes[0] == 2){
            swapNodes(startNodes);            
        }
        if(nodes[1] == 2){
            swapNodes(endNodes);
        }
        return bestTime+2000;
    }
    
    private void swapNodes(int[] nodes){
        int tmpN = nodes[0];
        int tmpD = nodes[1];
        nodes[0] = nodes[2];
        nodes[1] = nodes[3];
        nodes[2] = tmpN;
        nodes[3] = tmpD;
    }

	// unused interface methods
	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        throw new UnsupportedOperationException("Not supported yet.");
	}
    
    @Override
	public double getTravelTime(SimulationNode positionA, SimulationNode positionB) {
        throw new UnsupportedOperationException("Not supported yet.");
	}

}
