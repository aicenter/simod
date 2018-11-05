/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Map;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class TravelTimeProviderTaxify implements TravelTimeProvider{
	private final PositionUtil positionUtil;
	private final AmodsimConfig config;
    //private final double travelSpeedEstimatePerSecond;
	private long callCount = 0;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final double speedMillis;
    
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public TravelTimeProviderTaxify(PositionUtil positionUtil, AmodsimConfig config, TransportNetworks transportNetworks) {
		this.positionUtil = positionUtil;
		this.config = config;
		//travelSpeedEstimatePerSecond = config.amodsim.ridesharing.maxSpeedEstimation / 3.6;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMillis = config.amodsim.ridesharing.maxSpeedEstimation*1000;
	}
	
	
	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
//		callCount++;
//		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
//		long traveltime = MoveUtil.computeDuration(travelSpeedEstimatePerSecond, distance);
        
		//return traveltime;
        throw new UnsupportedOperationException("Not supported yet.");
	}
    
    @Override
	public double getTravelTime(SimulationNode positionA, SimulationNode positionB) {
//		callCount++;
//		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
//		long traveltime = MoveUtil.computeDuration(travelSpeedEstimatePerSecond, distance);
//		return traveltime;
        throw new UnsupportedOperationException("Not supported yet.");
	}

    @Override
    public double getTravelTime(Integer startId, Integer targetId) {
        SimulationNode startNode = graph.getNode(startId);
        SimulationNode targetNode = graph.getNode(targetId);
        double x = targetNode.getLongitudeProjected() - startNode.getLongitudeProjected();
        double y = targetNode.getLatitudeProjected() - startNode.getLatitudeProjected();
        return Math.sqrt(x*x + y*y);
    }

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

    @Override
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


}
