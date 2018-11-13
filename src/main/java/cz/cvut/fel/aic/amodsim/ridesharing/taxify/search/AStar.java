/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class AStar {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AStar.class); 
    private final Graph<SimulationNode,SimulationEdge> graph;
    private final Map<Integer, Integer> gScore;
    private final Map<Integer, Integer> predictedScore;
    private final Map<Integer, Integer> bestParent;
    private OpenList openList;
    private int callCount;
    private int numOfNodes;
    
    private int[][] hMatrix;
    
    /**
     * @param graph
     */
    public AStar(Graph<SimulationNode,SimulationEdge> graph) {
        this.graph = graph;
        gScore = new HashMap<>();
        predictedScore = new HashMap<>();
        bestParent = new HashMap<>();
        callCount = 0;
        numOfNodes = graph.numberOfNodes();
        hMatrix = new int[numOfNodes][numOfNodes];
        for(int[] r:hMatrix){
            for(int i=0; i<numOfNodes;i++){
                r[i] = -1;
            }
        }
    }
  
    /**
     * A* search
     * Search for the path with the shortest distance.
     * BFS with minimum heap.
     * Heuristic function - euclidean distance calculated from projected coordinates
     * 
     * @param origin SimulationNode
     * @param destination SimulationNode
     * @return 
     */
    public int[] search(int origin, int destination) {
        //long startTime = System.currentTimeMillis();
        //callCount++;
        //LOGGER.info(" "+callCount);
        HashSet<Integer> closedSet = new HashSet<>();

        initMaps(graph.getAllNodes());
        int currentNodeId = origin;
        gScore.put(currentNodeId, 0);
        predictedScore.put(currentNodeId, calculateHScore(currentNodeId, destination));
        openList = new OpenList(numOfNodes, predictedScore);
        openList.add(currentNodeId);

        while (!openList.isEmpty()) {
            currentNodeId = openList.pop();
            //SimulationNode currentNode = graph.getNodedeId(currentNodeId);
            if(currentNodeId == destination){
                break;
            }
            
            closedSet.add(currentNodeId);
            Collection<SimulationEdge> outEdges = graph.getOutEdges(currentNodeId);
            if (outEdges == null) {
                continue;
            }
            for (SimulationEdge edge : outEdges) {
                int nextNodeId = edge.toNode.id;
                if (closedSet.contains(nextNodeId)){
                    continue; //node is already checked
                }
                int newResult = gScore.get(currentNodeId) + edge.length;
                int currentResult = gScore.get(nextNodeId);

                if (currentResult > -1 && newResult >= currentResult){
                    continue; // our current time is better
                }

                gScore.put(nextNodeId, newResult);
                int hScore = calculateHScore(nextNodeId, destination);
                predictedScore.put(nextNodeId, newResult + hScore);
                bestParent.put(nextNodeId, currentNodeId);
                if (openList.containsNode(nextNodeId)){
                    openList.update(nextNodeId);
                } else{
                    openList.add(nextNodeId);
                }
            }//for
        }//while
        //LOGGER.info("Astar time "+ (System.currentTimeMillis() - startTime));
        return gmapToArray();
    }
    
    private int[] gmapToArray(){
        int[] result = new int[gScore.size()];
        for(Integer nodeId: gScore.keySet()){
            result[nodeId] = gScore.get(nodeId);
        }
        return result;
    }
    
    
    private void initMaps(Collection<SimulationNode> allNodes) {
        for (SimulationNode node : allNodes) {
            gScore.put(node.getId(),  -1);
            bestParent.put(node.getId(), -1);
            predictedScore.put(node.getId(), Integer.MAX_VALUE);
        }
    }
    
    private int calculateHScore(int sourceId, int destId){
        if(hMatrix[sourceId][destId] != -1){
            return hMatrix[sourceId][destId];
        }
        SimulationNode source = graph.getNode(sourceId);
        SimulationNode destination = graph.getNode(destId);
        double x = destination.getLongitudeProjected() - source.getLongitudeProjected();
        double y = destination.getLatitudeProjected() - source.getLatitudeProjected();
        hMatrix[sourceId][destId] = (int) Math.round(Math.sqrt(x*x + y*y));
        return hMatrix[sourceId][destId];
    }
}

