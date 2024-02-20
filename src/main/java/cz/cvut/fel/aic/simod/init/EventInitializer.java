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
package cz.cvut.fel.aic.simod.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationUtils;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
@Singleton
public class EventInitializer {
//	private static final double TRIP_MULTIPLICATION_FACTOR = 2.573;
//	private static final double TRIP_MULTIPLICATION_FACTOR = 13.63;
//	private static final double TRIP_MULTIPLICATION_FACTOR = 1.615;
//	private static final double TRIP_MULTIPLICATION_FACTOR = 3.433;
    
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EventInitializer.class);
	
	private static final long TRIP_MULTIPLICATION_TIME_SHIFT = 240_000;
	
	private static final long MAX_EVENTS = 0;
	
	private static final int RANDOM_SEED = 1;
	
	
	
	
	private final EventProcessor eventProcessor;

	private final DemandEventHandler demandEventHandler;
	
	private final StationsDispatcher onDemandVehicleStationsCentral;
	
	private final SimodConfig SimodConfig;
	
	private final AgentpolisConfig agentpolisConfig;
	
	private final SimulationUtils simulationUtils;

	private final TimeProvider timeProvider;
	
	private long eventCount;
        
	private long impossibleTripsCount;


	
	
	@Inject
	public EventInitializer(
		EventProcessor eventProcessor,
		StationsDispatcher onDemandVehicleStationsCentral,
		SimodConfig config,
		DemandEventHandler demandEventHandler,
		AgentpolisConfig agentpolisConfig,
		SimulationUtils simulationUtils,
		TimeProvider timeProvider
	) {
		this.eventProcessor = eventProcessor;
		this.demandEventHandler = demandEventHandler;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.SimodConfig = config;
		this.agentpolisConfig = agentpolisConfig;
		this.simulationUtils = simulationUtils;
		this.timeProvider = timeProvider;
		eventCount = 0;
		impossibleTripsCount = 0;
	}
	
	
	public void initialize(List<TimeTrip<SimulationNode>> trips, List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips){
		Random random = new Random(RANDOM_SEED);
		
		for (TimeTrip<SimulationNode> trip : trips) {
			long startTime = Duration.between(timeProvider.getInitDateTime(), trip.getStartTime()).toMillis();
			// trip have to start at least 1ms after start of the simulation and no later then last
			if(startTime < 1 || startTime > simulationUtils.getSimulationDuration()){
				impossibleTripsCount++;
//				LOGGER.info("Trip out of simulation time. Total: {}", impossibleTripsCount);
				continue;
			}
			
			for(int i = 0; i < SimodConfig.tripsMultiplier; i++){
				if(i + 1 >= SimodConfig.tripsMultiplier){
					double randomNum = random.nextDouble();
					if(randomNum > SimodConfig.tripsMultiplier - i){
						break;
					}
				}
				
				startTime = startTime + i * TRIP_MULTIPLICATION_TIME_SHIFT;
				eventProcessor.addEvent(null, demandEventHandler, null, trip, startTime);
				eventCount++;
				if(MAX_EVENTS != 0 && eventCount >= MAX_EVENTS){
					return;
				}
			}
		}
		if(rebalancingTrips != null){
			for (TimeTrip<OnDemandVehicleStation> rebalancingTrip : rebalancingTrips) {
				long startTime = Duration.between(timeProvider.getInitDateTime(), rebalancingTrip.getStartTime()).toMillis();
			if(startTime < 1 || startTime > simulationUtils.getSimulationDuration()){
				impossibleTripsCount++;
				continue;
			}
				eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.REBALANCING, onDemandVehicleStationsCentral, 
						null, rebalancingTrip, startTime);
			}
		}
		
		LOGGER.info("{} trips discarded because they are not within simulation time bounds", impossibleTripsCount);
	}
	
	
	
	
	public static class DemandEventHandler extends EventHandlerAdapter{
		
		private final IdGenerator demandIdGenerator;
 
		private final DemandAgentFactory demandAgentFactory;
		
		
		
		
		@Inject
		public DemandEventHandler(IdGenerator demandIdGenerator, DemandAgentFactory demandAgentFactory,
				SimulationCreator simulationCreator) {
			this.demandIdGenerator = demandIdGenerator;
			this.demandAgentFactory = demandAgentFactory;
		}

		
		
		

		@Override
		public void handleEvent(Event event) {
			TimeTrip<SimulationNode> trip = (TimeTrip<SimulationNode>) event.getContent();
			
			int id = demandIdGenerator.getId();
			
			DemandAgent demandAgent = demandAgentFactory.create("Demand " + Integer.toString(id), id, trip);
			
			demandAgent.born();
		}
	}	
}
