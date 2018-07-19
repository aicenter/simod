/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.tripUtil;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.jackson.MyModule;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.path.ShortestPathPlanners;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.slf4j.LoggerFactory;

/**
 * @author fido
 */
@Singleton
public class TripsUtilCached extends TripsUtil {
        
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripsUtilCached.class);
    
    private static final int OUTPUT_BATCH_SIZE = 10000;

    private final HashMap<StartTargetNodePair, SimpleJsonTrip> tripCache;

    private final File tripCacheFolder;
    
    private final ObjectMapper mapper;
    
    
    private HashMap<StartTargetNodePair, SimpleJsonTrip> newTrips;
    
    private int cacheFileCounter;

    private ShortestPathPlanner pathPlanner;


    @Inject
    public TripsUtilCached(ShortestPathPlanners pathPlanners, NearestElementUtils nearestElementUtils, 
            HighwayNetwork network, SimulationCreator simulationCreator, AmodsimConfig configuration) throws IOException {
        super(pathPlanners, nearestElementUtils, network);
        
        mapper = new ObjectMapper();
        mapper.registerModule(new MyModule());

        tripCacheFolder = getCacheFolder(configuration);
        
        tripCache = new HashMap<>();
        if(tripCacheFolder.exists()){
            loadTripCache();
        }
        else{
            tripCacheFolder.mkdir();
            cacheFileCounter = 0;
        }
        
        newTrips = new HashMap<>();

    }


    @Override
    public VehicleTrip createTrip(int startNodeId, int targetNodeId, PhysicalVehicle vehicle) {
        if (startNodeId == targetNodeId) {
            try {
                throw new Exception("Start node cannot be the same as end node");
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            }
        }

        ShortestPathPlanner pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES); 

        StartTargetNodePair tripStartTargetPair = new StartTargetNodePair(startNodeId, targetNodeId);

        VehicleTrip finalTrip = null;

        if (tripCache.containsKey(tripStartTargetPair)) {
            finalTrip =
                    new VehicleTrip(tripCache.get(tripStartTargetPair).getLocations(), EGraphType.HIGHWAY, vehicle.getId());
        } else {
            try {
                finalTrip = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
                tripCache.put(tripStartTargetPair, new SimpleJsonTrip(finalTrip.getLocations()));
                newTrips.put(tripStartTargetPair, new SimpleJsonTrip(finalTrip.getLocations()));
                if(newTrips.size() > OUTPUT_BATCH_SIZE){
                    saveNewTrips();
                }
            } catch (TripPlannerException ex) {
                LOGGER.error(null, ex);
            }
        }

        return finalTrip;
    }

    private void loadTripCache() throws IOException {
        TypeReference<HashMap<StartTargetNodePair, SimpleJsonTrip>> typeRef =
                new TypeReference<HashMap<StartTargetNodePair, SimpleJsonTrip>>() {
                };
        
        LOGGER.info("Loading cache start");
        for (final File file : tripCacheFolder.listFiles()) {
            HashMap<StartTargetNodePair, SimpleJsonTrip> tripCachePart = mapper.readValue(file, typeRef);
            tripCache.putAll(tripCachePart);
            cacheFileCounter++;
        }
        LOGGER.info("Loading cache finished - {} trips loaded", tripCache.size());

//        System.out.println(mapper.getSerializationConfig().toString());
    }

    private File getCacheFolder(AmodsimConfig config) {
        String filename = config.amodsim.tripCacheFile;
        if(config.amodsim.simplifyGraph){
            filename += "-simplified";
        }
        return new File(filename);
    }

    public void saveNewTrips() {
        File outputFile = new File(tripCacheFolder + File.separator + cacheFileCounter + ".json");
        cacheFileCounter++;
        try {
            mapper.writeValue(outputFile, newTrips);
            newTrips = new HashMap<>();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }

}
