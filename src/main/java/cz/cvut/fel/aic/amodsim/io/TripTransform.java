/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.OsmUtil;
//import cz.agents.amodsim.pathPlanner.PathPlanner;
import com.vividsolutions.jts.geom.Coordinate;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtilPair;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;


/**
 *
 * @author F-I-D-O
 */
@Singleton
public class TripTransform {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransform.class);
    private static final int SRID = 32633;
    private int zeroLenghtTripsCount = 0;
    private int startTooFarCount = 0;
    private int targetTooFarCount = 0;
    private int tooLongCount = 0;
    private int sameStartAndTargetInDataCount = 0;
    private int totalTrips = 0;
    
    private final double maxRideDistance;
    private final int pickupRadius;
        
    private final Graph<SimulationNode,SimulationEdge> highwayGraph;
   // private final NearestElementUtils nearestElementUtils;
    private final Rtree tree;
    
    @Inject
    public TripTransform(HighwayNetwork highwayNetwork, NearestElementUtils nearestElementUtils,
        AmodsimConfig config, TravelTimeProvider travelTimeProvider) {
        this.highwayGraph = highwayNetwork.getNetwork();
        //this.nearestElementUtils = nearestElementUtils;
        pickupRadius = config.amodsim.ridesharing.pickupRadius;
        maxRideDistance = 1000 * (config.amodsim.ridesharing.maxRideTime / 60.0) * config.amodsim.ridesharing.maxSpeedEstimation;
        tree = new Rtree(this.highwayGraph.getAllNodes(), this.highwayGraph.getAllEdges());
    }   
       

	public static <T> void tripsToJson(List<TimeTripWithValue<T>> trips, File outputFile) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(outputFile, trips);
	}
	
	public static <T> List<TimeTripWithValue<T>> jsonToTrips(File inputFile, Class<T> locationType) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		
		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
				List.class, typeFactory.constructParametricType(TimeTripWithValue.class, locationType)));
	}
    
    public List<TimeTripWithValue<GPSLocation>> loadTripsFromTxt(File inputFile){

        List<TimeTripWithValue<GPSLocation>> gpsTrips = new LinkedList<>();
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
                    if(parts.length == 6){
                        gpsTrips.add(new TimeTripWithValue<>(totalTrips, startLocation, targetLocation, 
                        Long.parseLong(parts[0].split("\\.")[0]), Double.parseDouble(parts[5])));
                    }else{
                        gpsTrips.add(new TimeTripWithValue<>(totalTrips, startLocation, targetLocation, 
                        Long.parseLong(parts[0].split("\\.")[0]), 0));
                    }
                }
                totalTrips++;
            }
        }catch (IOException ex) {
            LOGGER.error(null, ex);
        }
        List<TimeTripWithValue<GPSLocation>> trips = new ArrayList<>();
        for (TimeTripWithValue<GPSLocation> trip : ProgressBar.wrap(gpsTrips, "Process GPS trip: ")) {
                processGpsTrip(trip, trips);
        }
        
        LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
        LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);
        LOGGER.info("{} too long trips discarded", tooLongCount);
        LOGGER.info("{} trips with start node far away from graph discarded", startTooFarCount);
        LOGGER.info("{} trips with target node far away from graph  discarded", targetTooFarCount);
        LOGGER.info("{} trips remained", trips.size());
        LOGGER.info("{} nodes not found in node tree", tree.count);
        return trips; 
    }
        
    private void processGpsTrip(TimeTripWithValue<GPSLocation> gpsTrip, List<TimeTripWithValue<GPSLocation>>trips) {
        LinkedList<GPSLocation> locations = gpsTrip.getLocations();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);

        // longer than 25 km
        double x = startLocation.getLongitudeProjected() - targetLocation.getLongitudeProjected();
        double y = startLocation.getLatitudeProjected() - targetLocation.getLatitudeProjected();
        if((x*x + y*y) > maxRideDistance*maxRideDistance){
            tooLongCount++;
            return;
        }
        //SimulationNode startNode = nearestElementUtils.getNearestElement(startLocation, EGraphType.HIGHWAY);
        Map<Integer, Double> startNodesMap = new HashMap<>();
        Map<Integer, Double> targetNodesMap = new HashMap<>();
        Object[] result = tree.findNode(startLocation, pickupRadius);
        if (result == null){
            startTooFarCount++;
            return;
        }else if(result.length == 1){
            startNodesMap.put((int) result[0], 0.0);
        }else{
            startNodesMap.put((int) result[0], (double) result[2]);
            startNodesMap.put((int) result[1], (double) result[3]);
        }
        //SimulationNode targetNode = nearestElementUtils.getNearestElement(targetLocation, EGraphType.HIGHWAY);
        result = tree.findNode(targetLocation, pickupRadius);
        if (result == null){
            targetTooFarCount++;
            return;
        }else if(result.length == 1){
             targetNodesMap.put((int) result[0], 0.0);
        }else{
            targetNodesMap.put((int) result[0], (double) result[2]);
            targetNodesMap.put((int) result[1], (double) result[3]);
        }
        if( Collections.disjoint(startNodesMap.keySet(), targetNodesMap.keySet())){
            gpsTrip.addNodeMaps(startNodesMap, targetNodesMap);
            trips.add(gpsTrip);
            
        }else{
            zeroLenghtTripsCount++;
       }
    }
}
 
    
  