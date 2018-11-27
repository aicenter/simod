/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
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
	//private final ConfigTaxify config;
	private long callCount = 0;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final double speedMs;
    private final String pathToMatrix;
    private int[][] distMatrix;
    private AStar astar;
    int n;
   
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public TravelTimeProviderTaxify(ConfigTaxify config, TransportNetworks transportNetworks) {
		//this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMs = config.speed;
        n = graph.numberOfNodes();
        pathToMatrix = config.matrixFileName;
        buildMatrix();
	}
    
    
    public final void buildMatrix(){
        try{
            LOGGER.info("Reading matrix from " + pathToMatrix);
            FileInputStream fis = new FileInputStream(pathToMatrix);
            ObjectInputStream iis = new ObjectInputStream(fis);
            distMatrix = (int[][]) iis.readObject();
            LOGGER.info("Distance matrix successfully loaded");
            checkConnectivity();
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with  distance matrix  not found: "+ex);
            //return;
            astar = new AStar(graph);
            distMatrix = new int[n][n];
            computeMatrix(); // takes 4-5 hours for 10000 nodes
        }
 
    }
    
    private void computeMatrix(){
        for(int[] r: distMatrix){
            Arrays.fill(r, -1);
        }
        LOGGER.info("Start building matrix");
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                if(j == i){
                    continue;
                }
                if(distMatrix[i][j] == -1){
                    int[] result = astar.search(i, j);
                    updateMatrix(i, result);
                }
            }
            //LOGGER.info("Astar: "+i+" cycle, time "+ (System.currentTimeMillis() - startTime)); 
        }
        try {
            FileOutputStream fos = new FileOutputStream(pathToMatrix);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(distMatrix);
            LOGGER.info("Distance matrix saved successfully");
        } catch (IOException ex) {
            LOGGER.error("Error saving matrix "+ex);
        }
    }
   
    private void updateMatrix(int nodeId, int[] distArray){
        for(int target = 0; target < n; target++){
            if(distArray[target] != -1){
                distMatrix[nodeId][target] = distArray[target];
            }
        }
    }
    
    private void checkConnectivity(){
        int count = 0;
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                if(j != i && distMatrix[i][j] == -1){
                    LOGGER.error("No path between nodes "+i +" and "+ j);
                    count++;
                }
            }
        }
        if(count == 0){
            LOGGER.info("Graph is connected.");
        }else{
            LOGGER.error(count+" missing paths");
        }
    }

    @Override
    public int getTravelTimeInMillis(Integer startId, Integer targetId) {
        return distToTime(distMatrix[startId][targetId]);
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
        double bestDist = Integer.MAX_VALUE;
        for (Integer sn : startNodes.keySet()) {
            for (Integer en : endNodes.keySet()) {
                int n2n = distMatrix[sn][en];
                double s2n = startNodes.get(sn);
                double e2n = endNodes.get(en);
                double dist = s2n + n2n + e2n;
                bestDist = bestDist <= dist ? bestDist : dist;
            }
        }
        return distToTime(bestDist);
    }
    /**
     * 
     * @param startNodes
     * @param endNodes
     * @return 
     */
    @Override
    public int  getTravelTimeInMillis(int[] startNodes, int[] endNodes) {
        int bestDist = Integer.MAX_VALUE;
        int[] nodes = new int[]{1,1};
        for (int i=0; i < startNodes.length; i+=2){
            for(int j=0; j < endNodes.length; j+=2){
                int n2n = distMatrix[startNodes[i]][endNodes[j]];
                int s2n = startNodes[i+1];
                int e2n = endNodes[j+1];
                int dist = n2n + s2n + e2n;
                if(dist < bestDist){
                    bestDist = dist;
                    nodes[0] = i;
                    nodes[1] = j;
                }
            }
        }
//        if(nodes[0] == 2){
//            swapNodes(startNodes);            
//        }
//        if(nodes[1] == 2){
//            swapNodes(endNodes);
//        }
        return distToTime(bestDist);
    }

    private void swapNodes(int[] nodes){
        int tmpN = nodes[0];
        int tmpD = nodes[1];
        nodes[0] = nodes[2];
        nodes[1] = nodes[3];
        nodes[2] = tmpN;
        nodes[3] = tmpD;
    }
        
    private int distToTime(double dist){
        return (int) Math.round(1000*(dist/speedMs));
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
    
//    
//        private void dist2times(){
//        for(int r=0;r < n;r++){
//            for(int c = 0;c<n;c++){
//                distMatrix[r][c] = (int) Math.round(1000*(distMatrix[r][c]/speedMs)); 
//            }
//        }
//    }

    

}
