/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.rebalancing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.ridesharing.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.HashMap;
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
public class ReactiveRebalancing implements Routine{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReactiveRebalancing.class);
	
	
	private final PeriodicTicker ticker;
	
	private final AmodsimConfig config;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
	
	private final Map<OnDemandVehicleStation, Map<OnDemandVehicleStation, Double>> distancesBetweenStations;
	
	private final AstarTravelTimeProvider astarTravelTimeProvider;
	
	private final StationsDispatcher stationsDispatcher;
	
	private GRBEnv env;

	@Inject
	public ReactiveRebalancing(PeriodicTicker ticker, AmodsimConfig config, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			AstarTravelTimeProvider astarTravelTimeProvider, StationsDispatcher stationsDispatcher) {
		this.ticker = ticker;
		this.config = config;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.astarTravelTimeProvider = astarTravelTimeProvider;
		this.stationsDispatcher = stationsDispatcher;
		distancesBetweenStations = new HashMap<>();
		
		// gurobi environment init
		env = null;
		try {
			env = new GRBEnv("mip-rebalancing.log");
		} catch (GRBException ex) {
			Logger.getLogger(GurobiSolver.class.getName()).log(Level.SEVERE, null, ex);
		}
		LOGGER.info("Reactive rebalancing initialized.");
	}
	
	public void start(){
		computeDistancesBetweenStations();
		ticker.registerRoutine(this, config.amodsim.amodsimRebalancing.period * 1000);	
	}
	
	

	@Override
	public void doRoutine() {
		LOGGER.info("Reactive Rebalancing - start");
		Map<OnDemandVehicleStation,Integer> compensations = computeCompensations();
		List<Transfer> transfers = computeTransfers(compensations);
		sendOrders(transfers);
		LOGGER.info("Reactive Rebalancing - finished");
	}

	private Map<OnDemandVehicleStation, Integer> computeCompensations() {
		Map<OnDemandVehicleStation, Integer> compensations = new HashMap<>();
		
		for(OnDemandVehicleStation station: onDemandvehicleStationStorage){
			RebalancingOnDemandVehicleStation rebalancingStation = (RebalancingOnDemandVehicleStation) station;
			int carCount = station.getParkedVehiclesCount();
			int optimalCarCount = rebalancingStation.getOptimalCarCount();
			
			int buffer = (int) (config.amodsim.amodsimRebalancing.buffer * optimalCarCount);
			
			int compensation = 0;
			if(carCount > optimalCarCount){
				int optimalCarCountWithBuffer = optimalCarCount + buffer;
				if(carCount > optimalCarCountWithBuffer){
					compensation = optimalCarCountWithBuffer- carCount;
				}
			}
			else{
				int optimalCarCountWithBuffer = optimalCarCount - buffer;
				if(carCount < optimalCarCountWithBuffer){
					compensation = optimalCarCountWithBuffer- carCount;
				}
			}
			
			if(compensation != 0){
				compensations.put(station, compensation);
			}
		}
		return compensations;
	}

	private List<Transfer> computeTransfers(Map<OnDemandVehicleStation, Integer> compensations) {
		
		try {
			// solver init
			GRBModel model = new GRBModel(env);
			GRBLinExpr objetive = new GRBLinExpr();
			
			// dictionaries
			Map<OnDemandVehicleStation,List<GRBVar>> toFlowVars = new HashMap<>();
			Map<GRBVar,Double> varCosts = new HashMap<>();
			Map<GRBVar,Transfer> variablesToTransfer = new HashMap<>();
			
			// variables
			for(Entry<OnDemandVehicleStation, Integer> compensationFrom: compensations.entrySet()){
				List<GRBVar> fromFlowVars = new LinkedList<>();

				OnDemandVehicleStation stationFrom = compensationFrom.getKey();
				int fromFlow = compensationFrom.getValue();

				if(fromFlow < 0){
					int amountFrom = Math.abs(fromFlow);
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
							double cost = distancesBetweenStations.get(stationFrom).get(stationTo);
							varCosts.put(flow, cost);

							// solution mappping
							variablesToTransfer.put(flow, new Transfer(stationFrom, stationTo));
						}
					}

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
				for(GRBVar variable: toFlowVars.get(stationTo)){
					toFlowSumConstraint.addTerm(1, variable);
				}
				String toFlowSumConstraintName
						= String.format("Constarint - sum of flows to station %s", stationTo.getId());
				model.addConstr(toFlowSumConstraint, GRB.EQUAL, amountTo, toFlowSumConstraintName);
			}
		}
			
		
		// objective function
		for(Entry<GRBVar,Double> varCost: varCosts.entrySet()){
			objetive.addTerm(varCost.getValue(), varCost.getKey());
		}
		
		// solving
		model.setObjective(objetive, GRB.MINIMIZE);
		LOGGER.info("solving start");
		model.optimize();
		LOGGER.info("solving finished");

		LOGGER.info("Objective function value: {}", model.get(GRB.DoubleAttr.ObjVal));
		
		// create output from solution
		List<Transfer> transfers = new LinkedList<>();
		for (Entry<GRBVar, Transfer> entry : variablesToTransfer.entrySet()) {
			GRBVar flow = entry.getKey();
			Transfer transfer = entry.getValue();
			
			transfer.amount = (int) flow.get(GRB.DoubleAttr.X);
			transfers.add(transfer);
		}
		
		return transfers;
		} catch (GRBException ex) {
			Logger.getLogger(ReactiveRebalancing.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
	}

	private void computeDistancesBetweenStations() {
		for(OnDemandVehicleStation stationFrom: 
				ProgressBar.wrap(onDemandvehicleStationStorage, "Computing distances between stations")){
//		for(OnDemandVehicleStation stationFrom: onDemandvehicleStationStorage){
			Map<OnDemandVehicleStation,Double> mapFromStation = new HashMap<>();
			distancesBetweenStations.put(stationFrom, mapFromStation);
			for(OnDemandVehicleStation stationTo: onDemandvehicleStationStorage){
				if(stationFrom != stationTo){
					double distance = astarTravelTimeProvider.getExpectedTravelTime(
							stationFrom.getPosition(), stationTo.getPosition());
					mapFromStation.put(stationTo, distance);
				}
//				LOGGER.info("Computing distance from station {} to station {}", stationFrom, stationTo);
			}
		}
	}

	private void sendOrders(List<Transfer> transfers) {
		for(Transfer transfer: transfers){
			for(int i = 0; i < transfer.amount; i++){
				stationsDispatcher.rebalance(transfer.from, transfer.to);
			}
		}
	}
	
}
