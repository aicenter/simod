/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingDispatcher extends StationsDispatcher implements Routine{
	
	private final DARPSolver solver;
	
	
	private List<OnDemandRequest> requestQueue;
	
	
	private final List<Long> darpSolverComputationalTimes;

	
	
	
	public List<Long> getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			EventProcessor eventProcessor, AmodsimConfig config, DARPSolver solver, PeriodicTicker ticker,
			@Named("mapSrid") int srid) {
		super(onDemandvehicleStationStorage, eventProcessor, config, srid);
		this.solver = solver;
		requestQueue = new LinkedList<>();
		darpSolverComputationalTimes = new LinkedList<>();
		if(config.amodsim.ridesharing.vga.batchPeriod != 0){
			ticker.registerRoutine(this, config.amodsim.ridesharing.vga.batchPeriod * 1000);
		}
	}

	
	
	
	@Override
	protected void serveDemand(Node startNode, DemandData demandData) {
		OnDemandRequest newRequest = new OnDemandRequest(demandData.demandAgent, demandData.locations.get(1));
		requestQueue.add(newRequest);
		if(config.amodsim.ridesharing.vga.batchPeriod == 0){
			replan();
		}
	}
	
	protected void replan(){
		long startTime = System.nanoTime();
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans = solver.solve(requestQueue);
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);
		

		// dropped demand check
		Set<DemandAgent> demands = new HashSet();			
		for(OnDemandRequest request: requestQueue){
			demands.add(request.getDemandAgent());
		}

		for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
			RideSharingOnDemandVehicle vehicle = entry.getKey();
			DriverPlan plan = entry.getValue();

			// dropped demand check
			for(DriverPlanTask task: plan){
				demands.remove(task.getDemandAgent());
			}

			vehicle.replan(plan);
		}

		for(DemandAgent demandAgent: demands){
			demandAgent.setDropped(true);
			numberOfDemandsDropped++;
		}
		
		requestQueue = new LinkedList<>();
	}

	@Override
	public void doRoutine() {
		replan();
	}
	
}
