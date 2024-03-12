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
package cz.cvut.fel.aic.simod.ridesharing;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.SimodException;
import cz.cvut.fel.aic.simod.action.*;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.darp.*;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.jackson.MyModule;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cz.cvut.fel.aic.simod.tripUtil.TripsUtilCached;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingDispatcher extends StationsDispatcher implements Routine, EventHandler{

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingDispatcher.class);

	protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;

	private final DARPSolver solver;

	private final List darpSolverComputationalTimes;

	private final LinkedHashSet<PlanComputationRequest> waitingRequests;

	/**
	 * Requests mapped by the index in the request input file. Map is used to not leave space for skipped or
	 * invalid rows in the input file.
	 */
	private final Map<Integer,PlanComputationRequest> requests;
	
	private final PositionUtil positionUtil;

	private final TravelTimeProvider travelTimeProvider;

	private List<PlanComputationRequest> newRequests;

	private int requestCounter;

	private IdGenerator tripIdGenerator;

	private int demandsCount;

	private boolean plansExported = false;
	
	






	public List getDarpSolverComputationalTimes() {
		return darpSolverComputationalTimes;
	}


	@Override
	public int getDemandsCount() {
		return demandsCount;
	}
	
	
	
	
	@Inject
	public RidesharingDispatcher(
		OnDemandvehicleStationStorage onDemandvehicleStationStorage,
		TypedSimulation eventProcessor,
		SimodConfig config,
		DARPSolver solver,
		PeriodicTicker ticker,
		DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory,
		TimeProvider timeProvider,
		PositionUtil positionUtil,
		IdGenerator tripIdGenerator,
		TravelTimeProvider travelTimeProvider
	) {
		super(onDemandvehicleStationStorage, eventProcessor, config, tripIdGenerator, timeProvider);
		this.solver = solver;
		this.requestFactory = requestFactory;
		this.positionUtil = positionUtil;
		this.tripIdGenerator = tripIdGenerator;
		this.travelTimeProvider = travelTimeProvider;
		newRequests = new ArrayList<>();
		waitingRequests = new LinkedHashSet<>();
		darpSolverComputationalTimes = new ArrayList();
		requests = new HashMap<>();
		requestCounter = 0;
		if(config.ridesharing.batchPeriod != 0){
			ticker.registerRoutine(this, config.ridesharing.batchPeriod * 1000);
		}
		setEventHandeling();

		solver.setDispatcher(this);
	}




	protected void processRequest(DefaultPlanComputationRequest request) {
		demandsCount++;

		waitingRequests.add(request);
		newRequests.add(request);
		requests.put(request.getId(), request);
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
		savePlans(newPlans, config );

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
	public boolean tickAtStart() {
		return true;
	}

	@Override
	public void handleEvent(Event event) {
		// dispatcher common events
		if(event.getType() instanceof OnDemandVehicleStationsCentralEvent){
			super.handleEvent(event);
		}
		else if(event.getType() instanceof DemandEvent && event.getType() == DemandEvent.ANNOUNCEMENT) {
			processRequest((DefaultPlanComputationRequest) event.getContent());
		}
		// pickup event
		else{
			OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
			if(eventType == OnDemandVehicleEvent.PICKUP){
				OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
				PlanComputationRequest request = requests.get(eventContent.getRequestIndex());
				if(!waitingRequests.remove(request)){
					try {
						throw new SimodException("Request picked up but it is not present in the waiting request queue!");
					} catch (Exception ex) {
						Logger.getLogger(RidesharingDispatcher.class.getName()).log(Level.SEVERE, null, ex);
					}
				};
				request.setOnboard(true);
			}
		}
	}

	public PlanComputationRequest getRequest(int demandId){
		return requests.get(demandId);
	}

	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		eventProcessor.addEventHandler(this, typesToHandle);
	}

	public void savePlans(Map<RideSharingOnDemandVehicle, DriverPlan> plans, SimodConfig config) {
		if(plansExported) return;
		try {
			DarpSolutionPlan[] darpPlans = new DarpSolutionPlan[plans.size()];
			int totalCost = 0;
			for (int i = 0; i < plans.size(); i++) {
				Entry<RideSharingOnDemandVehicle, DriverPlan> entry = (Entry<RideSharingOnDemandVehicle, DriverPlan>) plans.entrySet().toArray()[i];
				RideSharingOnDemandVehicle vehicle = entry.getKey();
				DriverPlan plan = entry.getValue();
   				int cost = (int) (plan.cost);
				totalCost += cost;
				DarpSolutionPlanVehicle darpVehicle = new DarpSolutionPlanVehicle(vehicle.getIndex(), new DarpSolutionPosition(vehicle.getPosition().getIndex()));
				DarpSolutionStopAction[] planActions = new DarpSolutionStopAction[plan.plan.size()-1];

				SimulationNode lastPosition = plan.plan.get(0).getPosition();
				long t = 0;
				long globalDepartureTime = 0;
				long globalArrivalTime = 0;
				for (int j = 0; j < plan.plan.size(); j++) {
					PlanAction action = plan.plan.get(j);
					boolean isPickup = action instanceof PlanActionPickup;
					boolean isDropOff = action instanceof PlanActionDropoff;
					String type = isPickup ? "pickup": isDropOff? "dopoff": "current";
					if (action instanceof PlanActionCurrentPosition) continue;
					PlanRequestAction planRequestAction = (PlanRequestAction) action;

					DarpSolutionStopActionDetails details = new DarpSolutionStopActionDetails(0, 0, type, new DarpSolutionPosition(action.getPosition().getIndex()),planRequestAction.getMinTime(),planRequestAction.getMaxTime(),0);
					long travelTime = travelTimeProvider.getTravelTime(vehicle, lastPosition, action.getPosition()) / 1000;
					if(t==0) {
						t = planRequestAction.getMinTime()-travelTime;
						globalDepartureTime = t;
					}

					t += travelTime;
					long arrivalTime = t;
					globalArrivalTime = arrivalTime;
					long waitForMinTime = planRequestAction.getMinTime() - t;
					long departureTime = arrivalTime + config.serviceTime;
					if (waitForMinTime>0) departureTime += waitForMinTime;
					t = departureTime;
					lastPosition = action.getPosition();
					planActions[j-1] = new DarpSolutionStopAction((int) arrivalTime,(int) departureTime, details);
				}
				darpPlans[i] = new DarpSolutionPlan(cost,(int) globalDepartureTime,(int) globalArrivalTime, darpVehicle, planActions);
			}

			DarpSolution solution =new DarpSolution(true, totalCost, totalCost/60, darpPlans, new DarpSolutionDroppedRequest[]{});

			File outputFile = new File(config.bezbaOutputFilePath);
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new MyModule());
			mapper.writeValue(outputFile, solution);
			plansExported = true;

		} catch (IOException e) {
			Logger.getLogger(TripsUtilCached.class.getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		}

	}
	
}
