/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.OsmUtil;
//import cz.agents.amodsim.pathPlanner.PathPlanner;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;


/**
 *
 * @author F-I-D-O
 */
@Singleton
public class TripTransformTaxify {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransformTaxify.class);
    private static final int SRID = 32633;
    private int zeroLenghtTripsCount = 0;
    private int startTooFarCount = 0;
    private int targetTooFarCount = 0;
    private int tooLongCount = 0;
    private int sameStartAndTargetInDataCount = 0;

    
    private final double maxRideDistance;
    private final int pickupRadius;
    AmodsimConfig config;
    TravelTimeProvider travelTimeProvider;
    private final Graph<SimulationNode,SimulationEdge> graph;
    private Rtree rtree;
//     List<Integer>[] unmappedTrips; 
    
    @Inject
    public TripTransformTaxify(HighwayNetwork highwayNetwork, NearestElementUtils nearestElementUtils,
        AmodsimConfig config, TravelTimeProvider travelTimeProvider) {
        this.graph = highwayNetwork.getNetwork();
        this.travelTimeProvider = travelTimeProvider;
        //this.nearestElementUtils = nearestElementUtils;
        this.config = config;
        pickupRadius = config.amodsim.ridesharing.pickupRadius;
        maxRideDistance = 1000 * (config.amodsim.ridesharing.maxRideTime / 60.0) * config.amodsim.ridesharing.maxSpeedEstimation;
//        unmappedTrips = new List[2];
//        unmappedTrips[0] = new ArrayList<>();
//        unmappedTrips[1] = new ArrayList<>();
    }   
       

	
    
    public List<TripTaxify<GPSLocation>> loadTripsFromCsv(File inputFile) throws FileNotFoundException, IOException, ParseException{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse("2022-03-01 00:00:00" ,formatter);
            int startDay = startDateTime.getDayOfMonth();
            rtree = new Rtree(this.graph.getAllNodes(), this.graph.getAllEdges());
            List<TripTaxify<GPSLocation>> gpsTrips = new LinkedList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
                String line;
                int count = -1;
                System.out.println(br.readLine());
                while ((line = br.readLine()) != null) {
                    count++;
                    String[] parts = line.split(",");
                    //System.out.println(count+Arrays.toString(parts));
                    GPSLocation startLocation
                        = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, SRID);
                    GPSLocation targetLocation
                        = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, SRID);
//
//                if(startLocation.equals(targetLocation)){
//                   sameStartAndTargetInDataCount++;
//                   continue;
//                }
                    //System.out.println("parts[0]="+parts[0].substring(0,19));
                    LocalDateTime dateTime = LocalDateTime.parse(parts[0].substring(0,19) ,formatter);
                    int day = dateTime.getDayOfMonth() - startDay;
                    int hour = day*24 + dateTime.getHour();
                    int min = hour*60 + dateTime.getMinute();
                    int sec = min*60 + dateTime.getSecond();
                    int millisFromStart = sec*1000 + Integer.parseInt(parts[0].substring(20,23));
                    //long millisFromStart = (dateTime.getTime() - startMillis);
                    
                  //  System.out.println("millis from start "+millisFromStart);
                    gpsTrips.add(new TripTaxify<>(count, startLocation, targetLocation,
                        millisFromStart, Double.parseDouble(parts[5])));
                }
            }catch (IOException ex) {
                LOGGER.error(null, ex);
            }
            List<TripTaxify<GPSLocation>> trips = new ArrayList<>();
            for (TripTaxify<GPSLocation> trip : ProgressBar.wrap(gpsTrips, "Process GPS trip: ")) {
                processGpsTrip(trip, trips);
            }
            
            LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
            LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);
            LOGGER.info("{} too long trips discarded", tooLongCount);
            LOGGER.info("{} trips with start node far away from graph discarded", startTooFarCount);
            LOGGER.info("{} trips with target node far away from graph  discarded", targetTooFarCount);
            LOGGER.info("{} trips remained", trips.size());
            LOGGER.info("{} nodes not found in node tree", rtree.count);
            rtree = null;
