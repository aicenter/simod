/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.WKTPrintableCoord;
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
	
//	private PathPlanner pathPlanner;
	
	private int zeroLenghtTripsCount = 0;
	
	private int sameStartAndTargetInDataCount = 0;
	
	private final Graph<SimulationNode,SimulationEdge> highwayGraph;
	
	private final NearestElementUtils nearestElementUtils;
	
	private IdGenerator tripIdGenerator;
	

	@Inject
	public TripTransform(HighwayNetwork highwayNetwork, NearestElementUtils nearestElementUtils,IdGenerator tripIdGenerator) {
		this.highwayGraph = highwayNetwork.getNetwork();
		this.nearestElementUtils = nearestElementUtils;
		this.tripIdGenerator = tripIdGenerator;
	}
	
	
	
	
	
	
//	public List<TimeTrip<Long>> gpsTripsToOsmNodeTrips(List<TimeTrip<GPSLocation>> gpsTrips, 
//			File osmFile, int srid){
//		return gpsTripsToOsmNodeTrips(gpsTrips, osmFile, srid, true);
//	}
	
//	public List<TimeTrip<Long>> gpsTripsToOsmNodeTrips(List<TimeTrip<GPSLocation>> gpsTrips, 
//			File osmFile, int srid, boolean completedTrips){
//		Graph<SimulationNode, SimulationEdge> highwayGraph = OsmUtil.getHigwayGraph(osmFile,srid);
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
//		LOGGER.info("Number of trips with same source and destination: " + sameStartAndTargetInDataCount);
//		LOGGER.info(zeroLenghtTripsCount + " trips with zero lenght discarded");
//		
//		return osmNodeTrips;
//	}
	
	public static <T extends WKTPrintableCoord> void tripsToJson(List<TimeTrip<T>> trips, File outputFile) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(outputFile, trips);
	}
	
	public static <T extends WKTPrintableCoord> List<TimeTrip<T>> jsonToTrips(File inputFile, Class<T> locationType) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		
		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
				List.class, typeFactory.constructParametricType(TimeTrip.class, locationType)));
	}
	
	public List<TimeTrip<SimulationNode>> loadTripsFromTxt(File inputFile){
		List<TimeTrip<GPSLocation>> gpsTrips = new LinkedList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String line;
			while ((line = br.readLine()) != null) {
			   String[] parts = line.split(" ");
			   GPSLocation startLocation
					   = new GPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, 0);
			   GPSLocation targetLocation
					   = new GPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, 0);
			   
			   if(startLocation.equals(targetLocation)){
				   sameStartAndTargetInDataCount++;
			   }
			   else{
					gpsTrips.add(
							new TimeTrip<>(tripIdGenerator.getId(),Long.parseLong(parts[0].split("\\.")[0]), startLocation, targetLocation));
			   }
			}
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
		
		List<TimeTrip<SimulationNode>> trips = new ArrayList<>();

		for (TimeTrip<GPSLocation> trip : ProgressBar.wrap(gpsTrips, "Process GPS trip: ")) {
			processGpsTrip(trip, trips);
		}
		
		LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
		LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);
		
		return trips; 
	}
	
	private void processGpsTrip(TimeTrip<GPSLocation> gpsTrip, List<TimeTrip<SimulationNode>>trips) {
		GPSLocation[] locations = gpsTrip.getLocations();
		SimulationNode startNode = nearestElementUtils.getNearestElement(locations[0], EGraphType.HIGHWAY);
		SimulationNode targetNode 
				= nearestElementUtils.getNearestElement(locations[locations.length - 1], EGraphType.HIGHWAY);
	
		if(startNode != targetNode){
			SimulationNode[] nodesList = new SimulationNode[2];
			nodesList[0] = startNode;
			nodesList[1] = targetNode;
			trips.add(new TimeTrip<>(tripIdGenerator.getId(),gpsTrip.getStartTime(), gpsTrip.getEndTime(), nodesList));
		}   
		else{
			zeroLenghtTripsCount++;
		}
	}
	
//	public void tripsFromTxtToJson(File inputFile, File osmFile, int srid, File outputFile) throws IOException{
//		List<TimeTrip<GPSLocation>> gpsTrips = new LinkedList<>();
//		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
//			String line;
//			while ((line = br.readLine()) != null) {
//			   String[] parts = line.split(" ");
//			   GPSLocation startLocation
//					   = new GPSLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0, 0);
//			   GPSLocation targetLocation
//					   = new GPSLocation(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), 0, 0);
//			   
//			   if(startLocation.equals(targetLocation)){
//				   sameStartAndTargetInDataCount++;
//			   }
//			   else{
//					gpsTrips.add(new TimeTrip<>(startLocation, targetLocation, 
//					   Long.parseLong(parts[0].split("\\.")[0])));
//			   }
//			}
//		} catch (IOException ex) {
//			LOGGER.error(null, ex);
//		}
//
//		tripsToJson(gpsTripsToOsmNodeTrips(gpsTrips, osmFile, srid, false), outputFile); 
//	}

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
//			osmNodeTrips.add(new TimeTrip<>(osmNodesList, trip.getStartTime(), trip.getEndTime()));
//		}   
//		else{
//			zeroLenghtTripsCount++;
//		}
//	}
	
}
