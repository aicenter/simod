
package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import cz.cvut.fel.aic.amodsim.ridesharing.ch.PartialSolution.DownwardSolution;
import cz.cvut.fel.aic.amodsim.ridesharing.ch.PartialSolution.UpwardSolution;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ContractedDijkstra {
    
    public static DijkstraSolution contractedGraphDijkstra(List<CHNode> allNodes, Map<Integer, CHNode> idsTochNodes,
			CHNode startNode, CHNode endNode) {
        Preconditions.checkNoneNull(allNodes, startNode, endNode);
        UpwardSolution upwardSolution = calculateUpwardSolution(startNode);
        DownwardSolution downwardSolution = calculateDownwardSolution(endNode);
        return mergeUpwardAndDownwardSolutions(idsTochNodes, upwardSolution, downwardSolution);
    }
    
    public static DijkstraSolution mergeUpwardAndDownwardSolutions(Map<Integer, CHNode> idsTochNodes, UpwardSolution up, 
			DownwardSolution down) {
        
        IntBuffer commonIndices = getCommonEntryIndices(up.getContractionOrderBuffer(),
				down.getContractionOrderBuffer(), up.getTotalDriveTimeBuffer(), down.getTotalDriveTimeBuffer());
        if (commonIndices.get(0) == -1)
            return null;
        
        int shortestUpIdx = commonIndices.get(0);
        int shortestDownIdx = commonIndices.get(1);
        
        DijkstraSolution shortestSolutionUp = up.getDijkstraSolution(idsTochNodes, shortestUpIdx);
        DijkstraSolution shortestSolutionDown = down.getDijkstraSolution(idsTochNodes, shortestDownIdx);
        return unContract(upThenDown(shortestSolutionUp, shortestSolutionDown));
    }
    
    /*
        10 repetitions cached pathing from hatfield to 4000 locations in 1479 ms.
        getCommonEntryIndicesCalls:   88013
        whileLoopIterations:      324083997
        matchedContractionOrders: 152378966
        replacedShortest:           1848114

        3682 loops per subroutine call
        Shared entries: 47.01%
        21 shortest replacements per subroutine call

        16.8 us per subroutine call
        4.5 ns per loop iteration -> average 18 clock cycles at 4GHz
     */
    private static IntBuffer getCommonEntryIndices(IntBuffer a, IntBuffer b, IntBuffer aTimes, IntBuffer bTimes) {
        IntBuffer result = IntBuffer.allocate(2);
        result.put(0,-1).put(1,-1);
        
        int shortestTime = Integer.MAX_VALUE;
        int aIdx = 0;
        int bIdx = 0;
        
        while (aIdx<a.limit() && bIdx<b.limit()) {
            int aValue = a.get(aIdx);
            int bValue = b.get(bIdx);
            
            if (aValue==bValue) {
                int aTime = aTimes.get(aIdx);
                int bTime = bTimes.get(bIdx);
                if (aTime+bTime < shortestTime) {
                    shortestTime = aTime+bTime;
                    result.put(0, aIdx);
                    result.put(1, bIdx);
                }
                
                bIdx++;
                aIdx++;
            } else if (aValue > bValue) {
                bIdx++;
            } else {
                aIdx++;
            }
        }
        
        return result;
    }
    

    private static DijkstraSolution upThenDown(DijkstraSolution up, DijkstraSolution down) {
        int totalDriveTime = up.totalCost + down.totalCost;
        LinkedList<CHNode> nodes = new LinkedList();
        nodes.addAll(up.nodes);
        for (int i = down.nodes.size() - 1; i >= 0; i--) {
            nodes.add(down.nodes.get(i));
        }
        LinkedList<CHEdge> edges = new LinkedList();
        edges.addAll(up.edges);
        for (int i = down.edges.size() - 1; i >= 0; i--) {
            edges.add(down.edges.get(i));
        }
        return new DijkstraSolution(totalDriveTime, nodes, edges);
    }
    
    /**
     * Take in a solution with some shortcut edges / contracted nodes and 
     * convert to the equivalent non-contracted solution.
     */
    private static DijkstraSolution unContract(DijkstraSolution ds) {
        if (ds == null) {
            return null;
        }
        int totalCost = ds.totalCost;
        List<CHEdge> edges = Collections.EMPTY_LIST;
        for (CHEdge edge : ds.edges) {
            edges = new UnionList<>(edges ,edge.getUncontractedEdges());
        }
        
        List<CHNode> nodes;
        if (edges.isEmpty()) {
            nodes = Collections.singletonList(ds.getFirstNode());
        } else {
            nodes = new NodeListFromEdgeList(edges);
        }
        return new DijkstraSolution(totalCost, nodes, edges);
    }
    
    public static UpwardSolution calculateUpwardSolution(CHNode startNode) {
        List<DijkstraSolution> upwardSolutions = Dijkstra.dijkstrasAlgorithm(startNode, Dijkstra.Direction.FORWARDS);
        return new UpwardSolution(upwardSolutions);
    }
    
    public static DownwardSolution calculateDownwardSolution(CHNode endNode) {
        List<DijkstraSolution> downwardSolutions = Dijkstra.dijkstrasAlgorithm(endNode, Dijkstra.Direction.BACKWARDS);
        return new DownwardSolution(downwardSolutions);
    }

}
