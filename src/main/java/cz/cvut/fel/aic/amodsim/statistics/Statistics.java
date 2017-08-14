/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

import cz.cvut.fel.aic.agentpolis.simmodel.eventType.Transit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.eventType.DriveEvent;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.Config;
import cz.cvut.fel.aic.amodsim.io.Common;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
@Singleton
public class Statistics extends EventHandlerAdapter implements SimulationFinishedListener {
    
    private static final int TRANSIT_OUTPUT_BATCH_SIZE = 1000000;
    
    private final EventProcessor eventProcessor;
    
    private final LinkedList<Double> averageEdgeLoad;
    
    private final Provider<EdgesLoadByState> allEdgesLoadProvider;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final LinkedList<HashMap<Integer,Integer>> allEdgesLoadHistory;
    
    private final HashMap<OnDemandVehicleState, LinkedList<HashMap<Integer,Integer>>> allEdgesLoadHistoryPerState;
    
    private final Config config;
    
    private final LinkedList<Long> vehicleLeftStationToServeDemandTimes;
    
    private final LinkedList<DemandServiceStatistic> demandServiceStatistics;
    
    private LinkedList<Transit> allTransit;
    
    private final CsvWriter transitWriter;
    
    private final HashMap<OnDemandVehicleEvent,LinkedList<OnDemandVehicleEventContent>> onDemandVehicleEvents;
    
    private final LinkedList<Integer> tripDistances;
   
    
    private long tickCount;
    
    private int maxLoad;
    
    private double averageKmWithPassenger;
    
    private double averageKmToStartLocation;
    
    private double averageKmToStation;
    
    private double averageKmRebalancing;
    
    private int numberOfVehicles;
    
    
    
    
    @Inject
    public Statistics(EventProcessor eventProcessor, Provider<EdgesLoadByState> allEdgesLoadProvider, 
            OnDemandVehicleStorage onDemandVehicleStorage, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, Config config) throws IOException {
        this.eventProcessor = eventProcessor;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.config = config;
        allEdgesLoadHistory = new LinkedList<>();
        allEdgesLoadHistoryPerState = new HashMap<>();
        vehicleLeftStationToServeDemandTimes = new LinkedList<>();
        demandServiceStatistics = new LinkedList<>();
        allTransit = new LinkedList<>();
        onDemandVehicleEvents = new HashMap<>();
        tripDistances = new LinkedList<>();
        transitWriter = new CsvWriter(
                    Common.getFileWriter(config.agentpolis.statistics.transitStatisticFilePath));
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            allEdgesLoadHistoryPerState.put(onDemandVehicleState, new LinkedList<>());
        }
        for(OnDemandVehicleEvent onDemandVehicleEvent : OnDemandVehicleEvent.values()){
            onDemandVehicleEvents.put(onDemandVehicleEvent, new LinkedList<>());
        }
        
        tickCount = 0;
        averageEdgeLoad = new LinkedList<>();
        maxLoad = 0;
        
