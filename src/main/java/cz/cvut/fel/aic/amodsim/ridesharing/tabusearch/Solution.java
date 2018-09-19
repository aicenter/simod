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
import static cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.SearchNode.createNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
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
    
    //constants for differentiantion of node types by node id;
    final int N; //pickup  ids [0..N); N is total number of orders in source file before any filtering
    final int N2;//dropoff ids [N ..N2)
    final int N2M; //depot ids   [N2..N2M); M is number of depos
      
    
    private int carCounter;
   
    private List<Route> routes;
    
    final int[] times;
    final double[] values;
    TripList tripList;
   Map<Integer, double[][]> depos;
   
   int[] failReasons = new int[]{0,0,0,0,0};
    
    
    RTree<Integer, Geometry> rtree;
    RTree<Integer, Geometry> depoRtree;
    
    public Solution(TripList tripList) {
        N = tripList.getNumOfTrips();
        N2 = 2*N;
        N2M = N2 + tripList.getNumOfDepos();
        this.tripList = tripList;
        times = tripList.times;
        values = tripList.values;
        depos = tripList.getDepoMap();
        this.id = counter++;
        carCounter = 0;
        
        routes = new LinkedList<>();
        rtree = buildRtree('n');
        depoRtree = buildRtree('d');
        
        
        System.out.println("Solution with parameters: \n");
        System.out.println("Pickup nodes id range [0  "+N+")\n");
        System.out.println("Dropoff nodes id range ["+N+" "+N2+")\n");
        System.out.println("Pickup nodes id range ["+N2+" "+N2M+")\n");
        System.out.println("Trips Rtree size: "+rtree.size());
        System.out.println("Depo Rtree size: "+rtree.size());
    }
    
    //TODO move from here, mb to triplist, returns rtree
    private RTree<Integer, Geometry> buildRtree(char type){
        RTree<Integer, Geometry> tree = RTree.star().create();
        if(type == 'n'){
            for(Integer tripId: tripList.searchNodes.keySet()){
                SearchNode node = tripList.searchNodes.get(tripId);
                double[] point = DistUtils.degreeToUtm(node.point);
                Point p = point(point[0], point[1]);
                tree = tree.add(tripId, p);
            }
        } else if (type == 'd'){
            for(Integer depoId: depos.keySet()){
                double[][] coord = depos.get(depoId);
                Point p = point(coord[1][0], coord[1][0]);
                tree = tree.add(depoId, p);
            }
        }
        return tree;
    }

    public void makeInitialSolution(){
        List<SearchNode> queue = tripList.searchNodes.values().stream()
            .filter(n->n.id<1000).collect(Collectors.toCollection(LinkedList::new));
        
        while(!queue.isEmpty()){
        //for(SearchNode p: queue){
            SearchNode p = queue.remove(0);
            if(carCounter < MAXCAR){
                int[] depo = findNearestDepo(p);
                if(depo[0] < 0){
                    failReasons[0]++;
                    System.out.println("Couldn't find depo for the trip");
                    continue;
                }
            SearchNode depoNode = createNode(depo[0] + N2M, new Object[]{depo[1]}, 0 );
            SearchNode d = tripList.searchNodes.get(p.id + N);
            Route route = new Route(carCounter,
                new SearchNode[]{p, d});
            routes.add(route);
            carCounter++;    
                }
           
           }
        for(Route r: routes){
               System.out.println(r);
        }
        //Stage 1. new car to each trip.
        //get node from list, check if exists empty root, 
        //TODO find nearest depo, reachable in 3 min
        //create new route(=add new car), assign trip to that route
        // add depo node, and first trip nodes,
        //check feasibility(?), update route params
        
        // Stage 2.
        // add new pickup node after last dropoff node for the car
        // which is closest to the point
        // check feasibility, update route
        
        
    }
    
    private int[] findNearestDepo(SearchNode node){
        double radius = 2500;
        double[] point = DistUtils.degreeToUtm(node.point);

        Rectangle bounds = rectangle(point[0] - radius, point[1] - radius, 
                                     point[0] + radius, point[1] + radius);
        
        int[] nearestDepo = new int[]{-1, -1};
        double minDist = Double.MAX_VALUE;
        
        Iterator<Entry<Integer, Geometry>> results = depoRtree.search(bounds).toBlocking().getIterator();
        while(!results.hasNext()){
            radius *= 2;
            bounds = rectangle(point[0] - radius, point[1] - radius,
                               point[0] + radius, point[1] + radius);
            results = depoRtree.search(bounds).toBlocking().getIterator();
        }
        
        while(results.hasNext()){
            Integer depoId = results.next().value();
            SimulationNode depoNode = tripList.depos.get(depoId).getPosition();
            int dist = tripList.getShortesPathDist(depoNode.id, node.nodes[0].id);
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
    
    public void addNewRoute(int carId, SearchNode[] nodes){
        Route newRoute = new Route(carId, nodes);
        routes.add(newRoute);
//        if(carMap)
//        carMap.put(carId, newRoute.id);
    }
    
    public boolean isFeasible(){
        return routes.stream().allMatch((route) -> (route.isFeasible()));
    }
    
//    public double getTotalValue(){
//        return routes.stream().map(route->route.getTotalValue())
//            .mapToDouble(Double::doubleValue).sum();
//    }
    
    
    
    
}
