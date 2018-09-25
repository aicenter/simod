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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private double totalValue = 0;
    private int totalTrips = 0;
    
//    //move to config
//    private final double maxTripLength = 25;
//    private final double minTripLength = 0.05;
    
    private final Graph<SimulationNode,SimulationEdge> highwayGraph;
    private final NearestElementUtils nearestElementUtils;

    public int getZeroLenghtTripsCount() {
        return zeroLenghtTripsCount;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public int getTotalTrips() {
        return totalTrips;
    }

    public int getSameStartAndTargetInDataCount() {
        return sameStartAndTargetInDataCount;
    }

    public Graph<SimulationNode, SimulationEdge> getHighwayGraph() {
        return highwayGraph;
    }
    
    @Inject
    public TripTransform(HighwayNetwork highwayNetwork, NearestElementUtils nearestElementUtils) {
        this.highwayGraph = highwayNetwork.getNetwork();
        this.nearestElementUtils = nearestElementUtils;
    }
       

	public static <T> void tripsToJson(List<TimeValueTrip<T>> trips, File outputFile) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(outputFile, trips);
	}
	
	public static <T> List<TimeValueTrip<T>> jsonToTrips(File inputFile, Class<T> locationType) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		
		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
				List.class, typeFactory.constructParametricType(TimeValueTrip.class, locationType)));
	}
    
    public List<TimeValueTrip<SimulationNode>> loadTripsFromTxt(File inputFile){

        List<TimeValueTrip<GPSLocation>> gpsTrips = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                GPSLocation startLocation
//                       = new GPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, 0);
                    = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, SRID);
                GPSLocation targetLocation
 //                      = new GPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, 0);
                 = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, SRID);
                
                if(startLocation.equals(targetLocation)){
                   sameStartAndTargetInDataCount++;
                }else{
                    if(parts.length == 6){
                        gpsTrips.add(new TimeValueTrip<>(totalTrips, startLocation, targetLocation, 
                        Long.parseLong(parts[0].split("\\.")[0]), Double.parseDouble(parts[5])));
                    }else{
                        gpsTrips.add(new TimeValueTrip<>(totalTrips, startLocation, targetLocation, 
                        Long.parseLong(parts[0].split("\\.")[0]), 0));
                    }
                }
                totalTrips++;
            }
        }catch (IOException ex) {
            LOGGER.error(null, ex);
        }
        List<TimeValueTrip<SimulationNode>> trips = new ArrayList<>();
        for (TimeValueTrip<GPSLocation> trip : ProgressBar.wrap(gpsTrips, "Process GPS trip: ")) {
                processGpsTrip(trip, trips);
        }
        
        LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
        LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);
        LOGGER.info("{} too long trips discarded", targetTooFarCount);
        LOGGER.info("{} trips with start node far away from graph discarded", startTooFarCount);
        LOGGER.info("{} trips with target node far away from graph  discarded", targetTooFarCount);
        return trips; 
    }
    
    
    private void processGpsTrip(TimeValueTrip<GPSLocation> gpsTrip, List<TimeValueTrip<SimulationNode>>trips) {
        List<GPSLocation> locations = gpsTrip.getLocations();
        GPSLocation startLocation = locations.get(0);
        GPSLocation targetLocation = locations.get(locations.size() - 1);
         //max ride length check
        double x = startLocation.getLongitudeProjected() - targetLocation.getLongitudeProjected();
        double y = startLocation.getLatitudeProjected() - targetLocation.getLatitudeProjected();
        if((x*x + y*y) > 25000*25000){
            tooLongCount++;
            return;
        }
        SimulationNode startNode = nearestElementUtils.getNearestElement(startLocation, EGraphType.HIGHWAY);
        x = startLocation.getLongitudeProjected() - startNode.getLongitudeProjected();
        y = startLocation.getLatitudeProjected() - startNode.getLatitudeProjected();
        if((x*x + y*y) > 50*50){
            startTooFarCount++;
            return;
        }
       
        SimulationNode targetNode = nearestElementUtils.getNearestElement(targetLocation, EGraphType.HIGHWAY);
        x = startLocation.getLongitudeProjected() - startNode.getLongitudeProjected();
        y = startLocation.getLatitudeProjected() - startNode.getLatitudeProjected();
        if((x*x + y*y) > 50*50){
            targetTooFarCount++;
            return;
        }
        double rideValue = gpsTrip.getRideValue();
	
	if(startNode != targetNode){
            LinkedList<SimulationNode> nodesList = new LinkedList<>();
            nodesList.add(startNode);
            nodesList.add(targetNode);
            //trips.add(new TimeValueTrip<>(gpsTrip.id, nodesList, gpsTrip.getStartTime(), gpsTrip.getEndTime(), rideValue ));
            trips.add(new TimeValueTrip<>(gpsTrip.id, nodesList, gpsTrip.getStartTime(), rideValue ));
            totalValue += rideValue;
        }else{
            zeroLenghtTripsCount++;
       }
    }
}
 
    
  