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
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.PlanActionCurrentPosition;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
public class RidesharingDispatcher extends StationsDispatcher implements Routine, EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingDispatcher.class);
	
	private final DARPSolver solver;
	
	private final List darpSolverComputationalTimes;
	
	private final LinkedHashSet<DefaultPlanComputationRequest> waitingRequests;
	
	private final Map<Integer,OnDemandRequest> requestsMapByDemandAgents;
	
	
	private List<OnDemandRequest> newRequests;
	
	

	
	
	
	public List getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			TypedSimulation eventProcessor, AmodsimConfig config, DARPSolver solver, PeriodicTicker ticker) {
		super(onDemandvehicleStationStorage, eventProcessor, config);
		this.solver = solver;
		newRequests = new ArrayList<>();
		waitingRequests = new LinkedHashSet<>();
		darpSolverComputationalTimes = new ArrayList();
		requestsMapByDemandAgents = new HashMap<>();
		if(config.ridesharing.batchPeriod != 0){
			ticker.registerRoutine(this, config.ridesharing.batchPeriod * 1000);
		}
		setEventHandeling();
	}

	
	
	
	@Override
	protected void serveDemand(Node startNode, DemandData demandData) {
		OnDemandRequest newRequest = new OnDemandRequest(demandData.demandAgent, demandData.locations.get(1));
		newRequests.add(newRequest);
		waitingRequests.add(newRequest);
		requestsMapByDemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
		if(config.ridesharing.batchPeriod == 0){
			replan();
		}
	}
	
	protected void replan(){
		int droppedDemandsThisBatch = 0;
		long startTime = System.nanoTime();
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans = solver.solve(newRequests);
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);
		

		// dropped demand check	
		int currentTimeSec 
				= (int) Math.round(VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0);
		for(OnDemandRequest request: waitingRequests){
			
		}

		for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
			RideSharingOnDemandVehicle vehicle = entry.getKey();
			DriverPlan plan = entry.getValue();

			// dropped demand check
			for(PlanAction task: plan){
				if(!(task instanceof PlanActionCurrentPosition)){
					requestsToDrop.remove(requestsMapByDemandAgents.get(
							((PlanRequestAction) task).getRequest().getDemandAgent().getSimpleId()));
				}	
			}

			vehicle.replan(plan);
		}

		for(OnDemandRequest request: requestsToDrop){
			request.getDemandAgent().setDropped(true);
			numberOfDemandsDropped++;
			droppedDemandsThisBatch++;
			waitingRequests.remove(request);
		}
		
		newRequests = new LinkedList<>();
		
		LOGGER.info("Demands dropped in this batch: {}", droppedDemandsThisBatch);
		LOGGER.info("Total dropped demands count: {}", numberOfDemandsDropped);
	}

	@Override
	public void doRoutine() {
		replan();
	}
	
	@Override
	public void handleEvent(Event event) {
		if(event.getType() instanceof OnDemandVehicleStationsCentralEvent){
			super.handleEvent(event);
		}
		else{
			OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
			OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
			OnDemandRequest request = requestsMapByDemandAgents.get(eventContent.getDemandId());
			if(eventType == OnDemandVehicleEvent.PICKUP){
				waitingRequests.remove(request);
			}
		}
	}
	
	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		eventProcessor.addEventHandler(this, typesToHandle);
	}
	
}
