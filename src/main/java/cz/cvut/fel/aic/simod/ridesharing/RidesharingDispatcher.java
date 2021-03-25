/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.Routine;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingDispatcher extends StationsDispatcher implements Routine, EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingDispatcher.class);
	
	
	private final TimeProvider timeProvider;
	
	protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;
	
	private final DARPSolver solver;
	
	private final List darpSolverComputationalTimes;
	
	private final LinkedHashSet<PlanComputationRequest> waitingRequests;
	
	private final Map<Integer,PlanComputationRequest> requestsMapByDemandAgents;
	
	private final PositionUtil positionUtil;
	
	
	private List<PlanComputationRequest> newRequests;
	
	private int requestCounter;
	
	private IdGenerator tripIdGenerator;
	
	

	
	
	
	public List getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			TypedSimulation eventProcessor, SimodConfig config, DARPSolver solver, PeriodicTicker ticker,
			DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory, 
			TimeProvider timeProvider, PositionUtil positionUtil,IdGenerator tripIdGenerator) {
		super(onDemandvehicleStationStorage, eventProcessor, config, tripIdGenerator);
		this.timeProvider = timeProvider;
		this.solver = solver;
		this.requestFactory = requestFactory;
		this.positionUtil = positionUtil;
		this.tripIdGenerator = tripIdGenerator;
		newRequests = new ArrayList<>();
		waitingRequests = new LinkedHashSet<>();
		darpSolverComputationalTimes = new ArrayList();
		requestsMapByDemandAgents = new HashMap<>();
		requestCounter = 0;
		if(config.ridesharing.batchPeriod != 0){
			ticker.registerRoutine(this, config.ridesharing.batchPeriod * 1000);
		}
		setEventHandeling();
		
		solver.setDispatcher(this);
	}

	
	
	
	@Override
	protected void serveDemand(SimulationNode startNode, DemandData demandData) {
		SimulationNode requestStartPosition = demandData.locations[0];
		DefaultPlanComputationRequest newRequest = requestFactory.create(requestCounter++, requestStartPosition, 
				demandData.locations[1], demandData.demandAgent);
		waitingRequests.add(newRequest);
		newRequests.add(newRequest);
		requestsMapByDemandAgents.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
		
		if(config.ridesharing.batchPeriod == 0){
			replan();
		}
	}
	
	protected void replan(){
		int droppedDemandsThisBatch = 0;
		
		// logger info
		int currentTimeSec = (int) Math.round(timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("Current sim time is: {} seconds", currentTimeSec);
		LOGGER.info("No. of new requests: {}", newRequests.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequests.size());
		
		// dropping demands that waits too long
		Iterator<PlanComputationRequest> waitingRequestIterator = waitingRequests.iterator();
		while(waitingRequestIterator.hasNext()){
			PlanComputationRequest request = waitingRequestIterator.next();
			if(request.getMaxPickupTime() + 5  < currentTimeSec){
				request.getDemandAgent().setDropped(true);
				numberOfDemandsDropped++;
				droppedDemandsThisBatch++;
				waitingRequestIterator.remove();
				eventProcessor.addEvent(DemandEvent.LEFT, null, null, request);
				LOGGER.info("Demand {} dropped", request.getId());
			}
		}		
		LOGGER.info("Demands dropped in this batch: {}", droppedDemandsThisBatch);
		LOGGER.info("Total dropped demands count: {}", numberOfDemandsDropped);
		
		// DARP solving
		long startTime = System.nanoTime();
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans 
				= solver.solve(newRequests, new ArrayList<>(waitingRequests));
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);

		// executing new plans
		for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
			RideSharingOnDemandVehicle vehicle = entry.getKey();
    			DriverPlan plan = entry.getValue();
			vehicle.replan(plan);
		}
		
		// printing nice plans
		if(false){
			for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
				RideSharingOnDemandVehicle vehicle = entry.getKey();
				DriverPlan plan = entry.getValue();
				
				int pickupCount = 0;
				int dropoffCount = 0;
				Set<SimulationNode> positions = new HashSet<>();
				boolean positionOverlaps = false;
				
				for(PlanAction planAction: plan){
					if(positions.contains(planAction.getPosition())){
						positionOverlaps = true;
						break;
					}
					positions.add(planAction.getPosition());
					if(planAction instanceof PlanActionPickup){
						pickupCount++;
					}
					else if(planAction instanceof  PlanActionDropoff){
						dropoffCount++;
					}
				}
				
				if(!positionOverlaps && pickupCount > 1 && dropoffCount > 1){
					boolean nearby = false;
				
					for(SimulationNode node: positions){
						for(SimulationNode node2: positions){
							if(node != node2){
								int distance = (int) Math.round(positionUtil.getPosition(node).distance(
										positionUtil.getPosition(node2)));
								if(distance <  500){
									nearby = true;
									break;
								}
							}
							if(nearby){
								break;
							}
						}
					}
					if(!nearby){
						LOGGER.info(vehicle.getId() + ": " + plan.toString());
					}
				}
			}
		}

		// reseting new request for next iteration
		newRequests = new LinkedList<>();
	}

	@Override
	public void doRoutine() {
		replan();
	}
	
	@Override
	public void handleEvent(Event event) {
		// dispatcher common events
		if(event.getType() instanceof OnDemandVehicleStationsCentralEvent){
			super.handleEvent(event);
		}
		// pickup event
		else{
			OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
			if(eventType == OnDemandVehicleEvent.PICKUP){
				OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
				PlanComputationRequest request = requestsMapByDemandAgents.get(eventContent.getDemandId());
				if(!waitingRequests.remove(request)){
					try {
						throw new Exception("Request picked up but it is not present in the waiting request queue!");
					} catch (Exception ex) {
						Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
					}
				};
				request.setOnboard(true);
			}
		}
	}
	
	public PlanComputationRequest getRequest(int demandId){
		return requestsMapByDemandAgents.get(demandId);
	}
	
	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		eventProcessor.addEventHandler(this, typesToHandle);
	}
	
}
