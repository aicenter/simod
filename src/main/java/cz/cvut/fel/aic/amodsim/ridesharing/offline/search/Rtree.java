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
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class Rtree {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Rtree.class);
 
    RTree<Integer, Point> nodeTree;
    int[] sample = new int[]{15502,16003,25538,138230,159386};
    
    // matching demand to graph

    /**
     * Rtree for trips (nodes and edges)
     * 
     * @param nodes
     * @param edges
     */
    public Rtree(Collection<SimulationNode> nodes, Collection<SimulationEdge> edges){
        nodeTree = RTree.star().create();
        for(SimulationNode node:nodes){
//            LOGGER.debug("\nunproj: " +node.getLongitude() + ", " + node.getLatitude()+"; proj: " +
//                node.getLongitudeProjected() + ", " + node.getLatitudeProjected());
            nodeTree = nodeTree.add(node.id, point(node.getLongitudeProjected(), node.getLatitudeProjected()));
        }
    }
    /**
     * 
     * @param loc   {@link cz.cvut.fel.aic.geographtools.GPSLocation} 
     * @param radius search radius
     * @return id of the nearest node
     * 
     */
    public Integer findNodeId(GPSLocation loc, double radius){
        double locX = loc.getLongitudeProjected();
       
        double locY = loc.getLatitudeProjected();
   //     LOGGER.debug(locX + ", "+locY);
        Rectangle mbr = rectangle(locX - radius, locY - radius, locX + radius, locY + radius);
        
        Iterator<Entry<Integer, Point>> pointIt = nodeTree.search(mbr).toBlocking().toIterable().iterator();
        if(pointIt.hasNext()){
            Entry entry = pointIt.next();
            return (int) entry.value();
        }
        return null;
    }

    /**
     * Rtree for stations (nodes only).
     * @param nodes
     */
    public  Rtree(Collection<SimulationNode> nodes){
        nodeTree = RTree.star().create();
        for(SimulationNode node:nodes){
            nodeTree = nodeTree.add(node.id, point(node.getLongitudeProjected(), node.getLatitudeProjected()));
        }
    }
    
    /**
     * 
     * @param loc {@link cz.cvut.fel.aic.geographtools.GPSLocation} 
     * @return list of station nodes ids
     */
    public List<Integer> findNearestStations(GPSLocation loc){
        int radius = 100;
        List<Integer> result = new ArrayList<>();
        double locX = loc.getLongitudeProjected();
        double locY = loc.getLatitudeProjected();
        while(result.isEmpty()){
            Rectangle mbr = rectangle(locX - radius, locY - radius, locX + radius, locY + radius);
            Iterator<Entry<Integer, Point>> pointIt = nodeTree.search(mbr).toBlocking().toIterable().iterator();
            while(pointIt.hasNext()){
                Entry entry = pointIt.next();
                result.add((Integer) entry.value());
            }
            radius *=2;
        }
        return result;       
    }
    
    /**
     * Total number of elements in node and edge trees.
     * @return
     */
    public int size(){
        return nodeTree.size();
    }
    
    
    // groups

    /**
     *
     * @param points
     */
    public Rtree(List<double[]> points){
        nodeTree = RTree.star().create();
        for(int i=0; i<points.size();i++){
            double[] point = points.get(i);
            nodeTree = nodeTree.add(i, point(point[1], point[0]));
        }
    }
    
    /**
     * Find graph nodes inside the given radius.
     * 
     * @param point array with point coordinates
     * @param radius search radius
     * @return list of nodes ids
     */
    public List<Integer> findNodes(double[] point, int radius){
        Rectangle mbr = rectangle(point[1] - radius, point[0] - radius, point[1] + radius, point[0] + radius);
        
        Iterator<Entry<Integer, Point>> pointIt = nodeTree.search(mbr).toBlocking().toIterable().iterator();
        List<Integer> result = new ArrayList<>(); 
        while(pointIt.hasNext()){
            result.add((int) pointIt.next().value());
        }
       return result; 
    }
    
}
