package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    
    public enum Direction{FORWARDS,BACKWARDS};
    private enum EndAfterFinding{ALL,ONE,EXHAUSTED};
    
    private static final int DEFAULT_SET_SIZE = 4096;
    
    /**
     * Convenience method for a one-to-one search on an uncontracted graph.
     */
    public static DijkstraSolution dijkstrasAlgorithm(CHNode startNode, CHNode endNode, Direction direction) {
        Preconditions.checkNoneNull(startNode,endNode,direction);
        HashSet<CHNode> hs = new HashSet<>(1);
        hs.add(endNode);
        List<DijkstraSolution> solutions = dijkstrasAlgorithm(startNode, hs, Integer.MAX_VALUE, direction);
        if (solutions.size() == 1)
            return solutions.get(0);
        else
            return null;
    }
	
	// Called for the two halves of a contracted dijkstra.
    public static List<DijkstraSolution> dijkstrasAlgorithm(CHNode startNode, Direction direction) {
        Preconditions.checkNoneNull(startNode, direction);
        return dijkstrasAlgorithm(startNode, null, Integer.MAX_VALUE, direction);
    }
    
    /**
     * dijkstrasAlgorithm performs a best-first graph search starting at startNode
     * and continuing until all endNodes have been reached, or until the best
     * solution has a drive time greater than maxSearchTime, whichever happens
     * first.
     */
    static List<DijkstraSolution> dijkstrasAlgorithm(CHNode startNode, HashSet<CHNode> endNodes, 
			int maxSearchCost, Direction direction) {
		EndAfterFinding endCondition = EndAfterFinding.ALL;
		
        Preconditions.checkNoneNull(startNode, direction);
        Preconditions.require(startNode != null);
        HashMap<CHNode,NodeInfo> nodeInfo = new HashMap<>(DEFAULT_SET_SIZE);
        ArrayList<DijkstraSolution> solutions = new ArrayList<>(DEFAULT_SET_SIZE);

        PriorityQueue<DistanceOrder> unvisitedNodes = new PriorityQueue<>();

		DistanceOrder startDo = new DistanceOrder(0,startNode);
		unvisitedNodes.add(startDo);

		NodeInfo startNodeInfo = new NodeInfo();
		startNodeInfo.minCost = 0;
		startNodeInfo.distanceOrder = startDo;
		nodeInfo.put(startNode, startNodeInfo);
        
        
        while (!unvisitedNodes.isEmpty()) {
            // Find the node with the shortest cost so far:
            DistanceOrder minHeapEntry = unvisitedNodes.poll();
            CHNode shortestTimeNode = minHeapEntry.node;
            NodeInfo thisNodeInfo = nodeInfo.get(shortestTimeNode);
            
            if (thisNodeInfo.minCost > maxSearchCost)
                break;

            if (endNodes == null) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
            } else if (endNodes.contains(shortestTimeNode)) {
                solutions.add(extractShortest(shortestTimeNode, nodeInfo));
                if (solutions.size() == endNodes.size() || endCondition==EndAfterFinding.ONE)
                    return solutions;
            }
            
            thisNodeInfo.visited = true;
            thisNodeInfo.distanceOrder = null;

            for (CHEdge edge : (direction == Direction.FORWARDS ? shortestTimeNode.outEdges : shortestTimeNode.inEdges)) {
                CHNode n = (direction == Direction.FORWARDS ? edge.to : edge.from);
                if (n.contractionOrder < shortestTimeNode.contractionOrder)
                    break;
                
                NodeInfo neighborNodeInfo = nodeInfo.get(n);
                if (neighborNodeInfo == null) {
                    neighborNodeInfo = new NodeInfo();
                    nodeInfo.put(n, neighborNodeInfo);
                }
                
                if (neighborNodeInfo.visited)
                    continue;
                
                int newTime = thisNodeInfo.minCost + edge.cost;
                int previousTime = neighborNodeInfo.minCost;
                
                if (newTime < previousTime) {
                    neighborNodeInfo.minCost = newTime;
                    neighborNodeInfo.minCostFrom = shortestTimeNode;
                    neighborNodeInfo.minCostVia = edge;
                    
                    if (neighborNodeInfo.distanceOrder != null) {
                        unvisitedNodes.remove(neighborNodeInfo.distanceOrder);
                    }
                    DistanceOrder newDistOrder = new DistanceOrder(newTime, n);
                    neighborNodeInfo.distanceOrder = newDistOrder;
                    unvisitedNodes.add(newDistOrder);
                }
            }
        }
        
        return solutions;
    }

    private static final class DistanceOrder implements Comparable<DistanceOrder> {
        private final int minCost;
        public final CHNode node;

        public DistanceOrder(int minDriveTime, CHNode node) {
            this.minCost = minDriveTime;
            this.node = node;
        }
        
        @Override
        public int compareTo(DistanceOrder that) {
            if (this.minCost < that.minCost) {
                return -1;
            } else if (this.minCost > that.minCost) {
                return 1;
            } else {
                return Long.compare(this.node.node.id,that.node.node.id);
            }
        }
    }
        
    
    private static final class NodeInfo {
        boolean visited = false;
        int minCost = Integer.MAX_VALUE;
        CHNode minCostFrom = null;
        CHEdge minCostVia = null;
        DistanceOrder distanceOrder = null;
        DijkstraSolution solution = null;
    }
    
    private static DijkstraSolution extractShortest(final CHNode endNode, HashMap<CHNode,NodeInfo> nodeInfo) {
        NodeInfo endNodeInfo = nodeInfo.get(endNode);
        int totalDriveTime = endNodeInfo.minCost;
        
        List<CHNode> nodes = new LinkedList();
        List<CHEdge> edges = new LinkedList();
        
        CHNode thisNode = endNode;
        while (thisNode != null) {
            NodeInfo thisNodeInfo = nodeInfo.get(thisNode);
            if (thisNodeInfo.solution == null) {
                nodes.add(0, thisNode);
                if (thisNodeInfo.minCostVia != null)
                    edges.add(0,thisNodeInfo.minCostVia);
                thisNode = thisNodeInfo.minCostFrom;
            } else {
                endNodeInfo.solution = new DijkstraSolution(totalDriveTime, nodes, edges, thisNodeInfo.solution);
                return endNodeInfo.solution;
            }
        }
        
        if (nodes.isEmpty()) {
            System.out.println("Created empty solution?!?!");
        }
        
        endNodeInfo.solution = new DijkstraSolution(totalDriveTime, nodes, edges);
        return endNodeInfo.solution;
    }
}
