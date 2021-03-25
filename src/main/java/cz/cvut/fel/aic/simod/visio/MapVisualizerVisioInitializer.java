/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.BikewayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.MetrowayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.PedestrianNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.RailwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TramwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.DefaultVisioInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.GridLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.HighwayLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.LayerManagementLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.MapTilesLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.cvut.fel.aic.alite.simulation.Simulation;
import cz.cvut.fel.aic.alite.vis.VisManager;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class MapVisualizerVisioInitializer  extends DefaultVisioInitializer{
	
	private final HighwayLayer highwayLayer;
	
	private final NodeIdLayer nodeIdLayer;
	
	private final LayerManagementLayer layerManagementLayer;
	
	private final OnDemandVehicleStationsLayer onDemandVehicleStationsLayer;
	
	private final SimpleBackgroundLayer simpleBackgroundLayer;
	
	private final MapTilesLayer mapTilesLayer;
	
	@Inject
	public MapVisualizerVisioInitializer(Simulation simulation, PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
			HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork, 
			RailwayNetwork railwayNetwork, SimulationControlLayer simulationControlLayer, GridLayer gridLayer,
			HighwayLayer highwayLayer, NodeIdLayer nodeIdLayer, AgentpolisConfig config, 
			OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, LayerManagementLayer layerManagementLayer,
			SimpleBackgroundLayer simpleBackgroundLayer, MapTilesLayer mapTilesLayer) {
		super(simulation, highwayNetwork,simulationControlLayer, gridLayer);
		this.highwayLayer = highwayLayer;
		this.nodeIdLayer = nodeIdLayer;
		this.layerManagementLayer = layerManagementLayer;
		this.onDemandVehicleStationsLayer = onDemandVehicleStationsLayer;
		this.simpleBackgroundLayer = simpleBackgroundLayer;
		this.mapTilesLayer = mapTilesLayer;
	}
	
	@Override
	protected void initGraphLayers() {
		VisManager.registerLayer(simpleBackgroundLayer);
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Map Tiles", mapTilesLayer));
	}

	@Override
	protected void initEntityLayers(Simulation simulation) {
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Stations", onDemandVehicleStationsLayer));
	}
	
	@Override
	protected void initLayersAfterEntityLayers() {
		VisManager.registerLayer(layerManagementLayer);
		VisManager.registerLayer(nodeIdLayer);
	}
	
}
