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
package cz.cvut.fel.aic.simod.visual.ridesharing.vga.common;

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
import cz.cvut.fel.aic.simod.visio.DemandLayer;
import cz.cvut.fel.aic.simod.visio.OnDemandVehicleLayer;
import cz.cvut.fel.aic.simod.visio.OnDemandVehiclePlanLayer;

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
		super(simulation, highwayNetwork, layerManagementLayer, simulationControlLayer, highwayLayer, 
                        nodeIdLayer, gridLayer, carLayer);
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
