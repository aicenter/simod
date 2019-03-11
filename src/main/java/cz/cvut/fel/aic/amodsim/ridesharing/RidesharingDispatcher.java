/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.PlanActionCurrentPosition;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingDispatcher extends StationsDispatcher implements Routine{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingDispatcher.class);
	
	private final DARPSolver solver;
	
	
	private List<OnDemandRequest> requestQueue;
	
	
	private final List<Long> darpSolverComputationalTimes;

	
	
	
	public List<Long> getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			EventProcessor eventProcessor, AmodsimConfig config, DARPSolver solver, PeriodicTicker ticker) {
		super(onDemandvehicleStationStorage, eventProcessor, config);
		this.solver = solver;
		requestQueue = new LinkedList<>();
		darpSolverComputationalTimes = new LinkedList<>();
		if(config.ridesharing.batchPeriod != 0){
			ticker.registerRoutine(this, config.ridesharing.batchPeriod * 1000);
		}
	}

	
	
	
	@Override
	protected void serveDemand(Node startNode, DemandData demandData) {
		OnDemandRequest newRequest = new OnDemandRequest(demandData.demandAgent, demandData.locations.get(1));
		requestQueue.add(newRequest);
		if(config.ridesharing.batchPeriod == 0){
			replan();
		}
	}
	
	protected void replan(){
		int droppedDemandsThisBatch = 0;
		long startTime = System.nanoTime();
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans = solver.solve(requestQueue);
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);
		

		// dropped demand check
		Set<DemandAgent> demandsToDrop = new HashSet();			
		for(OnDemandRequest request: requestQueue){
			demandsToDrop.add(request.getDemandAgent());
		}

		for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
			RideSharingOnDemandVehicle vehicle = entry.getKey();
			DriverPlan plan = entry.getValue();

			// dropped demand check
			for(PlanAction task: plan){
				if(!(task instanceof PlanActionCurrentPosition)){
					demandsToDrop.remove(((PlanRequestAction) task).getRequest().getDemandAgent());
				}	
			}

			vehicle.replan(plan);
		}

		for(DemandAgent demandAgent: demandsToDrop){
			demandAgent.setDropped(true);
			numberOfDemandsDropped++;
			droppedDemandsThisBatch++;
		}
		
		requestQueue = new LinkedList<>();
		
		LOGGER.info("Demands dropped in this batch: {}", droppedDemandsThisBatch);
		LOGGER.info("Total dropped demands count: {}", numberOfDemandsDropped);
	}

	@Override
	public void doRoutine() {
		replan();
	}
	
}
