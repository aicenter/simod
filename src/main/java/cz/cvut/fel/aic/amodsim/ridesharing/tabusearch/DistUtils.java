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
       return getHaversineDist(start.getLatitude(),start.getLongitude(),
                               target.getLatitude(), target.getLongitude());
    }
    
    public static double getHaversineDist(double lat1, double lon1, 
        double lat2, double lon2){
//        φ1 = lat1.toRadians();
//        φ2 = lat2.toRadians();
//        Δφ = (lat2-lat1).toRadians();
//        Δλ = (lon2-lon1).toRadians();
//        a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
//                Math.cos(φ1) * Math.cos(φ2) *
//                Math.sin(Δλ/2) * Math.sin(Δλ/2);
//        c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//        d = R * c;

       double R = 6371000;
       lat1 = Math.toRadians(lat1);
       lat2 = Math.toRadians(lat2);
       double deltaLat = Math.toRadians(lat2 - lat1);
       double deltaLon = Math.toRadians(lon2 - lon1);
       
       double sinLat = Math.sin(deltaLat/2);
       double sinLon = Math.sin(deltaLon/2);
       double a = sinLat * sinLat +
                  Math.cos(lat1)*Math.cos(lat2)*
                  sinLon * sinLon;
       
       double c = 2 *Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
       return R * c;
   }
       
    public static double getSphericalCosineDist(GPSLocation start, GPSLocation target){
        //φ1 = lat1.toRadians(), 
        //φ2 = lat2.toRadians(), 
        //Δλ = (lon2-lon1).toRadians(),
        //d = R * Math.acos(Math.sin(φ1)*Math.sin(φ2) +
        //                  Math.cos(φ1)*Math.cos(φ2) * Math.cos(Δλ) ) 

        double R = 6371000;
        double lat1 = Math.toRadians(start.getLatitude());
        double lat2 = Math.toRadians(target.getLatitude());
        double deltaLon = Math.toRadians(start.getLongitude() - target.getLongitude());
        
        double a = Math.sin(lat1)*Math.sin(lat2) +
                   Math.cos(lat1)*Math.cos(lat2)*Math.cos(deltaLon);
        return Math.acos(a)*R;
        
    }
         
    public static double getEuclideanDist(GPSLocation start, GPSLocation target){
        double x = start.getLatitudeProjected() - target.getLatitudeProjected();
        double y = start.getLongitudeProjected() - target.getLongitudeProjected();
        return Math.sqrt(x*x + y*y);
    }
    
    public static double getEquirectangularDist(GPSLocation start, GPSLocation target){
        double R = 6371000; 
        double avgLat = (start.getLatitude() + target.getLatitude())/2;
        double cosLatitude = Math.cos(avgLat);
        double x = start.getLatitude() - target.getLatitude();
        double y = (start.getLongitude() - target.getLongitude()) * cosLatitude;
        return Math.sqrt(x*x + y*y)*R;
    }
    
//    public static double getCircleDist(GPSLocation start, GPSLocation target){
//        //WGS84 World Geodetic System 1984
//        double wgs84_a = 6378137.0;
//        double wgs84_b = 6356752.314245;
//        double wgs84_f = 298.257223563;
//        double wgs84_R = 6371008.8;
//       
//    }    
    public static double[] pathToCoordinates(LinkedList<SimulationNode> path){
        List<Double> coordinates = new ArrayList<>();
        for(SimulationNode node: path){
            coordinates.add(node.getLatitude());
            coordinates.add(node.getLongitude());
        }
        return coordinates.stream().mapToDouble(Double::doubleValue).toArray();
    }
   
  
    
}
