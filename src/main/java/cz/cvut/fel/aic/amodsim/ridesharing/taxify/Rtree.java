/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify;
import cz.cvut.fel.aic.amodsim.io.*;
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
 * @author olga
 */
public class Rtree {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Rtree.class);
    RTree<Integer, Point> nodeTree;
    RTree<int[], Rectangle> edgeTree;
    int count = 0;
    
    public Rtree(Collection<SimulationNode> nodes, Collection<SimulationEdge> edges){
        nodeTree = RTree.star().create();
        for(SimulationNode node:nodes){
            nodeTree = nodeTree.add(node.id, point(node.getLongitudeProjected(), node.getLatitudeProjected()));
        }
        edgeTree = RTree.star().create();
        for(SimulationEdge edge:edges){
            Rectangle mbr = rectangle(Math.min(edge.getFromNode().getLongitudeProjected(), edge.getToNode().getLongitudeProjected()),
                                       Math.min(edge.getFromNode().getLatitudeProjected(), edge.getToNode().getLatitudeProjected()),
                                       Math.max(edge.getFromNode().getLongitudeProjected(), edge.getToNode().getLongitudeProjected()),
                                       Math.max(edge.getFromNode().getLatitudeProjected(), edge.getToNode().getLatitudeProjected()));
            edgeTree = edgeTree.add(new int[]{edge.fromNode.id, edge.toNode.id}, mbr);
        }
    }
    
    public  Rtree(Collection<SimulationNode> nodes){
        nodeTree = RTree.star().create();
        for(SimulationNode node:nodes){
            nodeTree = nodeTree.add(node.id, point(node.getLongitudeProjected(), node.getLatitudeProjected()));
        }

    }
    
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
    
    public Object[] findNode(GPSLocation loc, double radius){
        double locX = loc.getLongitudeProjected();
        double locY = loc.getLatitudeProjected();
        Rectangle mbr = rectangle(locX - radius, locY - radius, locX + radius, locY + radius);
        
        Iterator<Entry<Integer, Point>> pointIt = nodeTree.search(mbr).toBlocking().toIterable().iterator();
        double radius2 = radius*radius; 
        while(pointIt.hasNext()){
            Entry entry = pointIt.next();
            Point point = (Point) entry.geometry();
            double x = locX - point.x();
            double y = locY - point.y();
            double dist2 = x*x + y*y ;
            if (dist2 <= radius2){
                return new Object[]{entry.value()};
            }
        }
        
        Iterator<Entry<int[], Rectangle>> edgeIt = edgeTree.search(mbr).toBlocking().toIterable().iterator();
            while(edgeIt.hasNext()){
                Entry entry = edgeIt.next();
                int[] value = (int[]) entry.value();
                Rectangle rect = (Rectangle) entry.geometry();
                double[] edge_start = new double[]{rect.x1(), rect.y1()};
                double[] edge_end = new double[]{rect.x2(), rect.y2()};
                Object[] result = findNearestPoint(new double[]{locX, locY}, edge_start, edge_end, radius2);
                if (result == null){
                    return null;
                }
                if(result.length == 1){
                    return new Object[]{value[(int) result[0]]};
                }else{
                    return new Object[]{value[0], value[1], result[0], result[1]};
                }
            }
        return null;
    }
            
    private Object[] findNearestPoint(double[] point, double[] p0, double[] p1, double radius2){
        //distance( Point P,  Segment P0:P1 ){
       //     v = P1 - P0;        w = P - P0
 //      if ( (c1 = w·v) <= 0 )  // before P0
   //            return d(P, P0)
   //    if ( (c2 = v·v) <= c1 ) // after P1
   //            return d(P, P1)

   //    b = c1 / c2
   //    Pb = P0 + bv
   //    return d(P, Pb)

        double[] v = {p1[0]- p0[0], p1[1] - p0[1]};
        double[] w = {point[0]- p0[0], point[1] - p0[1]};
        double c1 = v[0]*w[0] + v[1]*w[1];
        if(c1 <= 0){
            double x_dist = point[0] - p0[0];
            double y_dist = point[1] - p0[1];
            if((x_dist*x_dist + y_dist*y_dist) <= radius2){
                //LOGGER.error("Wide angle.");
                //LOGGER.debug("c1 >= 0, dist = "+Math.sqrt(x_dist*x_dist + y_dist*y_dist));
                count++;
                return new Object[]{0};
            }
            //LOGGER.error("Closest point is further than query radius1"+Math.sqrt(x_dist*x_dist + y_dist*y_dist));

            return null;
        }
        double c2 = v[0]*v[0] + v[1]*v[1];
        if (c2 <= c1){
            double x_dist = point[0] - p1[0];
            double y_dist = point[1] - p1[1];
            if((x_dist*x_dist + y_dist*y_dist) <= radius2){
                //LOGGER.error("Wide angle.");
                //LOGGER.debug("c2 <= c1, dist = "+Math.sqrt(x_dist*x_dist + y_dist*y_dist));
                count++;
                return new Object[]{1};
            }
            //LOGGER.error("Closest point is further than query radius2"+Math.sqrt(x_dist*x_dist + y_dist*y_dist));
            return null;
        }
        double b = c1/c2;
        double[] pb = new double[]{p0[0] + b*v[0], p0[1]+b*v[1]};
        double x_dist = point[0] - pb[0];
        double y_dist = point[1] - pb[1];
        if((x_dist*x_dist + y_dist*y_dist) > radius2){
                //LOGGER.error("Point not found.");
                //LOGGER.error("Closest point is further than query radius3"+Math.sqrt(x_dist*x_dist + y_dist*y_dist));
                return null;
        }    
        double dist = Math.sqrt(v[0]*v[0] + v[1]*v[1]);
        return new Object[]{b*dist, (1 - b)*dist };
    }
    
    
    public int size(){
        if(edgeTree == null){
            return nodeTree.size();
        }else{
            return nodeTree.size() +edgeTree.size();
        }
    }
    
    
}
