/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.tripUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.amodsim.entity.DemandAgent;
import cz.agents.amodsim.jackson.MyModule;
import cz.agents.amodsim.statistics.Statistics;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanners;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.amodsim.config.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author fido
 */
@Singleton
public class TripsUtilCached extends TripsUtil implements SimulationFinishedListener {

    private final HashMap<StartTargetNodePair, Trip<TripItem>> tripCache;

    private static File tripCacheFile;


    @Inject
    public TripsUtilCached(ShortestPathPlanners pathPlanners, SimulationCreator simulationCreator, Config configuration) {
        super(pathPlanners);

        tripCacheFile = new File(configuration.agentpolis.tripCacheFile);
        
        if(tripCacheFile.exists()){
             tripCache = loadTripCache();
        }
        else{
            tripCache = new HashMap<>();
        }
        
        simulationCreator.addSimulationFinishedListener(this);
    }


    @Override
    public VehicleTrip createTrip(int startNodeId, int targetNodeId, Vehicle vehicle) {
        if (startNodeId == targetNodeId) {
            try {
                throw new Exception("Start node cannot be the same as end node");
            } catch (Exception ex) {
                Logger.getLogger(TripsUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (pathPlanner == null) {
            pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES);
        }


        StartTargetNodePair tripStartTargetPair = new StartTargetNodePair(startNodeId, targetNodeId);

        VehicleTrip finalTrip = null;

        if (tripCache.containsKey(tripStartTargetPair)) {
            finalTrip =
                    new VehicleTrip(tripCache.get(tripStartTargetPair).getLocations(), EGraphType.HIGHWAY, vehicle.getId());
        } else {
            try {
                finalTrip = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
                tripCache.put(tripStartTargetPair, new Trip<>(finalTrip.getLocations()));
            } catch (TripPlannerException ex) {
                Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return finalTrip;
    }

    @Override
    public void simulationFinished() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule());


        try {
            mapper.writeValue(tripCacheFile, tripCache);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HashMap<StartTargetNodePair, Trip<TripItem>> loadTripCache() {
        HashMap<StartTargetNodePair, Trip<TripItem>> tripCache = null;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new MyModule());

        System.out.println(mapper.getSerializationConfig().toString());

        TypeReference<HashMap<StartTargetNodePair, SimpleJsonTrip>> typeRef =
                new TypeReference<HashMap<StartTargetNodePair, SimpleJsonTrip>>() {
                };

        try {
            tripCache = mapper.readValue(tripCacheFile, typeRef);
        } catch (IOException ex) {
            Logger.getLogger(TripsUtilCached.class.getName()).log(Level.SEVERE, null, ex);
        }

        return tripCache;
    }


}
