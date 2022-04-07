/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.eventType.DriveEvent;
import cz.cvut.fel.aic.agentpolis.simmodel.eventType.Transit;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.AliteEntity;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.CsvWriter;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.io.Common;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStats;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsIH;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStatsVGA;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	private static final int MILLION = 1000000;
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Statistics.class);
	
	private final EventProcessor eventProcessor;
	
	private final LinkedList<Double> averageEdgeLoad;
	
	private final Provider<EdgesLoadByState> allEdgesLoadProvider;
	
	private final OnDemandVehicleStorage onDemandVehicleStorage;
	
	private final StationsDispatcher onDemandVehicleStationsCentral;
	
	private final LinkedList<HashMap<BigInteger,Integer>> allEdgesLoadHistory;
	
	private final HashMap<OnDemandVehicleState, LinkedList<HashMap<BigInteger,Integer>>> allEdgesLoadHistoryPerState;
	
	private final SimodConfig config;
	
	private final LinkedList<Long> vehicleLeftStationToServeDemandTimes;
	
	private final LinkedList<DemandServiceStatistic> demandServiceStatistics;
	
	private LinkedList<TransitRecord> allTransit;
	
	private final CsvWriter transitWriter;
	
	private final HashMap<OnDemandVehicleEvent,LinkedList<OnDemandVehicleEventContent>> onDemandVehicleEvents;
	
	private final LinkedList<Integer> tripDistances;
	
	private final List<Map<String,Integer>> vehicleOccupancy;
	
	private final List<Long> darpSolverComputationalTimes;
	
	private final DARPSolver dARPSolver;
   
	
	private long tickCount;
	
	private int maxLoad;
	
	private double averageKmWithPassenger;
	
	private double averageKmToStartLocation;
	
	private double averageKmToStation;
	
	private double averageKmRebalancing;
	
	private int numberOfVehicles;
	
	private long totalDistanceWithPassenger;
	
	private long totalDistanceToStartLocation;
	
	private long totalDistanceToStation;
	
	private long totalDistanceRebalancing;
	
	
	
	
	@Inject
	public Statistics(TypedSimulation eventProcessor, Provider<EdgesLoadByState> allEdgesLoadProvider, 
			OnDemandVehicleStorage onDemandVehicleStorage, 
			StationsDispatcher onDemandVehicleStationsCentral, SimodConfig config, DARPSolver dARPSolver) throws IOException {
		this.eventProcessor = eventProcessor;
		this.allEdgesLoadProvider = allEdgesLoadProvider;
		this.onDemandVehicleStorage = onDemandVehicleStorage;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.config = config;
		this.dARPSolver = dARPSolver;
		allEdgesLoadHistory = new LinkedList<>();
		allEdgesLoadHistoryPerState = new HashMap<>();
		vehicleLeftStationToServeDemandTimes = new LinkedList<>();
		demandServiceStatistics = new LinkedList<>();
		allTransit = new LinkedList<>();
		onDemandVehicleEvents = new HashMap<>();
		tripDistances = new LinkedList<>();
		vehicleOccupancy = new LinkedList<>();
		transitWriter = new CsvWriter(
					Common.getFileWriter(config.statistics.transitStatisticFilePath));
		darpSolverComputationalTimes = new ArrayList<>();
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
			Transit transit = (Transit) event.getContent();
			TransitRecord record = new TransitRecord(transit.getTime(), transit.getId(),
					((OnDemandVehicle) transit.getAgent()).getState());
			allTransit.add(record);
			if(allTransit.size() > TRANSIT_OUTPUT_BATCH_SIZE){
				saveTransit();
			}
		}
		
	}

	public void addDarpSolverComputationalTime(long totalTimeNano){
		darpSolverComputationalTimes.add(totalTimeNano);
	}
	
	private void handleTick() {
		tickCount++;
		measure();
		eventProcessor.addEvent(StatisticEvent.TICK, this, null, null,
				config.statistics.statisticIntervalMilis);
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
				onDemandVehicleStationsCentral.getNumberOfRebalancingDropped(), totalDistanceWithPassenger,
		totalDistanceToStartLocation, totalDistanceToStation, totalDistanceRebalancing);
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			mapper.writeValue(new File(config.statistics.resultFilePath), result);
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
		if(onDemandVehicleStationsCentral instanceof RidesharingDispatcher){
			saveDarpSolverComputationalTimes();
		}
		if(config.ridesharing.on){
			saveRidesharingStatistics();
		}
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
		EdgesLoadByState allEdgesLoad = allEdgesLoadProvider.get();
		
		if(tickCount % (config.statistics.allEdgesLoadIntervalMilis 
				/ config.statistics.statisticIntervalMilis) == 0){
			allEdgesLoadHistory.add(allEdgesLoad.getLoadPerEdge());
			for (Map.Entry<OnDemandVehicleState,HashMap<BigInteger, Integer>> stateEntry 
					: allEdgesLoad.getEdgeLoadsPerState().entrySet()) {
				OnDemandVehicleState onDemandVehicleState = stateEntry.getKey();
				HashMap<BigInteger, Integer> loadPerState = stateEntry.getValue();
				allEdgesLoadHistoryPerState.get(onDemandVehicleState).add(loadPerState);
			}
			
		}
		
		int edgeLoadTotal = 0;
		