//        FileOutputStream fos = new FileOutputStream(config.amodsimExperimentDir+"/unmapped");
//        ObjectOutputStream oos = new ObjectOutputStream(fos);
//        oos.writeObject(unmappedTrips);
        return trips;
    }
        
    private void processGpsTrip(TripTaxify<GPSLocation> gpsTrip, List<TripTaxify<GPSLocation>>trips) {
        int maxDist2 = 25000*25000;
        LinkedList<GPSLocation> locations = gpsTrip.getLocations();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);

        // longer than 25 km
        double x = startLocation.getLongitudeProjected() - targetLocation.getLongitudeProjected();
        double y = startLocation.getLatitudeProjected() - targetLocation.getLatitudeProjected();
        if((x*x + y*y) > maxDist2){
            tooLongCount++;
            return;
        }
        Map<Integer, Double> startNodesMap = new HashMap<>();
        Map<Integer, Double> targetNodesMap = new HashMap<>();
        double[] coord = new double[4];
        Object[] result = rtree.findNode(startLocation, pickupRadius);
        if (result == null){
            startTooFarCount++;
            return;
        }else if(result.length == 1){
            int simNodeId = (int) result[0];
            startNodesMap.put(simNodeId, 0.0);
            coord[0] = graph.getNode(simNodeId).getLatitude();
            coord[1] = graph.getNode(simNodeId).getLongitude();
        }else{
            startNodesMap.put((int) result[0], (double) result[2]);
            startNodesMap.put((int) result[1], (double) result[3]);
            double[] projCoord = (double[]) result[4];
            double[] gpsCoord = fromProjected(projCoord);
            coord[0] = gpsCoord[0];
            coord[1] = gpsCoord[1];
        }
        result = rtree.findNode(targetLocation, pickupRadius);
        if (result == null){
            targetTooFarCount++;
            
            return;
        }else if(result.length == 1){
            int simNodeId = (int) result[0];
            targetNodesMap.put(simNodeId, 0.0);
            coord[2] = graph.getNode(simNodeId).getLatitude();
            coord[3] = graph.getNode(simNodeId).getLongitude();
        }else{
            targetNodesMap.put((int) result[0], (double) result[2]);
            targetNodesMap.put((int) result[1], (double) result[3]);
            double[] projCoord = (double[]) result[4];
            double[] gpsCoord = fromProjected(projCoord);
            coord[0] = gpsCoord[0];
            coord[1] = gpsCoord[1];
        }
        gpsTrip.addNodeMaps(startNodesMap, targetNodesMap);
        if(travelTimeProvider.getTravelTimeInMillis(gpsTrip) < 1800000){
            
            gpsTrip.setCoordinates(coord);
            trips.add(gpsTrip);
        }
    }

    public Graph<SimulationNode, SimulationEdge> getGraph() {
        return graph;
    }
    
    public List<SimulationNode> loadStations() throws IOException{
        List<SimulationNode> stationNodes = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> data = mapper.readValue(new File(config.rebalancing.policyFilePath), Map.class);
        ArrayList stations = (ArrayList) data.get("stations");
        
        rtree = new Rtree(this.graph.getAllNodes(), this.graph.getAllEdges());
        for(int i = 0; i < stations.size();i++ ){
            ArrayList<Double> station = (ArrayList<Double>) stations.get(i);
            GPSLocation location = GPSLocationTools.createGPSLocation(station.get(0), station.get(1), 0, SRID);
            Object[] result = rtree.findNode(location, 4*pickupRadius);
            if (result == null){
                LOGGER.error("Node not found for station " + i);
            }else{
                stationNodes.add(this.graph.getNode((int) result[0]));
            }
        }
        rtree = null;
        return stationNodes;
    }
    
    private double[] fromProjected(double[] projCoord){
        GPSLocation loc = GPSLocationTools.createGPSLocationFromProjected(
            (int) (projCoord[1]*1E2),(int) (projCoord[0]*1E2),0, SRID);
        
        return new double[]{loc.getLatitude(), loc.getLongitude()};
    }
}
 
    
  