/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import static com.github.davidmoten.rtree.geometry.Geometries.point;
import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    TripList tripList;
   
   int[] failReasons = new int[]{0,0,0,0,0};
    
    
    RTree<Integer, Geometry> rtree;
    RTree<Integer, Geometry> depoRtree;
    GraphUtils GU;
    
    
    public Solution(TripList tripList) {
        N = tripList.getNumOfTrips();
        N2 = 2*N;
        N2M = N2 + tripList.getNumOfDepos();
        this.tripList = tripList;
        times = tripList.times;
        values = tripList.values;
        this.id = counter++;
        carCounter = 0;
        GU = new GraphUtils(tripList);
        
    //    routes = new LinkedList<>();
        rtree = tripList.buildRtree('n');
        depoRtree = tripList.buildRtree('d');
        
        
        System.out.println("Solution with parameters: \n");
        System.out.println("Pickup nodes id range [0  "+N+")\n");
        System.out.println("Dropoff nodes id range ["+N+" "+N2+")\n");
        System.out.println("Pickup nodes id range ["+N2+" "+N2M+")\n");
        System.out.println("Trips Rtree size: "+rtree.size());
        System.out.println("Depo Rtree size: "+rtree.size());
    }
    
  

    
    private int[] findNearestDepo(SearchNode node){
        double radius = 2500;
        
        SimulationNode simNode = GU.getNodeById(node.vi);
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
            SimulationNode depoNode = tripList.depos.get(depoId).getPosition();
//            int dist = tripList.getShortesPathDist(depoNode.id, node.nodes[0].id);
            if(dist <= radius){
                return new int[]{depoId, depoNode.id};
            }
            if(dist < minDist){
                minDist = dist;
                nearestDepo[0] = depoId;
                nearestDepo[1] = depoNode.id;
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