//		ArrayList<Integer> test = allEdgesLoad.test;
		
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
		totalDistanceWithPassenger = 0;
		totalDistanceToStartLocation = 0;
		totalDistanceToStation = 0;
		totalDistanceRebalancing = 0;
		
		
		for (OnDemandVehicle onDemandVehicle : onDemandVehicleStorage) {
			totalDistanceWithPassenger += onDemandVehicle.getMetersWithPassenger();
			totalDistanceToStartLocation += onDemandVehicle.getMetersToStartLocation();
			totalDistanceToStation += onDemandVehicle.getMetersToStation();
			totalDistanceRebalancing += onDemandVehicle.getMetersRebalancing();
		}
		
		numberOfVehicles = onDemandVehicleStorage.getEntities().size();
		
		averageKmWithPassenger = (double) totalDistanceWithPassenger / numberOfVehicles / 1000;
		averageKmToStartLocation = (double) totalDistanceToStartLocation / numberOfVehicles / 1000;
		averageKmToStation = (double) totalDistanceToStation / numberOfVehicles / 1000;
		averageKmRebalancing = (double) totalDistanceRebalancing / numberOfVehicles / 1000;
	}

	private void saveAllEdgesLoadHistory() {
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String,Object> outputMap = new HashMap<>();
		outputMap.put("ALL", allEdgesLoadHistory);
		for (Map.Entry<OnDemandVehicleState, LinkedList<HashMap<BigInteger,Integer>>> stateEntry 
				: allEdgesLoadHistoryPerState.entrySet()) {
			outputMap.put(stateEntry.getKey().name(), stateEntry.getValue());
		}
		
		try {
			mapper.writeValue(new File(config.statistics.allEdgesLoadHistoryFilePath), outputMap);
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}
	
	private void saveTransit() {
		try {
			for (TransitRecord transit : allTransit) {
				transitWriter.writeLine(Long.toString(transit.time), transit.staticId.toString(), 
						Integer.toString(transit.vehicleState.ordinal()));
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
					Common.getFileWriter(config.statistics.tripDistancesFilePath));
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
					Common.getFileWriter(config.statistics.occupanciesFilePath));
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
	
	private void saveDarpSolverComputationalTimes() {
		List<Long> times = ((RidesharingDispatcher) onDemandVehicleStationsCentral).getDarpSolverComputationalTimes();
		try {
			CsvWriter writer = new CsvWriter(
					Common.getFileWriter(config.statistics.darpSolverComputationalTimesFilePath));
			for (Long time : times) {
				writer.writeLine(Long.toString(time / MILLION));
			}
			transitWriter.flush();
			allTransit = new LinkedList<>();
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}
	
	private void saveServiceStatistics() {
		try {
			CsvWriter writer = new CsvWriter(
					Common.getFileWriter(config.statistics.serviceFilePath));
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
	
	private void saveRidesharingStatistics() {
                int highestGroup = 0;
		if(dARPSolver.getRidesharingStats().size() < 1){
			return;
		}
		try {
			CsvWriter writer = new CsvWriter(
					Common.getFileWriter(config.statistics.ridesharingFilePath));
			
			// header line
			if(config.ridesharing.method.equals("vga")){
				
				RidesharingBatchStatsVGA longestGroupStat 
						= (RidesharingBatchStatsVGA) dARPSolver.getRidesharingStats().get(0);
				for(RidesharingBatchStats stat: dARPSolver.getRidesharingStats()){
					RidesharingBatchStatsVGA statVga = (RidesharingBatchStatsVGA) stat;
					if(statVga.groupSizeData.length > longestGroupStat.groupSizeData.length){
						longestGroupStat = statVga;
					}
				}
				
                highestGroup = longestGroupStat.groupSizeData.length;
                                
				List<String> writerLine = new ArrayList(Arrays.asList("Batch", "New Request Count", "Active Request Count", 
						"Group Generation Time", "Solver Time", "Solver gap"));
				
				for(int i = 0; i < longestGroupStat.groupSizeData.length; i++){
					writerLine.add(String.format("%s Groups Count", i + 1));
					writerLine.add(String.format("%s Groups Total Time", i + 1));
				}
				for(int i = 0; i < longestGroupStat.groupSizeData.length; i++){
					writerLine.add(String.format("%s Feasible Groups Count", i + 1));
					writerLine.add(String.format("%s Feasible Groups Total Time", i + 1));
				}
				writer.writeLine(writerLine.toArray(new String[0]));
			}
			else{				
				writer.writeLine("Batch", "New Request Count", "Fail Fast Time", "Insertion Heuristic Time", 
						"Log Fail Time");
			}
			
			int batch = 0;
			for (RidesharingBatchStats ridesharingStat: dARPSolver.getRidesharingStats()) {
				if(config.ridesharing.method.equals("vga")){	
					RidesharingBatchStatsVGA vgaStat = (RidesharingBatchStatsVGA) ridesharingStat;
					
					List<String> writerLine = new ArrayList<>();
					writerLine.add(Integer.toString(batch));
					writerLine.add(Integer.toString(vgaStat.newRequestCount));
					writerLine.add(Integer.toString(vgaStat.activeRequestCount));
					writerLine.add(Integer.toString(vgaStat.groupGenerationTime));
					writerLine.add(Integer.toString(vgaStat.solverTime));
					writerLine.add(Double.toString(vgaStat.gap));
					
					for(int i = 0; i < highestGroup; i++){
						if(i < vgaStat.groupSizeDataPlanExists.length){
							writerLine.add(Integer.toString(vgaStat.groupSizeData[i].groupCount));
							writerLine.add(Integer.toString(vgaStat.groupSizeData[i].totalTime));
						}
						else{
							writerLine.add(Integer.toString(0));
							writerLine.add(Integer.toString(0));
						}
					}
					
					for(int i = 0; i < highestGroup; i++){
						if(i < vgaStat.groupSizeDataPlanExists.length){
							writerLine.add(Integer.toString(vgaStat.groupSizeDataPlanExists[i].groupCount));
							writerLine.add(Integer.toString(vgaStat.groupSizeDataPlanExists[i].totalTime));
						}
						else{
							writerLine.add(Integer.toString(0));
							writerLine.add(Integer.toString(0));
						}
					}
					
					writer.writeLine(writerLine.toArray(new String[writerLine.size()]));
				}
				else{
					RidesharingBatchStatsIH vgaStat = (RidesharingBatchStatsIH) ridesharingStat;
					
					writer.writeLine(Integer.toString(batch), Integer.toString(vgaStat.newRequestCount), 
					Integer.toString(vgaStat.failFastTime), Integer.toString(vgaStat.ihTime),
					Integer.toString(vgaStat.logFailTime));
				}
				batch++;
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
					filepath = config.statistics.onDemandVehicleStatistic.leaveStationFilePath;
					break;
				case PICKUP:
					filepath = config.statistics.onDemandVehicleStatistic.pickupFilePath;
					break;
				case DROP_OFF:
					filepath = config.statistics.onDemandVehicleStatistic.dropOffFilePath;
					break;
				case REACH_NEAREST_STATION:
					filepath = config.statistics.onDemandVehicleStatistic.reachNearestStationFilePath;
					break;
				case START_REBALANCING:
					filepath = config.statistics.onDemandVehicleStatistic.startRebalancingFilePath;
					break;
				case FINISH_REBALANCING:
					filepath = config.statistics.onDemandVehicleStatistic.finishRebalancingFilePath;
					break;
				case WAIT:
					filepath = config.statistics.onDemandVehicleStatistic.waitFilePath;
			}
			
			try {
				CsvWriter writer = new CsvWriter(Common.getFileWriter(filepath));
				for (OnDemandVehicleEventContent event : events) {
					writer.writeLine(Long.toString(event.getTime()), Long.toString(event.getDemandId()));
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
