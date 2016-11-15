/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mycompany.testsim.OnDemandVehicleStationsCentral;
import com.mycompany.testsim.entity.OnDemandVehicle;
import com.mycompany.testsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.AllEdgesLoad;
import cz.agents.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
@Singleton
public class Statistics extends EventHandlerAdapter implements SimulationFinishedListener {
    
    public static final long STATISTIC_INTERVAL = 60000;
    
    private static final File RESULT_FILE = new File("result.json");
    
    public static final long ALL_EDGES_LOAD_INTERVAL = 600000;
    
    private static final File ALL_EDGES_LOAD_HISTORY_FILE = new File("allEdgesLoadHistory.json");
    
    
    
    private final EventProcessor eventProcessor;
    
    private final LinkedList<Double> averageEdgeLoad;
    
    private final Provider<AllEdgesLoad> allEdgesLoadProvider;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final LinkedList<HashMap<String, Integer>> allEdgesLoadHistory;
    
    
    private long tickCount;
    
    private int maxLoad;
    
    private double averageKmWithPassenger;
    
    private double averageKmToStartLocation;
    
    private double averageKmToStation;
    
    private double averageKmRebalancing;
    
    
    
    
    
    @Inject
    public Statistics(EventProcessor eventProcessor, Provider<AllEdgesLoad> allEdgesLoadProvider, 
            OnDemandVehicleStorage onDemandVehicleStorage, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral) {
        this.eventProcessor = eventProcessor;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        allEdgesLoadHistory = new LinkedList<>();
        tickCount = 0;
        averageEdgeLoad = new LinkedList<>();
        maxLoad = 0;
    }
    
    
    

    @Override
    public void handleEvent(Event event) {
        handleTick();
    }

    
    private void handleTick() {
        tickCount++;
        measure();
        eventProcessor.addEvent(this, STATISTIC_INTERVAL);
    }

    private void measure() {
        countEdgeLoadForInterval();
    }
    
    
    private void saveResult(){
        double averageLoadTotal = countAverageEdgeLoad();
        
        Result result = new Result(tickCount, averageLoadTotal, maxLoad, averageKmWithPassenger, 
                averageKmToStartLocation, averageKmToStation, averageKmRebalancing, 
                onDemandVehicleStationsCentral.getNumberOfDemandsNotServedFromNearestStation(), 
                onDemandVehicleStationsCentral.getNumberOfDemandsDropped(), 
                onDemandVehicleStationsCentral.getDemandsCount());
        
        ObjectMapper mapper = new ObjectMapper();
		
        try {
            mapper.writeValue(RESULT_FILE, result);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void simulationFinished() {
        countAveragesFromAgents();
        saveResult();
        saveAllEdgesLoadHistory();
    }

    @Inject
    private void countEdgeLoadForInterval() {
        AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();
        
        if(tickCount % ALL_EDGES_LOAD_INTERVAL / STATISTIC_INTERVAL == 0){
            allEdgesLoadHistory.add(allEdgesLoad.getLoadPerEdge());
        }
        
        int edgeLoadTotal = 0;
        for (int edgeLoad : allEdgesLoad.loadsIterator) {
            edgeLoadTotal += edgeLoad;
            if(edgeLoad > maxLoad){
                maxLoad = edgeLoad;
            }
        }
        averageEdgeLoad.add((double) edgeLoadTotal / allEdgesLoad.getNumberOfEdges());
    }

    private double countAverageEdgeLoad() {
        double totalAverageLoad = 0;
        for (double loadForFrame : averageEdgeLoad) {
            totalAverageLoad += loadForFrame;
        }
        return totalAverageLoad / averageEdgeLoad.size();
    }

    private void countAveragesFromAgents() {
        long metersWithPassengerSum = 0;
        long metersToStartLocationSum = 0;
        long metersToStationSum = 0;
        long metersRebalancingSum = 0;
        
        
        for (OnDemandVehicle onDemandVehicle : onDemandVehicleStorage) {
            metersWithPassengerSum += onDemandVehicle.getMetersWithPassenger();
            metersToStartLocationSum += onDemandVehicle.getMetersToStartLocation();
            metersToStationSum += onDemandVehicle.getMetersToStation();
            metersRebalancingSum += onDemandVehicle.getMetersRebalancing();
        }
        
        int numberOfVehicles = onDemandVehicleStorage.getEntityIds().size();
        
        averageKmWithPassenger = (double) metersWithPassengerSum / numberOfVehicles / 1000;
        averageKmToStartLocation = (double) metersToStartLocationSum / numberOfVehicles / 1000;
        averageKmToStation = (double) metersToStationSum / numberOfVehicles / 1000;
        averageKmRebalancing = (double) metersRebalancingSum / numberOfVehicles / 1000;
    }

    private void saveAllEdgesLoadHistory() {
        ObjectMapper mapper = new ObjectMapper();
		
        try {
            mapper.writeValue(ALL_EDGES_LOAD_HISTORY_FILE, allEdgesLoadHistory);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
}
