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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.WaitActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.MoDVehicle;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicleFactory;
import cz.cvut.fel.aic.simod.storage.MoDVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

import java.time.ZonedDateTime;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class RidesharingOnDemandVehicleFactory extends OnDemandVehicleFactory{

	private final WaitActivityFactory waitActivityFactory;

	private final TravelTimeProvider travelTimeProvider;
	
	@Inject
	public RidesharingOnDemandVehicleFactory(
			MoDVehicleStorage vehicleStorage,
			TripsUtil tripsUtil, 
			StationsDispatcher onDemandVehicleStationsCentral,
			VisioPositionUtil positionUtil, 
			EventProcessor eventProcessor, 
			StandardTimeProvider timeProvider, 
			IdGenerator rebalancingIdGenerator, 
			SimodConfig config,
			IdGenerator idGenerator,
			AgentpolisConfig agentpolisConfig,
		WaitActivityFactory waitActivityFactory,
		TravelTimeProvider travelTimeProvider
	) {
		super(
				vehicleStorage, 
				tripsUtil, 
				onDemandVehicleStationsCentral, 
				positionUtil, 
				eventProcessor, 
				timeProvider, 
				rebalancingIdGenerator, 
				config, 
				idGenerator,
				agentpolisConfig);
		this.waitActivityFactory = waitActivityFactory;
		this.travelTimeProvider = travelTimeProvider;
	}

	@Override
	public OnDemandVehicle create(String vehicleId, SimulationNode startPosition,
		MoDVehicle vehicle, ZonedDateTime operationStart, ZonedDateTime operationEnd,
		OnDemandVehicleState onDemandVehicleState
	) {
		return new RideSharingOnDemandVehicle(
			vehicleStorage,
			tripsUtil,
			onDemandVehicleStationsCentral,
			driveActivityFactory,
			positionUtil,
			rebalancingIdGenerator,
			eventProcessor,
			timeProvider,
			rebalancingIdGenerator,
			config,
			idGenerator,
			agentpolisConfig,
			waitActivityFactory,
			travelTimeProvider,
			vehicleId,
			startPosition,
			vehicle,
			operationStart,
			operationEnd,
			onDemandVehicleState
		);
	}
	
	
	
}
