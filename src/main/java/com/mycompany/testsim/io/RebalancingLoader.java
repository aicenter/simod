/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.testsim.simulationon.OnDemandVehicleStation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fido
 */
public class RebalancingLoader {
    
    private static final int INTERVAL_COUNT = 140;
    
    private static final int MILIS_IN_DAY = 86400000;
    
    
    
    
    private final List<OnDemandVehicleStation> onDemandVehicleStations;
    
    private final List<Trip<OnDemandVehicleStation>> rebalancingTrips;

    
    
    
    public List<OnDemandVehicleStation> getOnDemandVehicleStations() {
        return onDemandVehicleStations;
    }

    public List<Trip<OnDemandVehicleStation>> getRebalancingTrips() {
        return rebalancingTrips;
    }

    
    
    
    
    public RebalancingLoader() {
        this.onDemandVehicleStations = new ArrayList<>();
        this.rebalancingTrips = new ArrayList<>();
    }
    
    
    
    
    
    public void load(File file) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> data = mapper.readValue(file, Map.class);
        
        ArrayList stations = (ArrayList) data.get("stations");
        ArrayList initialVehicleCount = (ArrayList) data.get("initial_vehicles");
        
        for (int i = 0; i < stations.size(); i++) {
            ArrayList station = (ArrayList) stations.get(i);
            onDemandVehicleStations.add(new OnDemandVehicleStation(Integer.toString(i), (double) station.get(0), 
                    (double) station.get(1), 0, 0, (int) initialVehicleCount.get(i)));
        }
        
        ArrayList rebalancingTimes = (ArrayList) data.get("rebalancing");
        
        for (int i = 0; i < rebalancingTimes.size(); i++) {
            ArrayList rebalancingStations = (ArrayList) rebalancingTimes.get(i);
            for (int j = 0; j < rebalancingStations.size(); j++) {
                ArrayList rebalancingTargetStations = (ArrayList) rebalancingStations.get(j);
                long startTime = computeStartTime(i);
                for (int k = 0; k < rebalancingTargetStations.size(); k++) {
                    int doRebalancingTrip = (int) rebalancingTargetStations.get(j);
                    if(doRebalancingTrip == 1){
                        rebalancingTrips.add(new Trip<>(onDemandVehicleStations.get(j), onDemandVehicleStations.get(k), 
                        startTime));
                    }
                }
            }
        }
    }

    private long computeStartTime(int interval) {
        return MILIS_IN_DAY / INTERVAL_COUNT * interval;
    }
}
