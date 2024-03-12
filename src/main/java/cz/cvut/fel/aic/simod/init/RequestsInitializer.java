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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.DateTimeParser;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationUtils;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * @author fido
 */
@Singleton
public class RequestsInitializer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RequestsInitializer.class);

	private static final long TRIP_MULTIPLICATION_TIME_SHIFT = 240_000;

	private static final long MAX_EVENTS = 0;

	private static final int RANDOM_SEED = 1;


	private final EventProcessor eventProcessor;

	private final DemandEventHandler demandEventHandler;

	private final StationsDispatcher dispatcher;

	private final SimodConfig config;

	private final AgentpolisConfig agentpolisConfig;

	private final SimulationUtils simulationUtils;

	private final TimeProvider timeProvider;

	private final NearestElementUtils nearestElementUtils;

	protected final DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory;


	private long eventCount;

	private long impossibleTripsCount;


	@Inject
	public RequestsInitializer(
		EventProcessor eventProcessor,
		StationsDispatcher dispatcher,
		SimodConfig config,
		DemandEventHandler demandEventHandler,
		AgentpolisConfig agentpolisConfig,
		SimulationUtils simulationUtils,
		TimeProvider timeProvider,
		NearestElementUtils nearestElementUtils,
		DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory
	) {
		this.eventProcessor = eventProcessor;
		this.demandEventHandler = demandEventHandler;
		this.dispatcher = dispatcher;
		this.config = config;
		this.agentpolisConfig = agentpolisConfig;
		this.simulationUtils = simulationUtils;
		this.timeProvider = timeProvider;
		this.nearestElementUtils = nearestElementUtils;
		this.requestFactory = requestFactory;
		eventCount = 0;
		impossibleTripsCount = 0;
	}


	public void initialize() {
		Random random = new Random(RANDOM_SEED);
		int sameStartAndTargetInDataCount = 0; // trips with same start and target coordinates in the data
		int zeroLenghtTripsCount = 0; // trips with same start and target node in the road network
		int requestCounter = 0;

		CsvSchema headerSchema = CsvSchema.emptySchema().withHeader();
		CsvMapper mapper = new CsvMapper();
		try {
			LOGGER.info("Loading trips from: {}", config.tripsPath);
			MappingIterator<Map<String, String>> it = mapper
				.readerForMapOf(String.class)
				.with(headerSchema)
				.readValues(new File(config.tripsPath));

			Iterator<Map<String, String>> iter = ProgressBar.wrap(it, "Loading trips");
			while(iter.hasNext()) {
				Map<String, String> row = iter.next();
//				 = it.next();

				GPSLocation startLocation = new GPSLocation(
					Double.parseDouble(row.get("Latitude_From")),
					Double.parseDouble(row.get("Longitude_From")),
					0,
					0
				);
				GPSLocation targetLocation = new GPSLocation(
					Double.parseDouble(row.get("Latitude_To")),
					Double.parseDouble(row.get("Longitude_To")),
					0,
					0
				);

				if (startLocation.equals(targetLocation)) {
					sameStartAndTargetInDataCount++;
				} else {
					SimulationNode startNode = nearestElementUtils.getNearestElement(startLocation, EGraphType.HIGHWAY);
					SimulationNode targetNode
						= nearestElementUtils.getNearestElement(targetLocation, EGraphType.HIGHWAY);

					if (startNode == targetNode) {
						zeroLenghtTripsCount++;
					} else {
						// announcement time processing
						var announcementTime = DateTimeParser.parseDateTimeFromUnknownFormat(row.get(
							"Announcement_Time"));

						// min travel time processing
						var desiredPickupTime = DateTimeParser.parseDateTimeFromUnknownFormat(row.get("Pickup_Time"));
						long startTime = Duration.between(timeProvider.getInitDateTime(), desiredPickupTime).toMillis();
						if (startTime < 1 || startTime > simulationUtils.getSimulationDuration()) {
							impossibleTripsCount++;
							//				LOGGER.info("Trip out of simulation time. Total: {}", impossibleTripsCount);
							continue;
						}

						// slot type processing
						SlotType requiredSlotType;
						if (config.heterogeneousVehicles) {
							requiredSlotType = SlotType.valueOf(row.get("Slot_Type"));
						} else {
							requiredSlotType = SlotType.STANDARD_SEAT;
						}

						// Required vehicle processing
						int requiredVehicleId = -1;
						if(row.containsKey("required_vehicle_id")){
							String requiredVehicleIdStr = row.get("required_vehicle_id");
							requiredVehicleId = Integer.parseInt(requiredVehicleIdStr);
						}

						for (int i = 0; i < config.tripsMultiplier; i++) {
							if (i + 1 >= config.tripsMultiplier) {
								double randomNum = random.nextDouble();
								if (randomNum > config.tripsMultiplier - i) {
									break;
								}
							}
							startTime = startTime + i * TRIP_MULTIPLICATION_TIME_SHIFT;

							DefaultPlanComputationRequest newRequest = requestFactory.create(
								requestCounter,
								startNode,
								targetNode,
								announcementTime,
								(int) Math.round(startTime / 1000.0),
								requiredSlotType,
								null,
								requiredVehicleId
							);

							// event for dispatcher
							long annoucementTimeMillis
								= Duration.between(timeProvider.getInitDateTime(), announcementTime).toMillis();
							if(annoucementTimeMillis < 0){
								throw new RuntimeException("Announcement time is before simulation start time");
							}
							else if(annoucementTimeMillis == 0){
								eventProcessor.addEvent(
									DemandEvent.ANNOUNCEMENT,
									dispatcher,
									null,
									newRequest
								);
							}
							else {
								eventProcessor.addEvent(
									DemandEvent.ANNOUNCEMENT,
									dispatcher,
									null,
									newRequest,
									annoucementTimeMillis
								);
							}

							// event for demand event handler
							eventProcessor.addEvent(null, demandEventHandler, null, newRequest, newRequest.getMinTime());

							eventCount++;
							if (MAX_EVENTS != 0 && eventCount >= MAX_EVENTS) {
								return;
							}
						}
					}
				}
				requestCounter++;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

//		for (TimeTrip<SimulationNode> trip : trips) {
//			long startTime = Duration.between(timeProvider.getInitDateTime(), trip.getStartTime()).toMillis();
//			// trip have to start at least 1ms after start of the simulation and no later then last
//			if(startTime < 1 || startTime > simulationUtils.getSimulationDuration()){
//				impossibleTripsCount++;
////				LOGGER.info("Trip out of simulation time. Total: {}", impossibleTripsCount);
//				continue;
//			}
//
//			for(int i = 0; i < config.tripsMultiplier; i++){
//				if(i + 1 >= config.tripsMultiplier){
//					double randomNum = random.nextDouble();
//					if(randomNum > config.tripsMultiplier - i){
//						break;
//					}
//				}
//
//				startTime = startTime + i * TRIP_MULTIPLICATION_TIME_SHIFT;
//				eventProcessor.addEvent(null, demandEventHandler, null, trip, startTime);
//				eventCount++;
//				if(MAX_EVENTS != 0 && eventCount >= MAX_EVENTS){
//					return;
//				}
//			}
//		}
		LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
		LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);
		LOGGER.info("{} trips discarded because they are not within simulation time bounds", impossibleTripsCount);
	}


	public static class DemandEventHandler extends EventHandlerAdapter {

		private final IdGenerator demandIdGenerator;

		private final DemandAgentFactory demandAgentFactory;


		@Inject
		public DemandEventHandler(
			IdGenerator demandIdGenerator, DemandAgentFactory demandAgentFactory,
			SimulationCreator simulationCreator
		) {
			this.demandIdGenerator = demandIdGenerator;
			this.demandAgentFactory = demandAgentFactory;
		}


		@Override
		public void handleEvent(Event event) {
			DefaultPlanComputationRequest request = (DefaultPlanComputationRequest) event.getContent();

			int id = demandIdGenerator.getId();

			DemandAgent demandAgent = demandAgentFactory.create("Demand " + Integer.toString(id), id, request);

			demandAgent.born();
		}
	}
}
