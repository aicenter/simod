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
package rebalancing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.RebalancingEventContent;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class ReactiveRebalancing implements Routine, EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReactiveRebalancing.class);
	
	private static final double NON_RABALANCING_COST = 100000000;
	
	
	private final PeriodicTicker ticker;
	
	private final SimodConfig config;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
	
	private final Map<OnDemandVehicleStation, Map<OnDemandVehicleStation, Double>> distancesBetweenStations;
	
	private final TravelTimeProvider travelTimeProvider;
	
	private final StationsDispatcher stationsDispatcher;
	
	private final Map<OnDemandVehicleStation,Integer> rebalancingOnWay;
	
	private final TypedSimulation eventProcessor;

	private GRBEnv env;


	@Inject
	public ReactiveRebalancing(PeriodicTicker ticker, SimodConfig config, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			TravelTimeProvider travelTimeProvider, StationsDispatcher stationsDispatcher, 
			TypedSimulation eventProcessor) {
		this.ticker = ticker;
		this.config = config;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.stationsDispatcher = stationsDispatcher;
		this.eventProcessor = eventProcessor;
		distancesBetweenStations = new HashMap<>();
		rebalancingOnWay = new HashMap<>();
		
		setEventHandeling();
		
		// gurobi environment init
		env = null;
		try {
			env = new GRBEnv(config.simodExperimentDir +"/log/mip-rebalancing.log");
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		LOGGER.info("Reactive rebalancing initialized.");
	}
	
	public void start(){
		computeDistancesBetweenStations();
		ticker.registerRoutine(this, config.rebalancing.period * 1000);	
	}
	
	

	@Override
	public void doRoutine() {
		LOGGER.info("Reactive Rebalancing - start");
		double averageFullness = computeAverageFullness();
		LinkedHashMap<OnDemandVehicleStation,Integer> compensations = computeCompensations(averageFullness);
		List<Transfer> transfers = computeTransfers(compensations);
		logTransferes(transfers);
		sendOrders(transfers);
		LOGGER.info("Reactive Rebalancing - finished");
	}
	
	
	/**
	 * Computes a car transfer to/from each station as optimalCarCountWithBuffer - carCount. Negative compensation
	 * means car transfer from station, while positive compensation means car transfer to station.
	 * @return Compensations for each station.
	 */
	private LinkedHashMap<OnDemandVehicleStation, Integer> computeCompensations(double averageFullness) {
		LinkedHashMap<OnDemandVehicleStation, Integer> compensations = new LinkedHashMap<>();
		
		for(OnDemandVehicleStation station: onDemandvehicleStationStorage){
			RebalancingOnDemandVehicleStation rebalancingStation = (RebalancingOnDemandVehicleStation) station;
			int carCount = station.getParkedVehiclesCount();
			int rebalancingOnWayToStation = rebalancingOnWay.containsKey(rebalancingStation) 
					? rebalancingOnWay.get(rebalancingStation) : 0;
			carCount += rebalancingOnWayToStation;
			int optimalCarCount = rebalancingStation.getOptimalCarCount();
			int targetCarCount = (int) Math.round(averageFullness * optimalCarCount);
			
			int excessBuffer = (int) (config.rebalancing.bufferExcess * optimalCarCount);
			int shortageBuffer = (int) (config.rebalancing.bufferShortage * optimalCarCount);
			
			int compensation = 0;
			if(carCount > targetCarCount){
				int targetCarCountWithBuffer = targetCarCount + excessBuffer;
				if(carCount > targetCarCountWithBuffer){
					compensation = targetCarCountWithBuffer - carCount;
				}
			}
			else{
				int targetCarCountWithBuffer = targetCarCount - shortageBuffer;
				if(carCount < targetCarCountWithBuffer){
					compensation = targetCarCountWithBuffer- carCount;
				}
			}
			
			if(compensation != 0){
				compensations.put(station, compensation);
			}
		}
		return compensations;
	}

	private List<Transfer> computeTransfers(LinkedHashMap<OnDemandVehicleStation, Integer> compensations) {
		
		try {
			// solver init
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
			// dictionaries
			Map<OnDemandVehicleStation,List<GRBVar>> toFlowVars = new HashMap<>();
			Map<GRBVar,Double> varCosts = new LinkedHashMap<>();
			Map<GRBVar,Transfer> variablesToTransfer = new LinkedHashMap<>();
			
			// variables
			for(Entry<OnDemandVehicleStation, Integer> compensationFrom: compensations.entrySet()){
				List<GRBVar> fromFlowVars = new LinkedList<>();

				OnDemandVehicleStation stationFrom = compensationFrom.getKey();
				int fromFlow = compensationFrom.getValue();

				// we filter only station with out flow (to many vehicles)
				if(fromFlow < 0){
					int amountFrom = Math.abs(fromFlow);
					
					// standard variables
					for(Entry<OnDemandVehicleStation, Integer> compensationTo: compensations.entrySet()){
						int amountTo = compensationTo.getValue();
						if(amountTo > 0){

							// variable
							int maxAmount = Math.min(amountFrom, amountTo);
							OnDemandVehicleStation stationTo = compensationTo.getKey();
							String flowName = String.format("flow from %s to %s", stationFrom.getId(), stationTo.getId());
							GRBVar flow = model.addVar(0, maxAmount, 0, GRB.INTEGER, flowName);

							fromFlowVars.add(flow);
							CollectionUtil.addToListInMap(toFlowVars, stationTo, flow);

							// variable costs
							try{
								double cost = distancesBetweenStations.get(stationFrom).get(stationTo);
								varCosts.put(flow, cost);

								// solution mappping
								variablesToTransfer.put(flow, new Transfer(stationFrom, stationTo));
							}
							catch(NullPointerException ex){
								LOGGER.debug("Exception when computing cost from {} to {}. Distances: {}", stationFrom, 
										stationTo, distancesBetweenStations);
							}
						}
					}
					
					// empty out flow variables
					String flowName = String.format("empty flow from %s", stationFrom.getId());
					GRBVar emptyFlowFrom = model.addVar(0, amountFrom, 0, GRB.INTEGER, flowName);
					fromFlowVars.add(emptyFlowFrom);
					varCosts.put(emptyFlowFrom, NON_RABALANCING_COST);

					// from flow sum constraint
					GRBLinExpr fromFlowSumConstraint = new GRBLinExpr();
					for(GRBVar variable: fromFlowVars){
						fromFlowSumConstraint.addTerm(1, variable);
					}
					String fromFlowSumConstraintName 
							= String.format("Constarint - sum of flows from station %s", stationFrom.getId());
					model.addConstr(fromFlowSumConstraint, GRB.EQUAL, amountFrom, fromFlowSumConstraintName);
				}
			}
		
			// to flow sum constraints
			for(Entry<OnDemandVehicleStation, Integer> compensationTo: compensations.entrySet()){
				int amountTo = compensationTo.getValue();
				if(amountTo > 0){
					OnDemandVehicleStation stationTo = compensationTo.getKey();

					GRBLinExpr toFlowSumConstraint = new GRBLinExpr();
					if(toFlowVars.containsKey(stationTo)){

						// empty in flow variables
						String flowName = String.format("empty flow to %s", stationTo.getId());
						GRBVar emptyFlowTo = model.addVar(0, amountTo, 0, GRB.INTEGER, flowName);
						toFlowSumConstraint.addTerm(1, emptyFlowTo);
						varCosts.put(emptyFlowTo, NON_RABALANCING_COST);

						for(GRBVar variable: toFlowVars.get(stationTo)){
							toFlowSumConstraint.addTerm(1, variable);
						}
					}
					String toFlowSumConstraintName
							= String.format("Constarint - sum of flows to station %s", stationTo.getId());
					model.addConstr(toFlowSumConstraint, GRB.EQUAL, amountTo, toFlowSumConstraintName);
				}
			}
		
			List<Transfer> transfers = new LinkedList<>();
		
			// rebalancing only make sense when there are some possible transfers
			if(!varCosts.isEmpty()){

				// objective function
				for(Entry<GRBVar,Double> varCost: varCosts.entrySet()){
					objetive.addTerm(varCost.getValue(), varCost.getKey());
				}

				// solving
				model.setObjective(objetive, GRB.MINIMIZE);
				LOGGER.info("solving start");
				model.optimize();
				LOGGER.info("solving finished");

				// feasibility check
				if(model.get(GRB.IntAttr.Status) == GRB.OPTIMAL){

					LOGGER.info("Objective function value: {}", model.get(GRB.DoubleAttr.ObjVal));

					// create output from solution

					for (Entry<GRBVar, Transfer> entry : variablesToTransfer.entrySet()) {
						GRBVar flow = entry.getKey();
						Transfer transfer = entry.getValue();

						transfer.amount = (int) flow.get(GRB.DoubleAttr.X);
						transfers.add(transfer);
					}
				}
				else{
					LOGGER.info("Solution Infeasible");
				}
			}
		
			return transfers;
		} catch (GRBException ex) {
			Logger.getLogger(ReactiveRebalancing.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}
	
	private void computeDistancesBetweenStations() {
		ProgressBar.wrap(onDemandvehicleStationStorage.stream().parallel(), "Computing distances between stations")
				.forEach(station -> computeDistancesFromStation(station));
	}

	private void sendOrders(List<Transfer> transfers) {
//		for(Transfer transfer: transfers){
//			for(int i = 0; i < transfer.amount; i++){
//				stationsDispatcher.rebalance(transfer.from, transfer.to);
//			}
//		}
		for(Transfer transfer: transfers){
			if(transfer.amount > 0){
				stationsDispatcher.createBulkDelaydRebalancing(transfer.from, transfer.to, transfer.amount, 
					config.rebalancing.period * 1000);	
			}
		}
	}

	private double computeAverageFullness() {
		double fullnessSum = 0;
		for(OnDemandVehicleStation station: onDemandvehicleStationStorage){
			RebalancingOnDemandVehicleStation rebalancingStation = (RebalancingOnDemandVehicleStation) station;
			int carCount = station.getParkedVehiclesCount();
			int optimalCarCount = rebalancingStation.getOptimalCarCount();
			fullnessSum += (double) carCount / optimalCarCount;
		}
		
		return fullnessSum / onDemandvehicleStationStorage.size();
	}

	private void logTransferes(List<Transfer> transfers) {
		boolean used = false;
		StringBuilder sb = new StringBuilder();
		for (Transfer transfer : transfers) {
			if(transfer.amount > 0){
				sb.append(String.format("%s cars rom %s to %s\n", transfer.amount, transfer.from, transfer.to));
				used = true;
			}
		}
		if(used){
			sb.insert(0, "Computed transfers:\n");
		}
		else{
			sb.append("No transfers");
		}
		LOGGER.info(sb.toString());
	}

	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
		OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
		RebalancingEventContent eventContent = (RebalancingEventContent) event.getContent();
		
		OnDemandVehicleStation stationTo = eventContent.to;
		
		if(eventType == OnDemandVehicleEvent.START_REBALANCING){
			CollectionUtil.incrementMapValue(rebalancingOnWay, stationTo, 1);
		}
		else if(eventType == OnDemandVehicleEvent.FINISH_REBALANCING){
			CollectionUtil.incrementMapValue(rebalancingOnWay, stationTo, -1);
		}
	}
	
	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.START_REBALANCING);
		typesToHandle.add(OnDemandVehicleEvent.FINISH_REBALANCING);
		eventProcessor.addEventHandler(this, typesToHandle);
	}

	private void computeDistancesFromStation(OnDemandVehicleStation stationFrom) {
		Map<OnDemandVehicleStation,Double> mapFromStation = new HashMap<>();
		distancesBetweenStations.put(stationFrom, mapFromStation);
		for(OnDemandVehicleStation stationTo: onDemandvehicleStationStorage){
			if(stationFrom != stationTo){
				double distance = travelTimeProvider.getExpectedTravelTime(
						stationFrom.getPosition(), stationTo.getPosition());
				mapFromStation.put(stationTo, distance);
			}
//				LOGGER.info("Computing distance from station {} to station {}", stationFrom, stationTo);
		}
	}
	
}
