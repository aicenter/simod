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
package cz.cvut.fel.aic.amodsim.entity.vehicle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;

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
	
	protected final AmodsimConfig config;

	
	
	
	@Inject
	public OnDemandVehicleFactory(PhysicalTransportVehicleStorage vehicleStorage, 
			TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
			VisioPositionUtil positionUtil, EventProcessor eventProcessor,
			StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, AmodsimConfig config) {
		this.tripsUtil = tripsUtil;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.positionUtil = positionUtil;
		this.eventProcessor = eventProcessor;
		this.timeProvider = timeProvider;
		this.rebalancingIdGenerator = rebalancingIdGenerator;
		this.vehicleStorage = vehicleStorage;
		this.config = config;
	}
	
	
	
	@Override
	public OnDemandVehicle create(String vehicleId, SimulationNode startPosition){
		return new OnDemandVehicle(vehicleStorage, tripsUtil, 
				onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, 
				rebalancingIdGenerator, config, vehicleId, startPosition);
	}
}
