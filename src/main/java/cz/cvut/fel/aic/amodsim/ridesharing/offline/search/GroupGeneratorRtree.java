/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.offline.search;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import static com.github.davidmoten.rtree.geometry.Geometries.*;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class GroupGeneratorRtree {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGeneratorRtree.class);
 
    RTree<Integer, Point> pickupTree;
    
    private final NormalDemand demand;
    
    int[] sample = new int[]{15502,16003,25538,138230,159386};
    
    // matching demand to graph

    /**
     * Rtree for trip pickup nodes
     *
     * @param demand
     */
    public GroupGeneratorRtree(NormalDemand demand){
        this.demand = demand;

    }
     
  
    /**
     * Add trips by demand index to tree.
     * @param tripIndices
     */
    public void addTrips(Collection<Integer> tripIndices){
        
        pickupTree = RTree.star().create();
     //   Collection<Integer> tripIndices = tripIds.stream().map((id)->demand.tripIdToInd(id)).collect(Collectors.toList());
        for(int ind : tripIndices){
            
            pickupTree = pickupTree.add(ind, 
                point(demand.getStartLongitudeProj(ind), demand.getStartLatitudeProj(ind)));
        }
    }
        
    public List<Integer> findNearestTrips(int tripInd, int distance){
        
        List<Integer> result = new ArrayList<>(); 
        tripInd = tripInd >= 0 ? tripInd : -tripInd -1;
        
//        int tripInd = demand.tripIdToInd(tripId);
        double lon = demand.getStartLongitudeProj(tripInd);
        double lat = demand.getStartLatitudeProj(tripInd);
        Rectangle mbr = rectangle(lon - distance, lat - distance , lon + distance, lat + distance);
        
        Iterator<Entry<Integer, Point>> pointIt = pickupTree.search(mbr).toBlocking().toIterable().iterator();
        while(pointIt.hasNext()){
            result.add((int) pointIt.next().value());
        }
       return result; 
    }
    
    /**
     * Total number of elements in node and edge trees.
     * @return
     */
    public int size(){
        return pickupTree.size();
    }
}
