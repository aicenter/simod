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
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.AliteEntity;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.Common;
import cz.cvut.fel.aic.amodsim.storage.DriveAgentStorage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
@Singleton
public class Statistics extends AliteEntity implements EventHandler{
    
    private static final int TRANSIT_OUTPUT_BATCH_SIZE = 1000000;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Statistics.class);
    
    private final EventProcessor eventProcessor;
    
    private final LinkedList<Double> averageEdgeLoad;
    
    private final Provider<DriveAgentEdgesLoadByState> allEdgesLoadProvider;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    private final DriveAgentStorage driveAgentStorage;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final LinkedList<HashMap<Integer,Integer>> allEdgesLoadHistory;
    
    private final HashMap<OnDemandVehicleState, LinkedList<HashMap<Integer,Integer>>> allEdgesLoadHistoryPerState;
    
    private final AmodsimConfig config;
    
    private final LinkedList<Long> vehicleLeftStationToServeDemandTimes;
    
    private final LinkedList<DemandServiceStatistic> demandServiceStatistics;
    
    private LinkedList<Transit> allTransit;
    
    private final CsvWriter transitWriter;
    
    private final HashMap<OnDemandVehicleEvent,LinkedList<OnDemandVehicleEventContent>> onDemandVehicleEvents;
    
    private final LinkedList<Integer> tripDistances;
	
    private final List<Map<String,Integer>> vehicleOccupancy;   
    
    private long tickCount;
    
    private int maxLoad;
    
    private double averageKmWithPassenger;
    
    private double averageKmToStartLocation;
    
    private double averageKmToStation;
    
    private double averageKmRebalancing;
    
    private int numberOfVehicles;
    
    
    
    
    @Inject
    public Statistics(TypedSimulation eventProcessor, Provider<DriveAgentEdgesLoadByState> allEdgesLoadProvider, 
            OnDemandVehicleStorage onDemandVehicleStorage, 
            DriveAgentStorage driveAgentStorage, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, AmodsimConfig config) throws IOException {
        this.eventProcessor = eventProcessor;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.driveAgentStorage = driveAgentStorage;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.config = config;
        allEdgesLoadHistory = new LinkedList<>();
        allEdgesLoadHistoryPerState = new HashMap<>();
        vehicleLeftStationToServeDemandTimes = new LinkedList<>();
        demandServiceStatistics = new LinkedList<>();
        allTransit = new LinkedList<>();
        onDemandVehicleEvents = new HashMap<>();
        tripDistances = new LinkedList<>();
		vehicleOccupancy = new LinkedList<>();
        transitWriter = new CsvWriter(
                    Common.getFileWriter(config.amodsim.statistics.transitStatisticFilePath));
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
		init(eventProcessor);
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
                case DEMAND_DROPPED_OFF:
                    handleDemandDropoff((DemandServiceStatistic) event.getContent());
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
                config.amodsim.statistics.statisticIntervalMilis);
    }

    private void measure() {
        countEdgeLoadForInterval();
		countVehicleOccupancyForInterval();
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
            mapper.writeValue(new File(config.amodsim.statistics.resultFilePath), result);
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }


    public void simulationFinished() {
        countAveragesFromAgents();
        saveResult();
        saveAllEdgesLoadHistory();
        saveTransit();
        saveOnDemandVehicleEvents();
        saveDistances();
		saveOccupancies();
		saveServiceStatistics();
        try {
            transitWriter.close();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }

	@Override
	protected List<Enum> getEventTypesToHandle() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		typesToHandle.add(OnDemandVehicleEvent.START_REBALANCING);
		typesToHandle.add(OnDemandVehicleEvent.FINISH_REBALANCING);
		typesToHandle.add(OnDemandVehicleEvent.LEAVE_STATION);
		typesToHandle.add(OnDemandVehicleEvent.REACH_NEAREST_STATION);
		typesToHandle.add(DriveEvent.VEHICLE_ENTERED_EDGE);
		typesToHandle.add(StatisticEvent.DEMAND_DROPPED_OFF);
		return typesToHandle;
	}
	
	

    @Inject
    private void countEdgeLoadForInterval() {
        //EdgesLoadByState allEdgesLoad = allEdgesLoadProvider.get();
        DriveAgentEdgesLoadByState allEdgesLoad = allEdgesLoadProvider.get();
        
        if(tickCount % (config.amodsim.statistics.allEdgesLoadIntervalMilis 
				/ config.amodsim.statistics.statisticIntervalMilis) == 0){
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
        
        //numberOfVehicles = onDemandVehicleStorage.getEntities().size();
        numberOfVehicles = driveAgentStorage.getEntities().size();
        
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
            mapper.writeValue(new File(config.amodsim.statistics.allEdgesLoadHistoryFilePath), outputMap);
        } catch (IOException ex) {
            LOGGER.error(null, ex);
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
            LOGGER.error(null, ex);
        }
    }
    
    private void saveDistances() {
        try {
            CsvWriter writer = new CsvWriter(
                    Common.getFileWriter(config.amodsim.statistics.tripDistancesFilePath));
            for (Integer distance : tripDistances) {
                writer.writeLine(Integer.toString(distance));
            }
            writer.close();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }
	
	private void saveOccupancies() {
        try {
            CsvWriter writer = new CsvWriter(
                    Common.getFileWriter(config.amodsim.statistics.occupanciesFilePath));
			int period = 0;
            for (Map<String,Integer> occupanciesInPeriod: vehicleOccupancy) {
				for(Map.Entry<String,Integer> entry: occupanciesInPeriod.entrySet()){
					writer.writeLine(Integer.toString(period), entry.getKey(), Integer.toString(entry.getValue()));
				}
				period++;
            }
            writer.close();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }
	
	private void saveServiceStatistics() {
        try {
            CsvWriter writer = new CsvWriter(
                    Common.getFileWriter(config.amodsim.statistics.serviceFilePath));
            for (DemandServiceStatistic demandServiceStatistic: demandServiceStatistics) {
				writer.writeLine(Long.toString(demandServiceStatistic.getDemandTime()), 
						demandServiceStatistic.getDemandId(), demandServiceStatistic.getVehicleId(), 
						Long.toString(demandServiceStatistic.getPickupTime()), 
						Long.toString(demandServiceStatistic.getDropoffTime()), 
						Long.toString(demandServiceStatistic.getMinPossibleServiceDelay()));
            }
            writer.close();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
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
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.leaveStationFilePath;
                    break;
                case PICKUP:
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.pickupFilePath;
                    break;
                case DROP_OFF:
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.dropOffFilePath;
                    break;
                case REACH_NEAREST_STATION:
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.reachNearestStationFilePath;
                    break;
                case START_REBALANCING:
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.startRebalancingFilePath;
                    break;
                case FINISH_REBALANCING:
                    filepath = config.amodsim.statistics.onDemandVehicleStatistic.finishRebalancingFilePath;
                    break;
            }
            
            try {
                CsvWriter writer = new CsvWriter(Common.getFileWriter(filepath));
                for (OnDemandVehicleEventContent event : events) {
                    writer.writeLine(Long.toString(event.getTime()), Long.toString(event.getId()));
                }
                writer.close();
            } catch (IOException ex) {
                LOGGER.error(null, ex);
            }
        }
    }

	private void countVehicleOccupancyForInterval() {
		Map<String,Integer> occupancies = new HashMap<>();
		for(OnDemandVehicle onDemandVehicle: onDemandVehicleStorage){
			if(onDemandVehicle.getState() != OnDemandVehicleState.WAITING 
					&& onDemandVehicle.getState() != OnDemandVehicleState.REBALANCING){
				occupancies.put(onDemandVehicle.getId(), onDemandVehicle.getVehicle().getTransportedEntities().size());
			}
		}
		vehicleOccupancy.add(occupancies);
	}

	private void handleDemandDropoff(DemandServiceStatistic demandServiceStatistic) {
		demandServiceStatistics.add(demandServiceStatistic);
	}

}
