/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.helpers;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author olga
 */
public class DistUtils {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistUtils.class);
    final static double RAD = FastMath.PI/180;
    final static double Z = 27*RAD;
    final static double N1 = 0.006739496742;
    final static double N2 = 1.674057895e-07;
    final static double N3 = 4.258201531e-05;
    final static double N4 = 0.005054622556;
    final static double N5 = 0.9996*6399593.625;
    final static double N6 = 0.0820944379*0.0820944379;
    
    public static double[] degreeToUtm(double[] point)  {
        return degreeToUtm(point[0], point[1]);
    }
    
    public static double[] degreeToUtm(double Lat,double Lon)  {
       
        double lon = Lon*RAD;
        double lat = Lat*RAD;
        double lonz = lon - Z;
        double cosLat = FastMath.cos(lat);
        double sinLon = FastMath.sin(lonz);
        double cosLon = FastMath.cos(lonz);
        double cosLatSquared = cosLat*cosLat;
        double sin2Lat = FastMath.sin(2*lat);
        double c2 = lat+sin2Lat/2;
        double cosLatSinLon = cosLat*sinLon;
        double c23 = 3*c2;
        double l = FastMath.log((1+cosLatSinLon)/(1-cosLatSinLon));
        double sc = sin2Lat*cosLatSquared;
        double c42 = 0.25*l*l;
      
        
        double x = 0.5 * l * N5 / FastMath.sqrt(1 + N6 * cosLatSquared)
                    * (1 + N6 / 2 * c42 * cosLatSquared / 3) 
                    + 500000;
      
        double y = (FastMath.atan(FastMath.tan(lat) / cosLon) - lat) * N5 
                    / FastMath.sqrt(1 + N1 * cosLatSquared) 
                    * (1 + N1 /2 * c42 * cosLatSquared)
                    + N5 * (lat - N4 * c2 + N3 * (c23 + sc) / 4
                    - N2 * (5 * (c23 + sc)/4 + sc * cosLatSquared) / 3);
        
        return new double[]{x, y};
    }
    
    static double findDistance(double x1, double y1, double x2, double y2){
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return FastMath.sqrt(deltaX*deltaX + deltaY*deltaY);
    }
    static double findDistance(double[] p1, double[] p2){
        double x = p2[0] - p1[0];
        double y = p2[1] - p1[1];
        return FastMath.sqrt(x*x + y*y);
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
       double deltaLat = FastMath.toRadians(lat2 - lat1);
       double deltaLon = FastMath.toRadians(lon2 - lon1);
       
       double sinLat = FastMath.sin(deltaLat/2);
       double sinLon = FastMath.sin(deltaLon/2);
       double a = sinLat * sinLat +
                  FastMath.cos(lat1)*FastMath.cos(lat2)*
                  sinLon * sinLon;
       
       double c = 2 *FastMath.atan2(FastMath.sqrt(a), FastMath.sqrt(1-a));
       return R * c;
   }
    
    
    
    public static double getHaversineDist(GPSLocation start, GPSLocation target){
       return getHaversineDist(start.getLatitude(),start.getLongitude(),
                               target.getLatitude(), target.getLongitude());
    }
    
        
    public static double getSphericalCosineDist(GPSLocation start, GPSLocation target){
        //φ1 = lat1.toRadians(), 
        //φ2 = lat2.toRadians(), 
        //Δλ = (lon2-lon1).toRadians(),
        //d = R * Math.acos(Math.sin(φ1)*Math.sin(φ2) +
        //                  Math.cos(φ1)*Math.cos(φ2) * Math.cos(Δλ) ) 

        double R = 6371000;
        double lat1 = FastMath.toRadians(start.getLatitude());
        double lat2 = FastMath.toRadians(target.getLatitude());
        double deltaLon = FastMath.toRadians(start.getLongitude() - target.getLongitude());
        
        double a = FastMath.sin(lat1)*FastMath.sin(lat2) +
                   FastMath.cos(lat1)*FastMath.cos(lat2)*FastMath.cos(deltaLon);
        return FastMath.acos(a)*R;
        
    }
    /**
     * Returns euclidean distance between 2 gps locations.
     * @param start
     * @param target
     * @return 
     */
    public static double getEuclideanDist(GPSLocation start, GPSLocation target){
        return getDistProjected(degreeToUtm(start.getLatitude(), start.getLongitude()),
                       degreeToUtm(target.getLatitude(), target.getLongitude()));
    }
    
    public static double getEuclideanDist(double lat1, double lon1, double lat2, double lon2){
        return getDistProjected(degreeToUtm(lat1, lon1), degreeToUtm(lat2, lon2));
    }
    
    public static double getDistProjected(double[] startProjected, double[] targetProjected){
        double x = targetProjected[0] - startProjected[0];
        double y = targetProjected[1] - startProjected[1];
        return FastMath.sqrt(x*x + y*y);
    }
     
    public static double getEquirectangularDist(GPSLocation start, GPSLocation target){
        double R = 6371000; 
        double avgLat = (start.getLatitude() + target.getLatitude())/2;
        double cosLatitude = FastMath.cos(avgLat*RAD);
        double x = start.getLatitude() - target.getLatitude();
        double y = (start.getLongitude() - target.getLongitude()) * cosLatitude;
        return Math.sqrt(x*x + y*y)*R;
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
