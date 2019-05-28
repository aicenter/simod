/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
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
import java.util.ArrayList;
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
	
	private final List darpSolverComputationalTimes;
	
	
	private List<OnDemandRequest> requestQueue;

	
	
	
	public List getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			EventProcessor eventProcessor, AmodsimConfig config, DARPSolver solver, PeriodicTicker ticker) {
		super(onDemandvehicleStationStorage, eventProcessor, config);
		this.solver = solver;
		requestQueue = new LinkedList<>();
		darpSolverComputationalTimes = new ArrayList();
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
