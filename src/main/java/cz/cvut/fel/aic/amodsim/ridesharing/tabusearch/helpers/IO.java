/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.helpers;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.amodsim.io.TimeValueTrip;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class IO {
    final static int SRID = 32633;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IO.class);
    
    
    public static List<TimeValueTrip<GPSLocation>> loadTripsFromTxt(File inputFile){
        int sameStartAndTargetInDataCount = 0;
        int tripCount = 0;
        List<TimeValueTrip<GPSLocation>> gpsTrips = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                GPSLocation startLocation
                       = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, SRID);
                GPSLocation targetLocation
                       = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, SRID);
                
                if(startLocation.equals(targetLocation)){
                   sameStartAndTargetInDataCount++;
                }else{
                    gpsTrips.add(new TimeValueTrip<>(tripCount, startLocation, targetLocation, 
                    Long.parseLong(parts[0].split("\\.")[0]), Double.parseDouble(parts[5])));
                }
                tripCount++;
            }
        }catch (IOException ex) {
            LOGGER.error(null, ex);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Total number of demands: ").append(tripCount).append("\n");
        System.out.println(sb.toString());
        LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
        return gpsTrips; 
    }
    
    
 public static void writeEdgesToFile(List<SimulationEdge> edges, String fn){           
        try(PrintWriter pw = new PrintWriter(
            new FileOutputStream(new File(fn)))){
                edges.forEach((e) -> {
                    pw.println(e.fromNode.getLatitude() +" "+ e.fromNode.getLongitude()
                                +" "+e.toNode.getLatitude()+" "+e.toNode.getLongitude());
                });
        }catch (IOException ex){
            LOGGER.error(null, ex);          
        }
 }

}
