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
package cz.cvut.fel.aic.amodsim.rebalancing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.amodsim.entity.*;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.geographtools.util.Transformer;

/**
 *
 * @author fido
 */
public class RebalancingOnDemandVehicleStation extends OnDemandVehicleStation{
	
	private final int optimalCarCount;

	public int getOptimalCarCount() {
		return optimalCarCount;
	}
	
	
	
	@Inject
	public RebalancingOnDemandVehicleStation(AmodsimConfig config, EventProcessor eventProcessor, 
			OnDemandVehicleFactorySpec onDemandVehicleFactory, NearestElementUtils nearestElementUtils, 
			OnDemandvehicleStationStorage onDemandVehicleStationStorage, OnDemandVehicleStorage onDemandVehicleStorage, 
			@Assisted String id, @Assisted SimulationNode node, 
			@Assisted int initialVehicleCount, Transformer transformer, VisioPositionUtil positionUtil, 
			StationsDispatcher onDemandVehicleStationsCentral) {
		super(config, eventProcessor, onDemandVehicleFactory, nearestElementUtils, onDemandVehicleStationStorage, 
				onDemandVehicleStorage, id, node, initialVehicleCount, transformer, positionUtil, 
				onDemandVehicleStationsCentral);
		this.optimalCarCount = initialVehicleCount;
	}
	
	public interface RebalancingOnDemandVehicleStationFactory {
		public RebalancingOnDemandVehicleStation create(String id, SimulationNode node, int initialVehicleCount);
	}
}
