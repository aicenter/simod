/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import cz.cvut.fel.aic.amodsim.OsmUtil;
//import cz.agents.amodsim.pathPlanner.PathPlanner;
import com.vividsolutions.jts.geom.Coordinate;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtilPair;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class TripTransform {
	
//	private PathPlanner pathPlanner;
    
    private int zeroLenghtTripsCount = 0;
    
    private int sameStartAndTargetInDataCount = 0;
    
    
    
	
//	public List<TimeTrip<Long>> gpsTripsToOsmNodeTrips(List<TimeTrip<GPSLocation>> gpsTrips, 
//			File osmFile, int srid){
//		return gpsTripsToOsmNodeTrips(gpsTrips, osmFile, srid, true);
//	}
	
//	public List<TimeTrip<Long>> gpsTripsToOsmNodeTrips(List<TimeTrip<GPSLocation>> gpsTrips, 
//			File osmFile, int srid, boolean completedTrips){
//        Graph<SimulationNode, SimulationEdge> highwayGraph = OsmUtil.getHigwayGraph(osmFile,srid);
//
//		List<NearestElementUtilPair<Coordinate, SimulationNode>> pairs = new ArrayList<>();
//		
//		for (SimulationNode roadNode : highwayGraph.getAllNodes()) {
//			pairs.add(new NearestElementUtilPair<>(new Coordinate(roadNode.getLongitude(), roadNode.getLatitude()), roadNode));
//		}
//		
//		NearestElementUtil<SimulationNode> nearestElementUtil = new NearestElementUtil<>(pairs, new Transformer(4326), 
//				new SimulationNodeArrayConstructor());
//		ArrayList<TimeTrip<Long>> osmNodeTrips = new ArrayList<>();
//		
//		if(completedTrips){
//			pathPlanner = new PathPlanner(highwayGraph);
//		}
//		
//		for (TimeTrip<GPSLocation> trip : gpsTrips) {
//			processGpsTrip(trip, nearestElementUtil, osmNodeTrips, completedTrips);
//		}
//        
//        System.out.println("Number of trips with same source and destination: " + sameStartAndTargetInDataCount);
//        System.out.println(zeroLenghtTripsCount + " trips with zero lenght discarded");
//		
//		return osmNodeTrips;
//	}
	
	public static <T> void tripsToJson(List<TimeTrip<T>> trips, File outputFile) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(outputFile, trips);
	}
	
	public static <T> List<TimeTrip<T>> jsonToTrips(File inputFile, Class<T> locationType) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		
		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
				List.class, typeFactory.constructParametricType(TimeTrip.class, locationType)));
	}
    
//    public void tripsFromTxtToJson(File inputFile, File osmFile, int srid, File outputFile) throws IOException{
//        List<TimeTrip<GPSLocation>> gpsTrips = new LinkedList<>();
//        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//               String[] parts = line.split(" ");
//               GPSLocation startLocation
//                       = new GPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, 0);
//               GPSLocation targetLocation
//                       = new GPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, 0);
//               
//               if(startLocation.equals(targetLocation)){
//                   sameStartAndTargetInDataCount++;
//               }
//               else{
//                    gpsTrips.add(new TimeTrip<>(startLocation, targetLocation, 
//                       Long.parseLong(parts[0].split("\\.")[0])));
//               }
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(TripTransform.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        tripsToJson(gpsTripsToOsmNodeTrips(gpsTrips, osmFile, srid, false), outputFile); 
//    }

//	private void processGpsTrip(TimeTrip<GPSLocation> trip, NearestElementUtil<RoadNode> nearestElementUtil, 
//			ArrayList<TimeTrip<Long>> osmNodeTrips, boolean completedTrips) {
//		LinkedList<Long> osmNodesList = new LinkedList<>();
//		
//		List<GPSLocation> locations = trip.getLocations();
//		long lastNodeId = nearestElementUtil.getNearestElement(locations.get(0)).getSourceId();
//		osmNodesList.add(lastNodeId);
//		
//		for (int i = 1; i < locations.size(); i++) {
//			RoadNode targetNode = nearestElementUtil.getNearestElement(locations.get(i));
//
//			long nodeId = targetNode.getSourceId();
//			if(nodeId != lastNodeId){
//				if(completedTrips){
//					List<Long> path = pathPlanner.findPath(lastNodeId, nodeId);
//					path.remove(0);
//					osmNodesList.addAll(path);
//				}
//				else{
//					osmNodesList.add(nodeId);
//				}
//				lastNodeId = nodeId;
//			}
//		}
//		if(osmNodesList.size() > 1){
//            osmNodeTrips.add(new TimeTrip<>(osmNodesList, trip.getStartTime(), trip.getEndTime()));
//        }   
//        else{
//            zeroLenghtTripsCount++;
//        }
//    }
	
}
