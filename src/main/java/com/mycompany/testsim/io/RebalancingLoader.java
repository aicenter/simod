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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    
    
    
    
    private final List<OnDemandVehicleStation> onDemandVehicleStations;
    
    private final List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips;
    
    private final OnDemandVehicleStationFactory onDemandVehicleStationFactory;

    
    
    
    public List<OnDemandVehicleStation> getOnDemandVehicleStations() {
        return onDemandVehicleStations;
    }

    public List<TimeTrip<OnDemandVehicleStation>> getRebalancingTrips() {
        return rebalancingTrips;
    }

    
    
    
    @Inject
    public RebalancingLoader(OnDemandVehicleStationFactory onDemandVehicleStationFactory) {
        this.onDemandVehicleStationFactory = onDemandVehicleStationFactory;
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
            onDemandVehicleStations.add(onDemandVehicleStationFactory.create(Integer.toString(i), 
                    (double) station.get(0), (double) station.get(1), (int) initialVehicleCount.get(i)));
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
							rebalancingTrips.add(new TimeTrip<>(onDemandVehicleStations.get(j), onDemandVehicleStations.get(k),
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
