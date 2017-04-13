/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.statistics;

import cz.agents.agentpolis.simmodel.eventType.Transit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.eventType.DriveEvent;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.CsvWriter;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.io.Common;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
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
        transitWriter = new CsvWriter(
                    Common.getFileWriter(config.agentpolis.statistics.transitStatisticFilePath));
        for(OnDemandVehicleState onDemandVehicleState : OnDemandVehicleState.values()){
            allEdgesLoadHistoryPerState.put(onDemandVehicleState, new LinkedList<>());
        }
        tickCount = 0;
        averageEdgeLoad = new LinkedList<>();
        maxLoad = 0;
        
        eventProcessor.addEventHandler(this);
    }
    
    
    
    public int getNumberOfVehiclsLeftStationToServeDemand(){
        return vehicleLeftStationToServeDemandTimes.size();
    }

    @Override
    public void handleEvent(Event event) {
        if(event.getType() instanceof StatisticEvent){
            switch((StatisticEvent) event.getType()){
                case TICK:
                    handleTick();
                    break;
                case VEHICLE_LEFT_STATION_TO_SERVE_DEMAND:
                    vehicleLeftStationToServeDemandTimes.add((Long) event.getContent());
                    break;
                case DEMAND_PICKED_UP:
                    demandServiceStatistics.add((DemandServiceStatistic) event.getContent());
            }
        }
        else if(event.getType() instanceof OnDemandVehicleEvent){
            switch((OnDemandVehicleEvent) event.getType()){
                case LEAVE_STATION:
                    vehicleLeftStationToServeDemandTimes.add((Long) event.getContent());
                    break;
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
        saveDepartures();
        saveServiceStatistics();
        saveTransit();
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

    private void saveDepartures() {
        try {
            CsvWriter writer = new CsvWriter(
                    new FileWriter(config.agentpolis.statistics.carLeftStationToServeDemandTimesFilePath));
            for (long departureTime : vehicleLeftStationToServeDemandTimes) {
                writer.writeLine(Long.toString(departureTime));
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void saveServiceStatistics() {
        try {
            CsvWriter writer = new CsvWriter(
                    new FileWriter(config.agentpolis.statistics.demandServiceStatisticFilePath));
            for (DemandServiceStatistic demandServiceStatistic : demandServiceStatistics) {
                writer.writeLine(Long.toString(demandServiceStatistic.getDemandTime()),
                        Long.toString(demandServiceStatistic.getPickupTime()));
            }
            writer.close();
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
    
    
}
