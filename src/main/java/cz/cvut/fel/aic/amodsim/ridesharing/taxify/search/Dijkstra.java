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
public class Dijkstra {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Dijkstra.class); 
    private final Graph<SimulationNode,SimulationEdge> graph;
    private final Map<Integer, Integer> gScore;
    private final Map<Integer, Integer> bestParent;
    private OpenList openList;
    private int callCount;
    private int numOfNodes;
    
    private int[][] hMatrix;
    
    /**
     * @param graph
     */
    public Dijkstra(Graph<SimulationNode,SimulationEdge> graph) {
        this.graph = graph;
        gScore = new HashMap<>();
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
     * 
     * BFS with minimum heap.
     * 
     * @param origin SimulationNode
     * @param destination SimulationNode
     * @return 
     */
    public int[] search(int origin) {

        HashSet<Integer> closedSet = new HashSet<>();

        initMaps(graph.getAllNodes());
        int currentNodeId = origin;
        gScore.put(currentNodeId, 0);
        openList = new OpenList(numOfNodes, gScore);
        openList.add(currentNodeId);

        while (!openList.isEmpty()) {
            currentNodeId = openList.pop();
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

                if (newResult >= currentResult){
                    continue; // our current time is better
                }

                gScore.put(nextNodeId, newResult);
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
            gScore.put(node.getId(),  Integer.MAX_VALUE);
            bestParent.put(node.getId(), -1);
        }
    }
    
}
