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
package cz.cvut.fel.aic.amodsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import java.util.List;
import java.util.Random;

import cz.cvut.fel.aic.agentpolis.simulator.SimulationUtils;

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
	
	private static final long TRIP_MULTIPLICATION_TIME_SHIFT = 240_000;
	
	private static final long MAX_EVENTS = 0;
	
	private static final int RANDOM_SEED = 1;
	
	
	
	
	private final EventProcessor eventProcessor;

	private final DemandEventHandler demandEventHandler;
	
	private final StationsDispatcher onDemandVehicleStationsCentral;
	
	private final AmodsimConfig amodsimConfig;
	
	private final AgentpolisConfig agentpolisConfig;
	
	private final SimulationUtils simulationUtils;
	
	private long eventCount;
	
	
	@Inject
	public EventInitializer(EventProcessor eventProcessor, 
			StationsDispatcher onDemandVehicleStationsCentral, AmodsimConfig config, 
			DemandEventHandler demandEventHandler, AgentpolisConfig agentpolisConfig, SimulationUtils simulationUtils) {
		this.eventProcessor = eventProcessor;
		this.demandEventHandler = demandEventHandler;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.amodsimConfig = config;
		this.agentpolisConfig = agentpolisConfig;
		this.simulationUtils = simulationUtils;
		eventCount = 0;
	}
	
	
	public void initialize(List<TimeTrip<SimulationNode>> trips, List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips){
		Random random = new Random(RANDOM_SEED);
		
		for (TimeTrip<SimulationNode> trip : trips) {
			long startTime = trip.getStartTime() - amodsimConfig.startTime;
			// trip have to start at least 1ms after start of the simulation and no later then last
			if(startTime < 1 || startTime > simulationUtils.computeSimulationDuration()){

				continue;
			}
			
			for(int i = 0; i < amodsimConfig.tripsMultiplier; i++){
				if(i + 1 >= amodsimConfig.tripsMultiplier){
					double randomNum = random.nextDouble();
					if(randomNum > amodsimConfig.tripsMultiplier - i){
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
				long startTime = rebalancingTrip.getStartTime() - amodsimConfig.startTime;
			if(startTime < 1 || startTime > simulationUtils.computeSimulationDuration()){
					continue;
				}
				eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.REBALANCING, onDemandVehicleStationsCentral, 
						null, rebalancingTrip, startTime);
			}
		}
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
