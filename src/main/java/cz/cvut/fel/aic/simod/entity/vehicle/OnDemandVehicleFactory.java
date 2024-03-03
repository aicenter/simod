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
package cz.cvut.fel.aic.simod.entity.vehicle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleFactory implements OnDemandVehicleFactorySpec{
	
	protected final TripsUtil tripsUtil;
	
	protected final StationsDispatcher onDemandVehicleStationsCentral;
	
	@Inject(optional = true)
	protected final PhysicalVehicleDriveFactory driveActivityFactory = null;
	
	protected final VisioPositionUtil positionUtil;
	
	protected final EventProcessor eventProcessor;
	
	protected final StandardTimeProvider timeProvider;
	
	protected final IdGenerator rebalancingIdGenerator;
	
	protected final PhysicalTransportVehicleStorage vehicleStorage;
	
	protected final SimodConfig config;
	
	protected final IdGenerator idGenerator;
	
	protected final AgentpolisConfig agentpolisConfig;

	
	
	
	@Inject
	public OnDemandVehicleFactory(
			PhysicalTransportVehicleStorage vehicleStorage, 
			TripsUtil tripsUtil, 
			StationsDispatcher onDemandVehicleStationsCentral, 
			VisioPositionUtil positionUtil, 
			EventProcessor eventProcessor,
			StandardTimeProvider timeProvider, 
			IdGenerator rebalancingIdGenerator, 
			SimodConfig config,
			IdGenerator idGenerator,
			AgentpolisConfig agentpolisConfig) {
		this.tripsUtil = tripsUtil;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.positionUtil = positionUtil;
		this.eventProcessor = eventProcessor;
		this.timeProvider = timeProvider;
		this.rebalancingIdGenerator = rebalancingIdGenerator;
		this.vehicleStorage = vehicleStorage;
		this.config = config;
		this.idGenerator = idGenerator;
		this.agentpolisConfig = agentpolisConfig;
	}
	
	
	
	@Override
	public OnDemandVehicle create(String vehicleId, SimulationNode startPosition,
		PhysicalTransportVehicle<? extends TransportableEntity> vehicle
	){
		return new OnDemandVehicle(
			vehicleStorage,
			tripsUtil,
			onDemandVehicleStationsCentral,
			driveActivityFactory,
			positionUtil,
			eventProcessor,
			timeProvider,
			rebalancingIdGenerator,
			config,
			idGenerator,
			agentpolisConfig,
			vehicleId,
			startPosition,
			vehicle
		);
	}
}
