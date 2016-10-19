/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import com.mycompany.testsim.entity.OnDemandVehicleStation.OnDemandVehicleStationFactory;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fido
 */
@Singleton
public class RebalancingLoader {
    
    private static final int INTERVAL_COUNT = 140;
    
    private static final int MILIS_IN_DAY = 86400000;
    
    private static final int NUMBER_OF_LOCATIONS_TRIED_PER_STATION = 5;
    
    
    
    
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
            Node[] positionsInGraph = nearestElementUtils.getNearestElements(new GPSLocation((double) station.get(0), 
                    (double) station.get(1), 0, 0), EGraphType.HIGHWAY, NUMBER_OF_LOCATIONS_TRIED_PER_STATION);
            
            int j = 0;
            Node positionInGraph;
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
                            continue;
                        }
                        for (int l = 0; l < rebalancingTripsCount; l++) {
                            rebalancingTrips.add(new TimeTrip<>(onDemandVehicleStations.get(j), 
                                    onDemandVehicleStations.get(k), startTime));
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
