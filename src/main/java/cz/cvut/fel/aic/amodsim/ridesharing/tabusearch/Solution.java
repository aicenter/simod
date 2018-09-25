/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.helpers.GraphUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import static cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.helpers.TreeUtils.buildRtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import java.util.Arrays;

/**
 *
 * @author olga
 */
public class Solution {
    private static int counter = 0;
    private final static int MAXCAR = 10;
    private final int id;
    
    //constants for differentiantion of vi types by vi i;
    final int N; //pickup  ids [0..N); N is total number of orders in source file before any filtering
    final int N2;//dropoff ids [N ..N2)
    final int N2M; //depot ids   [N2..N2M); M is number of depos
       
    private int carCounter;
   
 //   private List<Route> routes;
    
    final int[] times;
    final double[] values;
   
    int[] failReasons = new int[]{0,0,0,0,0};
    
    
    RTree<Integer, Geometry> rtree;
    RTree<Integer, Geometry> depoRtree;
    GraphUtils GU;
    List<SearchNode> depos;
    
    
    public Solution(TripList tripList) {
        N = tripList.getNumOfTrips();
        N2 = 2*N;
        N2M = N2 + tripList.getNumOfDepos();
        times = tripList.times;
        values = tripList.values;
        this.id = counter++;
        carCounter = 0;
        GU = new GraphUtils(tripList.buildTripGraph(), N);
        depos = new ArrayList<>();
        int depoId = 0;
        for(OnDemandVehicleStation depo: tripList.depos) {
                depos.add(new SearchNode(depoId + N2M, depo.getPosition()));
                depoId++;
        }
        System.out.println(Arrays.toString(depos.toArray()));
    //    routes = new LinkedList<>();
        rtree = buildRtree(GU, tripList.searchNodes.values());
        System.out.println("Node  tree built: size "+rtree.size()+", depth "+rtree.calculateDepth());
        depoRtree = buildRtree(GU, depos);
        System.out.println("Depo  tree built: size "+depoRtree.size()+", depth "+depoRtree.calculateDepth());
        
        
        System.out.println("Solution with parameters: ");
        System.out.println("Pickup nodes id range [0  "+N+")");
        System.out.println("Dropoff nodes id range ["+N+" "+N2+")");
        System.out.println("Pickup nodes id range ["+N2+" "+N2M+")");
        System.out.println("Graph with "+GU.numberOfNodes()+" and "+GU.numberOfEdges()+" edges");
    }
    
  

    
    private int[] findNearestDepo(SearchNode node){
        double radius = 2500;
        
        SimulationNode simNode = GU.getNode(node.vi);
        double x = simNode.getLongitudeProjected();
        double y = simNode.getLatitudeProjected();
        Rectangle bounds = rectangle(x - radius, y - radius, x + radius, y + radius);
        
        int[] nearestDepo = new int[]{-1, -1};
        double minDist = Double.MAX_VALUE;
        
        Iterator<Entry<Integer, Geometry>> results = depoRtree.search(bounds).toBlocking().getIterator();
        while(!results.hasNext()){
            radius *= 2;
            bounds = rectangle(x - radius, y - radius, x + radius, y + radius);
            results = depoRtree.search(bounds).toBlocking().getIterator();
        }
        
        while(results.hasNext()){
            Integer depoId = results.next().value();
            SearchNode depoNode = depos.get(depoId);
            double dist = GU.getSPDist(depoNode.i, node.vi);
            if(dist <= radius){
                return new int[]{depoId, depoNode.i};
            }
            if(dist < minDist){
                minDist = dist;
                nearestDepo[0] = depoId;
                nearestDepo[1] = depoNode.i;
            }
        }
        return nearestDepo;
    }
    
//    public void addNewRoute(int carId, SearchNode[] nodes){
//        Route newRoute = new Route(carId, nodes);
//        routes.add(newRoute);
////        if(carMap)
////        carMap.put(carId, newRoute.i);
//    }
//    
//    public boolean isFeasible(){
//        return routes.stream().allMatch((route) -> (route.isFeasible()));
//    }
    
//    public double getTotalValue(){
//        return routes.stream().map(route->route.getTotalValue())
//            .mapToDouble(Double::doubleValue).sum();
//    }
    
    
    
    
}
