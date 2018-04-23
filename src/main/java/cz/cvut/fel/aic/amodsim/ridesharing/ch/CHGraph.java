package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.ch.Dijkstra.Direction;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author F.I.D.O.
 */
public class CHGraph {
	
	private final Map<Integer, CHNode> idsTochNodes;
	
	private final List<CHNode> chNodes;
	
	private final List<CHEdge> chEdges;
	
	private final BidirectionalTreeMap<Ordering,CHNode> contractionOrder;
	
	private final ExecutorService es;

	
	private long nodePreContractChecks;
	
	private long findShortcutsCalls;
	
	private long nodePreContractChecksPassed;
	
	private long millisSpentOnContractionOrdering;

	

	public List<CHNode> getChNodes() {
		return chNodes;
	}
	
	
	CHGraph(Map<Long, CHNode> readback) {
		idsTochNodes = new HashMap<>();
		chNodes = new LinkedList<>();
		chEdges = new LinkedList<>();
		contractionOrder = new BidirectionalTreeMap<>();
		es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		findShortcutsCalls = 0;
		nodePreContractChecks = 0;
		nodePreContractChecksPassed = 0;
		millisSpentOnContractionOrdering = 0;
		
		for(CHNode node: readback.values()){
			idsTochNodes.put(node.node.id, node);
			chNodes.add(node);
			
			for(CHEdge edge: node.outEdges){
				chEdges.add(edge);
			}
		}
	}
	
	public CHGraph(Graph<SimulationNode,SimulationEdge> sourceGraph) {
		idsTochNodes = new HashMap<>();
		chNodes = new LinkedList<>();
		chEdges = new LinkedList<>();
		contractionOrder = new BidirectionalTreeMap<>();
		es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		findShortcutsCalls = 0;
		nodePreContractChecks = 0;
		nodePreContractChecksPassed = 0;
		millisSpentOnContractionOrdering = 0;
		
		for(Node node: sourceGraph.getAllNodes()){
			CHNode chNode = new CHNode(node);
			idsTochNodes.put(node.id, chNode);
			chNodes.add(chNode);
		}
		
		for(SimulationEdge edge: sourceGraph.getAllEdges()){
			CHNode fromNode = idsTochNodes.get(edge.fromId);
			CHNode toNode = idsTochNodes.get(edge.toId);
			
			CHEdge chEdge = new WraperEdge(edge, fromNode, toNode, (int) (edge.length / edge.allowedMaxSpeedInMpS));
			chEdges.add(chEdge);

			fromNode.addOutEdge(chEdge);
			toNode.addInEdge(chEdge);
		}
	}
	
	
	
	
	public void prepare(){
		initialiseContractionOrder();
        contractAll();
	}
	
	public Trip<SimulationNode> query(Node from, Node to){
		CHNode fromCH = idsTochNodes.get(from.id);
		CHNode toCH = idsTochNodes.get(to.id);
		
		DijkstraSolution computed = ContractedDijkstra.contractedGraphDijkstra(chNodes, idsTochNodes, fromCH, toCH);
		
		return solutionToTrip(computed);
	}
	
	private void initialiseContractionOrder() {
        long orderingStart = System.currentTimeMillis();
        parallelInitContractionOrder();
        long orderingDuration = System.currentTimeMillis()-orderingStart;
        System.out.println("Generated contraction order for " + contractionOrder.size() + 
                " nodes in " + orderingDuration + " ms.");
    }
	
