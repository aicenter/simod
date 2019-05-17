/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import cz.cvut.fel.aic.geographtools.Node;
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
