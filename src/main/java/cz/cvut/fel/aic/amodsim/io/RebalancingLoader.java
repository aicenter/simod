/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStationFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
@Singleton
public class RebalancingLoader {
    
    private static final int INTERVAL_COUNT = 140;
    
    private static final int MILIS_IN_DAY = 86400000;
    
    private static final int NUMBER_OF_LOCATIONS_TRIED_PER_STATION = 5;
    
    private static final int REBALANCING_INTERVAL = 600000;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RebalancingLoader.class);
    
    
    private final List<OnDemandVehicleStation> onDemandVehicleStations;
    
    private final List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips;
    
    private final OnDemandVehicleStationFactory onDemandVehicleStationFactory;
    
    private final NearestElementUtils nearestElementUtils;
    

    
    
    
    public List<OnDemandVehicleStation> getOnDemandVehicleStations() {
        return onDemandVehicleStations;
    }

    public List<TimeTrip<OnDemandVehicleStation>> getRebalancingTrips() {
        return rebalancingTrips;
    }

    
    
    
    @Inject
    public RebalancingLoader(OnDemandVehicleStationFactory onDemandVehicleStationFactory, 
            NearestElementUtils nearestElementUtils) {
        this.onDemandVehicleStationFactory = onDemandVehicleStationFactory;
        this.nearestElementUtils = nearestElementUtils;
        this.onDemandVehicleStations = new ArrayList<>();
        this.rebalancingTrips = new ArrayList<>();
    }
    
    
    
    
    
    public void load(File file) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> data = mapper.readValue(file, Map.class);
        
        
        // stations
        ArrayList stations = (ArrayList) data.get("stations");
        ArrayList initialVehicleCount = (ArrayList) data.get("initial_vehicles");
        
        HashSet<Integer> usedPositions = new HashSet<>();
        for (int i = 0; i < stations.size(); i++) {
            ArrayList station = (ArrayList) stations.get(i);
            SimulationNode[] positionsInGraph = nearestElementUtils.getNearestElements(new GPSLocation((double) station.get(0),
                    (double) station.get(1), 0, 0), EGraphType.HIGHWAY, NUMBER_OF_LOCATIONS_TRIED_PER_STATION);
            
            int j = 0;
            SimulationNode positionInGraph;
            do{
                positionInGraph = positionsInGraph[j];
                if(!usedPositions.contains(positionInGraph.getId())){
                    usedPositions.add(positionInGraph.getId());
                    break;
                }
                j++;
            }while (j < positionsInGraph.length);
            
            
            onDemandVehicleStations.add(onDemandVehicleStationFactory.create(Integer.toString(i), positionInGraph, 
                    (int) initialVehicleCount.get(i)));
        }
        
        // rebalancing
        ArrayList rebalancingTimes = (ArrayList) data.get("rebalancing");
        
        for (int i = 0; i < rebalancingTimes.size(); i++) {
            ArrayList rebalancingStations = (ArrayList) rebalancingTimes.get(i);
            for (int j = 0; j < rebalancingStations.size(); j++) {
                ArrayList rebalancingTargetStations = (ArrayList) rebalancingStations.get(j);
                long startTime = computeStartTime(i);
                for (int k = 0; k < rebalancingTargetStations.size(); k++) {
                    int rebalancingTripsCount = (int) rebalancingTargetStations.get(k);
                    if(rebalancingTripsCount > 0){
                        // hack for the rebalancing with identical from to
                        if(j == k){
                            LOGGER.warn("Cannot rebalance to the same station (station number: {}" + 
                                    "interval: {}", k, i);
                            continue;
                        }
                        
                        int intervalBetweenCars = REBALANCING_INTERVAL / rebalancingTripsCount;
                        long finalStartTime = startTime;
                        
						rebalancingTripsCount = (int) ((double) rebalancingTripsCount / 4);
                        for (int l = 0; l < rebalancingTripsCount; l++) {
                            rebalancingTrips.add(new TimeTrip<>(onDemandVehicleStations.get(j), 
                                    onDemandVehicleStations.get(k), finalStartTime));
                            finalStartTime += intervalBetweenCars;
                        }
                    }
                }
            }
        }
    }

    private long computeStartTime(int interval) {
        return 1 + (MILIS_IN_DAY - 1) / INTERVAL_COUNT * interval;
    }
}