	 private void parallelInitContractionOrder() {
        ArrayList<Callable<KeyValue>> callables = new ArrayList(chNodes.size());
        for (final CHNode node : chNodes) {
            if (!node.isContracted()) {
                callables.add(new Callable<KeyValue>() {
                    public KeyValue call() throws Exception {
                        Ordering key = new Ordering(node, getBalanceOfEdgesRemoved(node));
                        return new KeyValue(key, node);
                    }
                });
            }
        }
            
        try {
            List<Future<KeyValue>> kvs = es.invokeAll(callables);

            contractionOrder.clear();
            for (Future<KeyValue> f : kvs) {
                KeyValue kv = f.get();
                contractionOrder.put(kv.key, kv.value);
            }
            
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
	 
	private void contractAll() {
        int contractionProgress = 1;
        
        long startTime = System.currentTimeMillis();
        long recentTime = System.currentTimeMillis();
        long recentPreContractChecks = 0;
        long recentPreContractChecksPassed = 0;
        long recentOrderingMillis = 0;

        CHNode node;
        while ((node = lazyContractNextNode(contractionProgress++, true)) != null) {
            reorderImmediateNeighbors(node);
            
            //System.out.println("Contracted " + n);
            if (contractionOrder.size() % 10000 == 0) {
                long now = System.currentTimeMillis();
                long runTimeSoFar = now-startTime;
                //long orderingTimeThisRun = millisSpentOnContractionOrdering-recentOrderingMillis;
                //float checksPerPass = (float)(nodePreContractChecks-recentPreContractChecks) / (float)(nodePreContractChecksPassed-recentPreContractChecksPassed);
                System.out.println(runTimeSoFar+"," + contractionOrder.size());
                //        "," + (now-recentTime) + "," + orderingTimeThisRun + 
                //        "," + checksPerPass);
                recentTime=now;
                recentPreContractChecks=nodePreContractChecks;
                recentPreContractChecksPassed=nodePreContractChecksPassed;
                recentOrderingMillis=millisSpentOnContractionOrdering;
            }
        }
        
        for (CHNode sortNode : chNodes) {
            sortNode.sortNeighborLists();
        }
       
        System.out.println("findShortcutsCalls: "+findShortcutsCalls);
        System.out.println("nodePreContractChecks: "+nodePreContractChecks);
        System.out.println("nodePreContractChecksPassed: "+nodePreContractChecksPassed);
        System.out.println("millisSpentOnContractionOrdering: " + millisSpentOnContractionOrdering);
        
        es.shutdown();
    }
	
	private int getBalanceOfEdgesRemoved(CHNode node) {
        int edgesRemoved = getEdgeRemovedCount(node);
        int shortcutsAdded = findShortcuts(node).size();
        return edgesRemoved-shortcutsAdded;
    }
	
	private CHNode lazyContractNextNode(int contractionProgress, boolean includeUnprofitable) {
        Map.Entry<Ordering,CHNode> next = contractionOrder.pollFirstEntry();
        while (next != null) {
            Ordering oldOrder = next.getKey();
            CHNode node = next.getValue();
            
            ArrayList<CHEdge> shortcuts = findShortcuts(node);
            nodePreContractChecks++;
            
            int balanceOfEdgesRemoved = getEdgeRemovedCount(node) - shortcuts.size();
            boolean profitable = balanceOfEdgesRemoved > -30; // OK, so our threshold for 'profitable' has a bit of margin on it.
            
            if (profitable || includeUnprofitable) {
                
                Ordering newOrder = new Ordering(node, balanceOfEdgesRemoved);
                if (contractionOrder.isEmpty() 
                        || newOrder.compareTo(oldOrder) >= 0 
                        || newOrder.compareTo(contractionOrder.lastKey()) >= 0) {
                    // If the ContractionOrdering is unchanged, or has changed but 
                    // not enough to move this node off the top spot, contract.
                    contractNode(node,contractionProgress, shortcuts);
                    nodePreContractChecksPassed++;
                    return node;
                } else {
                    // Otherwise
                    contractionOrder.put(newOrder, node);
                }
                
            } else {
                System.out.println("Contraction became unprofitable with " + contractionOrder.size() 
						+ " nodes remaining.");
                return null;
            }
            
            next = contractionOrder.pollFirstEntry();
        }
        return null;
    }
	
	private ArrayList<CHEdge> findShortcuts(CHNode node) {
//        findShortcutsCalls.incrementAndGet();
        ArrayList<CHEdge> shortcuts = new ArrayList<>();
        
        HashSet<CHNode> destinationNodes = new HashSet<>();
        int maxCost = 0;
        for (CHEdge outgoing : node.outEdges) {
            if (!outgoing.to.isContracted()) {
                destinationNodes.add(outgoing.to);
                if (outgoing.cost > maxCost)
                    maxCost = outgoing.cost;
            }
        }
        
        for (CHEdge incoming : node.inEdges) {
            CHNode startNode = incoming.from;
            if (startNode.isContracted())
                continue;
            
            List<DijkstraSolution> routed = Dijkstra.dijkstrasAlgorithm(
                    startNode,
                    new HashSet<>(destinationNodes),
                    incoming.cost+maxCost,
                    Direction.FORWARDS);
            
            for (DijkstraSolution ds : routed) {
                if (ds.nodes.size() == 3 && ds.nodes.get(1)==node) {
                    shortcuts.add(new ShortcutEdge(ds.getFirstNode(), ds.getLastNode(), ds.totalCost, ds.edges.get(0),
						ds.edges.get(1)));
                } else {
                    //System.out.println();
                }
            }
        }
        
        return shortcuts;
    }
	
	private int getEdgeRemovedCount(CHNode node) {
        return node.getCountOutgoingUncontractedEdges() + node.getCountIncomingUncontractedEdges();
    }
	
	private void contractNode(CHNode node, int order, ArrayList<CHEdge> shortcuts) {
        for (CHEdge shortcut : shortcuts) {
//            CHEdge newShortcut = shortcut.cloneWithEdgeId(allNodes.getEdgeIdCounter().incrementAndGet());
            shortcut.from.outEdges.add(shortcut);
            shortcut.to.inEdges.add(shortcut);
        }
        node.contractionOrder = order;
    }
	
	private void reorderImmediateNeighbors(CHNode justContracted) {
        for (CHNode neighbor : justContracted.getNeighbors()) {
            neighbor.sortNeighborLists();
        }
        
        for (CHNode neighbor : justContracted.getNeighbors()) {
            reorderNodeIfNeeded(neighbor);
        }
    }
    
    private boolean reorderNodeIfNeeded(CHNode n) {
        Ordering oldOrder = contractionOrder.keyForValue(n);
        if (oldOrder == null) { // Already contracted (IIRC)
            return false;
        }
        
        ArrayList<CHEdge> shortcuts = findShortcuts(n);
        int balanceOfEdgesRemoved = getEdgeRemovedCount(n)-shortcuts.size();
        Ordering newOrder = new Ordering(n, balanceOfEdgesRemoved);
        
        if (oldOrder.compareTo(newOrder) != 0) {
            contractionOrder.remove(oldOrder);
            contractionOrder.put(newOrder, n);
            return true;
        } else {
            return false;
        }
    }

	private Trip<SimulationNode> solutionToTrip(DijkstraSolution computed) {
		LinkedList<SimulationNode> locations = new LinkedList<>();
		for (CHNode n : computed.nodes) {
			locations.add(new SimulationNode(n.node.id, findShortcutsCalls, n.node));
		}
		return new Trip<>(locations);
	}
	
	private class KeyValue {
        final Ordering key;
        final CHNode value;

        public KeyValue(Ordering key, CHNode value) {
            this.key = key;
            this.value = value;
        }
    }
}