        eventProcessor.addEventHandler(this);
    }
    
    
    
    public int getNumberOfVehiclsLeftStationToServeDemand(){
        return onDemandVehicleEvents.get(OnDemandVehicleEvent.LEAVE_STATION).size();
    }

    @Override
    public void handleEvent(Event event) {
        if(event.getType() instanceof StatisticEvent){
            switch((StatisticEvent) event.getType()){
                case TICK:
                    handleTick();
                    break;
            }
        }
        else if(event.getType() instanceof OnDemandVehicleEvent){
            OnDemandVehicleEvent onDemandVehicleEvent = (OnDemandVehicleEvent) event.getType();
            addOndDemandVehicleEvent(onDemandVehicleEvent, (OnDemandVehicleEventContent) event.getContent());
            if(onDemandVehicleEvent == OnDemandVehicleEvent.PICKUP){
                tripDistances.add(((PickupEventContent) event.getContent()).getDemandTripLength());
            }
        }
        else if(event.getType() instanceof DriveEvent){
            allTransit.add((Transit) event.getContent());
            if(allTransit.size() > TRANSIT_OUTPUT_BATCH_SIZE){
                saveTransit();
            }
        }
        
    }

    
    private void handleTick() {
        tickCount++;
        measure();
        eventProcessor.addEvent(StatisticEvent.TICK, this, null, null,
                config.agentpolis.statistics.statisticIntervalMilis);
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
                onDemandVehicleStationsCentral.getDemandsCount(), numberOfVehicles,
                onDemandVehicleStationsCentral.getNumberOfRebalancingDropped());
        
        ObjectMapper mapper = new ObjectMapper();
		
        try {
            mapper.writeValue(new File(config.agentpolis.statistics.resultFilePath), result);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void simulationFinished() {
        countAveragesFromAgents();
        saveResult();
        saveAllEdgesLoadHistory();
        saveTransit();
        saveOnDemandVehicleEvents();
        saveDistances();
        try {
            transitWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Inject
    private void countEdgeLoadForInterval() {
        EdgesLoadByState allEdgesLoad = allEdgesLoadProvider.get();
        
        if(tickCount % (config.agentpolis.statistics.allEdgesLoadIntervalMilis 
				/ config.agentpolis.statistics.statisticIntervalMilis) == 0){
            allEdgesLoadHistory.add(allEdgesLoad.getLoadPerEdge());
            for (Map.Entry<OnDemandVehicleState,HashMap<Integer, Integer>> stateEntry 
                    : allEdgesLoad.getEdgeLoadsPerState().entrySet()) {
                OnDemandVehicleState onDemandVehicleState = stateEntry.getKey();
                HashMap<Integer, Integer> loadPerState = stateEntry.getValue();
                allEdgesLoadHistoryPerState.get(onDemandVehicleState).add(loadPerState);
            }
            
        }
        
        int edgeLoadTotal = 0;
        
//        ArrayList<Integer> test = allEdgesLoad.test;
        
        for (Integer edgeLoad : (Iterable<Integer>) allEdgesLoad.loadsIterable) {
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
        
        numberOfVehicles = onDemandVehicleStorage.getEntities().size();
        
        averageKmWithPassenger = (double) metersWithPassengerSum / numberOfVehicles / 1000;
        averageKmToStartLocation = (double) metersToStartLocationSum / numberOfVehicles / 1000;
        averageKmToStation = (double) metersToStationSum / numberOfVehicles / 1000;
        averageKmRebalancing = (double) metersRebalancingSum / numberOfVehicles / 1000;
    }

    private void saveAllEdgesLoadHistory() {
        ObjectMapper mapper = new ObjectMapper();
        
        Map<String,Object> outputMap = new HashMap<>();
        outputMap.put("ALL", allEdgesLoadHistory);
        for (Map.Entry<OnDemandVehicleState, LinkedList<HashMap<Integer,Integer>>> stateEntry 
                : allEdgesLoadHistoryPerState.entrySet()) {
            outputMap.put(stateEntry.getKey().name(), stateEntry.getValue());
        }
		
        try {
            mapper.writeValue(new File(config.agentpolis.statistics.allEdgesLoadHistoryFilePath), outputMap);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void saveTransit() {
        try {
            for (Transit transit : allTransit) {
                transitWriter.writeLine(Long.toString(transit.getTime()), Long.toString(transit.getId()), 
                        Integer.toString(transit.getTripId()));
            }
            transitWriter.flush();
            allTransit = new LinkedList<>();
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void saveDistances() {
        try {
            CsvWriter writer = new CsvWriter(
                    Common.getFileWriter(config.agentpolis.statistics.tripDistancesFilePath));
            for (Integer distance : tripDistances) {
                writer.writeLine(Integer.toString(distance));
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addOndDemandVehicleEvent(OnDemandVehicleEvent onDemandVehicleEvent,
            OnDemandVehicleEventContent onDemandVehicleEventContent) {
        onDemandVehicleEvents.get(onDemandVehicleEvent).add(onDemandVehicleEventContent);
    }

    private void saveOnDemandVehicleEvents() {
        for(OnDemandVehicleEvent onDemandVehicleEvent : OnDemandVehicleEvent.values()){
            List<OnDemandVehicleEventContent> events = onDemandVehicleEvents.get(onDemandVehicleEvent);
            String filepath = null;
            switch(onDemandVehicleEvent){
                case LEAVE_STATION:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.leaveStationFilePath;
                    break;
                case PICKUP:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.pickupFilePath;
                    break;
                case DROP_OFF:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.dropOffFilePath;
                    break;
                case REACH_NEAREST_STATION:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.reachNearestStationFilePath;
                    break;
                case START_REBALANCING:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.startRebalancingFilePath;
                    break;
                case FINISH_REBALANCING:
                    filepath = config.agentpolis.statistics.onDemandVehicleStatistic.finishRebalancingFilePath;
                    break;
            }
            
            try {
                CsvWriter writer = new CsvWriter(Common.getFileWriter(filepath));
                for (OnDemandVehicleEventContent event : events) {
                    writer.writeLine(Long.toString(event.getTime()), Long.toString(event.getId()));
                }
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
}