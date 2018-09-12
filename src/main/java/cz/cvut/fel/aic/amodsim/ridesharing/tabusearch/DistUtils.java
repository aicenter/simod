/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class DistUtils {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistUtils.class);
    
    public static double getHaversineDist(GPSLocation start, GPSLocation target){
       double rad = 6366752;
       double deltaLat = Math.toRadians(target.getLatitude()) -Math.toRadians(start.getLatitude());
       double deltaLon = Math.toRadians(start.getLongitude()) - Math.toRadians(target.getLongitude());
       double sinLat = Math.sin(deltaLat/2);
       double sinLon = Math.sin(deltaLon/2);
       double h = sinLat*sinLat+Math.cos(Math.toRadians(start.getLatitude()))
           *Math.cos(Math.toRadians(target.getLatitude()))*sinLon*sinLon;
       if(h > 1){
           LOGGER.warn("Haversine greater than 1 {}", h);
       }
       return 2*rad*Math.asin(h);
   }
    
        
    public static double getEuclideanDist(GPSLocation start, GPSLocation target){
        double x = start.getLatitudeProjected() - target.getLatitudeProjected();
        double y = start.getLongitudeProjected() - target.getLongitudeProjected();
        return Math.sqrt(x*x + y*y);
    }
    
    public static double getApproximateDist(GPSLocation start, GPSLocation target){
                    
        int degreeLength = 111000;
        double cosLatitude = Math.cos(Math.toRadians(start.getLatitude()));
        double x = start.getLatitude() - target.getLatitude();
        double y = start.getLongitude() - target.getLongitude();
        y *= cosLatitude;
        return Math.sqrt(x*x + y*y)*degreeLength;
    }
    
    public static double[] pathToCoordinates(LinkedList<SimulationNode> path){
        List<Double> coordinates = new ArrayList<>();
        for(SimulationNode node: path){
            coordinates.add(node.getLatitude());
            coordinates.add(node.getLongitude());
        }
        return coordinates.stream().mapToDouble(Double::doubleValue).toArray();
    }
   
  
    
}
