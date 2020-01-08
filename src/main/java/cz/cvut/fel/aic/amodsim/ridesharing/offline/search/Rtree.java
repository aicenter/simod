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
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.Collection;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class Rtree {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Rtree.class);
 
    RTree<Integer, Point> nodeTree;
      
    /**
     * Finds graph node at max radius away from gps location.
     * (some nodes may be a bit further. it actually looks for 
     * nodes inside the square which much faster than to
     * use circle.
     * 
     * @param loc   {@link cz.cvut.fel.aic.geographtools.GPSLocation} 
     * @param radius search radius
     * @return id of the nearest node
     * 
     */
    public Integer findNodeId(GPSLocation loc, double radius){
        double locX = loc.getLongitudeProjected();
       
        double locY = loc.getLatitudeProjected();
        Rectangle mbr = rectangle(locX - radius, locY - radius, locX + radius, locY + radius);
        
        Iterator<Entry<Integer, Point>> pointIt = nodeTree.search(mbr).toBlocking().toIterable().iterator();
        if(pointIt.hasNext()){
            Entry entry = pointIt.next();
            return (int) entry.value();
        }
        return null;
    }

    /**
     * Rtree with nodes to match demand.
     * @param nodes
     */
    public  Rtree(Collection<SimulationNode> nodes){
        nodeTree = RTree.star().create();
        for(SimulationNode node:nodes){
            nodeTree = nodeTree.add(node.id, point(node.getLongitudeProjected(), node.getLatitudeProjected()));
        }
    }
    
   
    /**
     * Total number of elements in node and edge trees.
     * @return
     */
    public int size(){
        return nodeTree.size();
    }
    
    
}
