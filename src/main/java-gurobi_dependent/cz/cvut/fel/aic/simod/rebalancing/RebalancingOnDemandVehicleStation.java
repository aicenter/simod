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
package rebalancing;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;

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
	public RebalancingOnDemandVehicleStation(SimodConfig config, EventProcessor eventProcessor, 
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
