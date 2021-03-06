/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod.tripUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.NodesMappedByIndex;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.jackson.MyModule;
import cz.cvut.fel.aic.graphimporter.util.MD5ChecksumGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fido
 */
@Singleton
public class TripsUtilCached extends TripsUtil {
		
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripsUtilCached.class);
	
	private static final int OUTPUT_BATCH_SIZE = 10000;

	private final HashMap<StartTargetNodePair, int[]> tripCache;

	private final File tripCacheFolder;
	
	private final ObjectMapper mapper;
	
	private final NodesMappedByIndex nodesMappedByIndex;
	
	
	private HashMap<StartTargetNodePair, int[]> newTrips;
	
	private int cacheFileCounter;
	
	private IdGenerator tripIdGenerator;


	
	
	@Inject
	public TripsUtilCached(ShortestPathPlanner pathPlanner, NearestElementUtils nearestElementUtils, 
			HighwayNetwork network, SimulationCreator simulationCreator, SimodConfig amodConfig, 
			AgentpolisConfig agentpolisConfig,NodesMappedByIndex nodesMappedByIndex,IdGenerator tripIdGenerator) throws IOException {
		super(pathPlanner, nearestElementUtils, network,tripIdGenerator);
		this.nodesMappedByIndex = nodesMappedByIndex;
		this.tripIdGenerator = tripIdGenerator;
		
		mapper = new ObjectMapper();
		mapper.registerModule(new MyModule());

		tripCacheFolder = getCacheFolder(amodConfig, agentpolisConfig);
		
		tripCache = new HashMap<>();
		if(tripCacheFolder.exists()){
			loadTripCache();
		}
		else{
			tripCacheFolder.mkdirs();
			cacheFileCounter = 0;
		}
                
		              
		newTrips = new HashMap<>();

	}


	
	
	@Override
	public VehicleTrip createTrip(SimulationNode startNode, SimulationNode targetNode, PhysicalVehicle vehicle) {
		StartTargetNodePair tripStartTargetPair = new StartTargetNodePair(startNode.getIndex(), targetNode.getIndex());

		VehicleTrip<SimulationNode> finalTrip = null;

		if (tripCache.containsKey(tripStartTargetPair)) {
			SimulationNode[] locations = Arrays.stream(tripCache.get(tripStartTargetPair))
					.mapToObj(item -> nodesMappedByIndex.getNodeByIndex(item))
					.toArray(SimulationNode[]::new);
			finalTrip =
					new VehicleTrip<>(tripIdGenerator.getId(),vehicle, locations);
		} else {
			finalTrip = super.createTrip(startNode, targetNode, vehicle);
			int[] locationIndexes 
					= Arrays.stream(finalTrip.getLocations()).map(node -> node.getIndex()).mapToInt(x -> x).toArray();
			tripCache.put(tripStartTargetPair, locationIndexes);
			newTrips.put(tripStartTargetPair, locationIndexes);
			if(newTrips.size() > OUTPUT_BATCH_SIZE){
				saveNewTrips();
			}
		}

		return finalTrip;
	}

	private void loadTripCache() throws IOException {
		TypeReference<HashMap<StartTargetNodePair, int[]>> typeRef =
				new TypeReference<HashMap<StartTargetNodePair, int[]>>() {
				};
		
		LOGGER.info("Loading cache start");
		for (final File file : tripCacheFolder.listFiles()) {
			HashMap<StartTargetNodePair, int[]> tripCachePart = mapper.readValue(file, typeRef);
			tripCache.putAll(tripCachePart);
			cacheFileCounter++;
		}
		LOGGER.info("Loading cache finished - {} trips loaded", tripCache.size());

//		LOGGER.info(mapper.getSerializationConfig().toString());
	}

	private File getCacheFolder(SimodConfig amodConfig, AgentpolisConfig agentpolisConfig) {
		String filename = amodConfig.tripCacheFile;
		String edgesFilename = agentpolisConfig.mapEdgesFilepath;
		String checksum = MD5ChecksumGenerator.getGraphChecksum(new File(edgesFilename));
		filename += checksum;
		if(amodConfig.simplifyGraph){
			filename += "-simplified";
		}
		return new File(filename);
	}

	public void saveNewTrips() {
		File outputFile = new File(tripCacheFolder + File.separator + cacheFileCounter + ".json");
		cacheFileCounter++;
		if(!newTrips.isEmpty()){
			try {
				mapper.writeValue(outputFile, newTrips);
				newTrips = new HashMap<>();
			} catch (IOException ex) {
				Logger.getLogger(TripsUtilCached.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
