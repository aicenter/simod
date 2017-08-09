/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.geo;

import cz.cvut.fel.aic.geographtools.GPSLocation;

/**
 *
 * @author fido
 */
//public class ProjectionUtils {
//    public static int utmToSrid(GPSLocation gpsLocation){
//        
//        int baseSrid;
//    
//        if(gpsLocation.getLatitude() < 0){
//            // south hemisphere
//            baseSrid = 32700;
//        }
//        else{
//            // north hemisphere or on equator
//            baseSrid = 32600;
//        }
//
//        baseSrid = (int) (baseSrid + Math.floor((gpsLocation.getLongitude() + 186) / 6));
//        if(gpsLocation.getLongitude() == 180){
//                 out_srid := base_srid + 60;
//        }
//
//         --- TODO: consider special cases around norway etc.
//        return baseSrid;
//    }
//}
