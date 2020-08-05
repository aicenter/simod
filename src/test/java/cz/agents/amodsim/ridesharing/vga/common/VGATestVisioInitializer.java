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
package cz.agents.amodsim.ridesharing.vga.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.mock.TestVisioInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.congestion.drive.support.CarLayer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.congestion.drive.support.TestVehicleLayer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.BikewayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.MetrowayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.PedestrianNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.RailwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TramwayNetwork;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.GridLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.HighwayLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.LayerManagementLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.cvut.fel.aic.alite.simulation.Simulation;
import cz.cvut.fel.aic.alite.vis.VisManager;
import cz.cvut.fel.aic.amodsim.visio.DemandLayer;
import cz.cvut.fel.aic.amodsim.visio.OnDemandVehicleLayer;
import cz.cvut.fel.aic.amodsim.visio.OnDemandVehiclePlanLayer;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class VGATestVisioInitializer extends TestVisioInitializer{
	
	private final OnDemandVehicleLayer onDemandVehicleLayer;
	
	private final DemandLayer demandLayer;
	
	private final OnDemandVehiclePlanLayer onDemandVehiclePlanLayer;

	
	
	
	@Inject
	public VGATestVisioInitializer(Simulation simulation, PedestrianNetwork pedestrianNetwork, 
			BikewayNetwork bikewayNetwork, HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, 
			MetrowayNetwork metrowayNetwork, RailwayNetwork railwayNetwork, LayerManagementLayer layerManagementLayer,
			SimulationControlLayer simulationControlLayer, HighwayLayer highwayLayer, 
			TestVehicleLayer testVehicleLayer, NodeIdLayer nodeIdLayer, GridLayer gridLayer, CarLayer carLayer, 
			AgentpolisConfig config, OnDemandVehicleLayer onDemandVehicleLayer, DemandLayer demandLayer, 
			OnDemandVehiclePlanLayer onDemandVehiclePlanLayer) {
		super(simulation, highwayNetwork, layerManagementLayer, simulationControlLayer, highwayLayer, nodeIdLayer, gridLayer, carLayer);
		this.onDemandVehicleLayer = onDemandVehicleLayer;
		this.demandLayer = demandLayer;
		this.onDemandVehiclePlanLayer = onDemandVehiclePlanLayer;
	}
	


	@Override
	protected void initEntityLayers(Simulation simulation) {
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Vehicles", onDemandVehicleLayer));
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Passangers", demandLayer));
		VisManager.registerLayer(onDemandVehiclePlanLayer);
	}
	
	
	
}
