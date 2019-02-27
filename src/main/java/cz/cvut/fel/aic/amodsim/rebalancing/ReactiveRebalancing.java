///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package cz.cvut.fel.aic.amodsim.rebalancing;
//
//import com.google.inject.Inject;
//import com.google.inject.Singleton;
//import com.google.ortools.linearsolver.MPConstraint;
//import com.google.ortools.linearsolver.MPObjective;
//import com.google.ortools.linearsolver.MPSolver;
//import com.google.ortools.linearsolver.MPVariable;
//import cz.cvut.fel.aic.agentpolis.CollectionUtil;
//import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
//import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
//import cz.cvut.fel.aic.amodsim.StationsDispatcher;
//import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
//import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
//import cz.cvut.fel.aic.amodsim.ridesharing.AstarTravelTimeProvider;
//import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
///**
// *
// * @author David Fiedler
// */
//@Singleton
//public class ReactiveRebalancing implements Routine{
//	
//	private final PeriodicTicker ticker;
//	
//	private final AmodsimConfig config;
//	
//	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
//	
//	private final Map<OnDemandVehicleStation, Map<OnDemandVehicleStation, Double>> distancesBetweenStations;
//	
//	private final AstarTravelTimeProvider astarTravelTimeProvider;
//	
//	private final StationsDispatcher stationsDispatcher;
//
//	@Inject
//	public ReactiveRebalancing(PeriodicTicker ticker, AmodsimConfig config, 
//			OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
//			AstarTravelTimeProvider astarTravelTimeProvider, StationsDispatcher stationsDispatcher) {
//		this.ticker = ticker;
//		this.config = config;
//		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
//		this.astarTravelTimeProvider = astarTravelTimeProvider;
//		this.stationsDispatcher = stationsDispatcher;
//		distancesBetweenStations = new HashMap<>();
//	}
//	
//	public void start(){
//		computeDistancesBetweenStations();
//		ticker.registerRoutine(this, config.amodsim.amodsimRebalancing.period * 1000);	
//	}
//	
//	
//
//	@Override
//	public void doRoutine() {
////		Map<OnDemandVehicleStation,Integer> compensations = computeCompensations();
////		List<Transfer> transfers = computeTransfers(compensations);
////		sendOrders(transfers);
//	}
//
//	private Map<OnDemandVehicleStation, Integer> computeCompensations() {
//		Map<OnDemandVehicleStation, Integer> compensations = new HashMap<>();
//		
//		for(OnDemandVehicleStation station: onDemandvehicleStationStorage){
//			RebalancingOnDemandVehicleStation rebalancingStation = (RebalancingOnDemandVehicleStation) station;
//			int carCount = station.getParkedVehiclesCount();
//			int optimalCarCount = rebalancingStation.getOptimalCarCount();
//			
//			int buffer = (int) (config.amodsim.amodsimRebalancing.buffer * optimalCarCount);
//			
//			int compensation = 0;
//			if(carCount > optimalCarCount){
//				int optimalCarCountWithBuffer = optimalCarCount + buffer;
//				if(carCount > optimalCarCountWithBuffer){
//					compensation = optimalCarCountWithBuffer- carCount;
//				}
//			}
//			else{
//				int optimalCarCountWithBuffer = optimalCarCount - buffer;
//				if(carCount < optimalCarCountWithBuffer){
//					compensation = optimalCarCountWithBuffer- carCount;
//				}
//			}
//			
//			if(compensation != 0){
//				compensations.put(station, compensation);
//			}
//		}
//		return compensations;
//	}
//
//	private List<Transfer> computeTransfers(Map<OnDemandVehicleStation, Integer> compensations) {
//		
//		//solver creation
//		MPSolver solver = new MPSolver("solver", MPSolver.OptimizationProblemType.GLOP_LINEAR_PROGRAMMING);
//		
//		Map<OnDemandVehicleStation,List<MPVariable>> toFlowVars = new HashMap<>();
//		Map<MPVariable,Double> varCosts = new HashMap<>();
//		Map<MPVariable,Transfer> variablesToTransfer = new HashMap<>();
//		for(Entry<OnDemandVehicleStation, Integer> compensationFrom: compensations.entrySet()){
//			List<MPVariable> fromFlowVars = new LinkedList<>();
//			
//			OnDemandVehicleStation stationFrom = compensationFrom.getKey();
//			int fromFlow = compensationFrom.getValue();
//			
//			if(fromFlow < 0){
//				int amountFrom = Math.abs(fromFlow);
//				for(Entry<OnDemandVehicleStation, Integer> compensationTo: compensations.entrySet()){
//					int amountTo = compensationTo.getValue();
//					if(amountTo > 0){
//						
//						// variables
//						int maxAmount = Math.min(amountFrom, amountTo);
//						OnDemandVehicleStation stationTo = compensationTo.getKey();
//						String flowName = String.format("flow from %s to %s", stationFrom.getId(), stationTo.getId());
//						MPVariable flow = solver.makeNumVar(0, maxAmount, flowName);
//						
//						fromFlowVars.add(flow);
//						CollectionUtil.addToListInMap(toFlowVars, stationTo, flow);
//						
//						// variable costs
//						double cost = distancesBetweenStations.get(stationFrom).get(stationTo);
//						varCosts.put(flow, cost);
//						
//						// solution mappping
//						variablesToTransfer.put(flow, new Transfer(stationFrom, stationTo));
//					}
//				}
//				
//				// from flow sum constraint
//				String name = String.format("Constarint - sum of flows from station %s", stationFrom.getId());
//				MPConstraint fromFlowSumConstraint = solver.makeConstraint(amountFrom, amountFrom, name);
//				for(MPVariable variable: fromFlowVars){
//					fromFlowSumConstraint.setCoefficient(variable, 1);
//				}
//			}
//		}
//		
//		// to flow sum constraints
//		for(Entry<OnDemandVehicleStation, Integer> compensationTo: compensations.entrySet()){
//			int amountTo = compensationTo.getValue();
//			if(amountTo > 0){
//				OnDemandVehicleStation stationTo = compensationTo.getKey();
//
//				String name = String.format("Constarint - sum of flows to station %s", stationTo.getId());
//				MPConstraint fromFlowSumConstraint = solver.makeConstraint(amountTo, amountTo, name);
//				for(MPVariable variable: toFlowVars.get(stationTo)){
//					fromFlowSumConstraint.setCoefficient(variable, 1);
//				}
//			}
//		}
//			
//		
//		// objective function
//		MPObjective objective = solver.objective();
//		for(Entry<MPVariable,Double> varCost: varCosts.entrySet()){
//			objective.setCoefficient(varCost.getKey(), varCost.getValue());
//		}
//		
//		// solving
//		objective.setMinimization();
//        solver.setTimeLimit(10000);
//        MPSolver.ResultStatus status = solver.solve();
//        if (null != status) switch (status) {
//			case OPTIMAL:
//				System.out.println("Google optimization tools found an optimal solution.");
//				break;
//			case FEASIBLE:
//				System.out.println("Google optimization tools found a solution, but it was not able to prove in the given time limit, that it is optimal.");
//				break;
//			case INFEASIBLE:
//				System.out.println("Oops, the model is infeasible, it was probably created in a wrong way.");
//				break;
//		}
//
//		List<Transfer> transfers = new LinkedList<>();
//		for (Entry<MPVariable, Transfer> entry : variablesToTransfer.entrySet()) {
//			MPVariable flow = entry.getKey();
//			Transfer transfer = entry.getValue();
//			
//			transfer.amount = (int) Math.round(flow.solutionValue());
//			transfers.add(transfer);
//		}
//		
//		return transfers;
//	}
//
//	private void computeDistancesBetweenStations() {
//		for(OnDemandVehicleStation stationFrom: onDemandvehicleStationStorage){
//			Map<OnDemandVehicleStation,Double> mapFromStation = new HashMap<>();
//			distancesBetweenStations.put(stationFrom, mapFromStation);
//			for(OnDemandVehicleStation stationTo: onDemandvehicleStationStorage){
//				if(stationFrom != stationTo){
//					double distance = astarTravelTimeProvider.getExpectedTravelTime(
//							stationFrom.getPosition(), stationTo.getPosition());
//					mapFromStation.put(stationTo, distance);
//				}
//			}
//		}
//	}
//
//	private void sendOrders(List<Transfer> transfers) {
//		for(Transfer transfer: transfers){
//			for(int i = 0; i < transfer.amount; i++){
//				stationsDispatcher.rebalance(transfer.from, transfer.to);
//			}
//		}
//	}
//	
//}
