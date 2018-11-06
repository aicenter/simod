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
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.astar.AStar;
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
    private final double speedMillis;
    private final String pathToMatrix;
    private int[][] distMatrix;
    private final AStar astar;
    int n;
   
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public TravelTimeProviderTaxify(AmodsimConfig config, TransportNetworks transportNetworks) {
		this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMillis = config.amodsim.ridesharing.maxSpeedEstimation*1000;
        n = graph.numberOfNodes();
        astar = new AStar(graph);
        distMatrix = new int[n][n];
        pathToMatrix = config.amodsimDataDir+"/tallin_test.bin";

        buildMatrix();
	}
    
    private void buildMatrix(){
        LOGGER.info("Start building matrix");
        try{
            LOGGER.info("Reading matrix from "+pathToMatrix);
            FileInputStream fis = new FileInputStream(pathToMatrix);
            ObjectInputStream iis = new ObjectInputStream(fis);
            distMatrix = (int[][]) iis.readObject();
//            for(int[] row: distMatrix){
//                System.out.println(Arrays.toString(row));
//            }
            LOGGER.info("Distance matrix successfully loaded");
            return;
        }catch(IOException | ClassNotFoundException ex){
            LOGGER.error("File with  distances not found: "+ex);
        }
        for(int[] r: distMatrix){
            for(int i = 0; i <n; i++){
                r[i] = -1;
            }
        }
        
        for(int i = 0; i < n; i++){
            long startTime = System.currentTimeMillis();
            for(int j = 0; j<n; j++){
                if(j == i){
                    continue;
                }
                if(distMatrix[i][j] == -1){
                    int[] result = astar.search(i, j);
                    updateMatrix(i, result);
                }
            }
            LOGGER.info("Astar: "+i+" cycle, time "+ (System.currentTimeMillis() - startTime)); 
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


    @Override
    public double getTravelTime(Integer startId, Integer targetId) {
       // callCount++;
        //LOGGER.info(" "+callCount);
        if(distMatrix[startId][targetId] != -1){
            return distMatrix[startId][targetId];
        }
        int[] result = astar.search(startId, targetId);
        //System.out.println(Arrays.toString(result));
        updateMatrix(startId, result);
        //System.out.println(Arrays.toString(distMatrix[startId]));
        return distMatrix[startId][targetId];
    }
    
    private void updateMatrix(int nodeId, int[] distArray){
        int count = 0;
        for(int target = 0; target < distArray.length; target++){
            if(distArray[target] != -1){
                count++;
                distMatrix[nodeId][target] = distArray[target];
            }
        }
        //LOGGER.info("Updates count "+count);
        
    }


    @Override
    //?? time in millis?
    public double computeBestLength(TimeTripWithValue<GPSLocation> trip) {
        Map<Integer, Double> startNodes = trip.nodes.get(0);
        Map<Integer, Double> endNodes = trip.nodes.get(1);
        double bestLength = Double.MAX_VALUE;
        for (Integer sn : startNodes.keySet()) {
            for (Integer en : endNodes.keySet()) {
                double n2n = getTravelTime(sn, en);
                double s2n = startNodes.get(sn);
                double e2n = endNodes.get(en);
                double pathLength = s2n + n2n + e2n;
                bestLength = bestLength <= pathLength ? bestLength : pathLength;
            }
        }
        return bestLength;
    }

    @Override
    public int  getTravelTimeInMillis(int[] startNodes, int[] endNodes) {
        double bestLength = Double.MAX_VALUE;
        int[] nodes = new int[]{1,1};
        for (int i=0; i<startNodes.length; i+=2){
            for(int j=0; j<endNodes.length; j+=2){
                int startNode = startNodes[i];
                int endNode = endNodes[j];
                double n2n = getTravelTime(startNode, endNode);
                int s2n = startNodes[i+1];
                int e2n = endNodes[j+1];
                double pathLength = n2n + s2n + e2n;
                if(pathLength < bestLength){
                    bestLength = pathLength;
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
        return (int) Math.round(bestLength/speedMillis);
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


//    @Override
//    public double getTravelTime(Integer startId, Integer targetId) {
//        SimulationNode startNode = graph.getNode(startId);
//        SimulationNode targetNode = graph.getNode(targetId);
//        double x = targetNode.getLongitudeProjected() - startNode.getLongitudeProjected();
//        double y = targetNode.getLatitudeProjected() - startNode.getLatitudeProjected();
//        return Math.sqrt(x*x + y*y);
//    }

//    @Override
//    public double computeBestLength(SimulationNode start, TimeTripWithValue<GPSLocation> target) {
//        Map<Integer, Double> endNodes = target.nodes.get(0);
//        double bestLength = Double.MAX_VALUE;
//        for (Integer en : endNodes.keySet()) {
//            double n2n = getTravelTimeInMillis(start.id, en);
//            double e2n = endNodes.get(en);
//            double pathLength = n2n + e2n;
//            bestLength = bestLength <= pathLength ? bestLength : pathLength;
//        }
//        return bestLength;
//    }

//    @Override
//    public double computeBestLength(TimeTripWithValue<GPSLocation> start, SimulationNode target) {
//        Map<Integer, Double> startNodes = start.nodes.get(1);
//        double bestLength = Double.MAX_VALUE;
//        for (Integer sn : startNodes.keySet()) {
//            double n2n = getTravelTimeInMillis(start.id, sn);
//            double s2n = startNodes.get(sn);
//            double pathLength = n2n + s2n;
//            bestLength = bestLength <= pathLength ? bestLength : pathLength;
//        }
//        return bestLength;
//    }

//    @Override
//    public double computeBestLength(SimulationNode start, SimulationNode target) {
//        return getTravelTimeInMillis(start.id, target.id);
//    }


//    @Override
//    public double computeBestLength(TimeTripWithValue<GPSLocation> start, TimeTripWithValue<GPSLocation> target) {
//        Map<Integer, Double> startNodes = start.nodes.get(1);
//        Map<Integer, Double> endNodes = target.nodes.get(0);
//        double bestLength = Double.MAX_VALUE;
//        for (Integer sn : startNodes.keySet()) {
//            for (Integer en : endNodes.keySet()) {
//                double n2n = getTravelTimeInMillis(sn, en);
//                double s2n = startNodes.get(sn);
//                double e2n = endNodes.get(en);
//                double pathLength = s2n + n2n + e2n;
//                bestLength = bestLength <= pathLength ? bestLength : pathLength;
//            }
//        }
//        return bestLength;
//    }