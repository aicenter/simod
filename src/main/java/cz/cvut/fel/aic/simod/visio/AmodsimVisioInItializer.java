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
import cz.cvut.fel.aic.agentpolis.simmodel.environment.AgentStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.VehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.BikewayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.MetrowayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.PedestrianNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.RailwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TramwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.*;
import cz.cvut.fel.aic.alite.simulation.Simulation;
import cz.cvut.fel.aic.alite.vis.VisManager;
import cz.cvut.fel.aic.alite.vis.layer.VisLayer;
import cz.cvut.fel.aic.alite.vis.layer.common.ColorLayer;
import java.awt.Color;

/**
 *
 * @author fido
 */
@Singleton
public class AmodsimVisioInItializer extends DefaultVisioInitializer{

//	 pridat vrstvu do dependency injection
	
	private final OnDemandVehicleLayer onDemandVehicleLayer;
	
	private final TrafficDensityLayer trafficDensityLayer;
	
	protected final NodeIdLayer nodeIdLayer;
	
	private final DemandLayer demandLayer;
	
	private final OnDemandVehicleStationsLayer onDemandVehicleStationsLayer;
	
	private final RidesharingOnDemandVehiclePlanLayer onDemandVehiclePlanLayer;
	
	protected final HighwayLayer highwayLayer;
	
	protected final BufferedHighwayLayer bufferedHighwayLayer;

	private final TrafficDensityByDirectionLayer trafficDensityByDirectionLayer;

	private final VisLayer backgroundLayer;
	
	private final MapTilesLayer mapTilesLayer;
	
	private final LayerManagementLayer layerManagementLayer;
			
	private final VehicleHighlightingLayer vehicleHighlightingLayer;

	private final ScreenRecordingLayer screenRecordingLayer;

	private final ScreenCaputreLayer screenCaputreLayer;

	private final SimpleBackgroundLayer simpleBackgroundLayer;

	private final ParcelLayer parcelLayer;

	
	@Inject
	public AmodsimVisioInItializer(Simulation simulation, PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
								   HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork,
								   RailwayNetwork railwayNetwork, AgentStorage agentStorage,
								   VehicleStorage vehicleStorage, AllNetworkNodes allNetworkNodes,
								   SimulationCreator simulationCreator, OnDemandVehicleLayer onDemandVehicleLayer,
								   TrafficDensityLayer trafficDensityLayer, NodeIdLayer nodeIdLayer,
								   OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, DemandLayer demandLayer,
								   RidesharingOnDemandVehiclePlanLayer onDemandVehiclePlanLayer, HighwayLayer highwayLayer,
								   BufferedHighwayLayer bufferedHighwayLayer, SimulationControlLayer simulationControlLayer,
								   TrafficDensityByDirectionLayer trafficDensityByDirectionLayer, GridLayer gridLayer,
								   MapTilesLayer mapTilesLayer, AgentpolisConfig config, LayerManagementLayer layerManagementLayer,
								   VehicleHighlightingLayer vehicleHighlightingLayer, ScreenRecordingLayer screenRecordingLayer,
								   ScreenCaputreLayer screenCaputreLayer, SimpleBackgroundLayer simpleBackgroundLayer,
								   ParcelLayer parcelLayer) {
		super(simulation, highwayNetwork, simulationControlLayer, gridLayer);
		this.onDemandVehicleLayer = onDemandVehicleLayer;
		this.trafficDensityLayer = trafficDensityLayer;
		this.nodeIdLayer = nodeIdLayer;
		this.onDemandVehicleStationsLayer = onDemandVehicleStationsLayer;
		this.demandLayer = demandLayer;
		this.onDemandVehiclePlanLayer = onDemandVehiclePlanLayer;
		this.highwayLayer = highwayLayer;
		this.bufferedHighwayLayer = bufferedHighwayLayer;
		this.trafficDensityByDirectionLayer = trafficDensityByDirectionLayer;
		this.mapTilesLayer = mapTilesLayer;
		this.backgroundLayer = ColorLayer.create(Color.white);
		this.layerManagementLayer = layerManagementLayer;
		this.vehicleHighlightingLayer = vehicleHighlightingLayer;
		this.screenRecordingLayer = screenRecordingLayer;
		this.screenCaputreLayer = screenCaputreLayer;
		this.simpleBackgroundLayer = simpleBackgroundLayer;
		this.parcelLayer = parcelLayer;
	}

	@Override
	protected void initEntityLayers(Simulation simulation) {
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Stations", onDemandVehicleStationsLayer));
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Vehicles", onDemandVehicleLayer));
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Parcels", parcelLayer));
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Passengers", demandLayer));
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Vehicle Plan", onDemandVehiclePlanLayer));
	}

	@Override
	protected void initLayersBeforeEntityLayers() {
//		VisManager.registerLayer(trafficDensityLayer);
//		VisManager.registerLayer(trafficDensityByDirectionLayer);
	}

	@Override
	protected void initLayersAfterEntityLayers() {
		VisManager.registerLayer(layerManagementLayer);
		VisManager.registerLayer(vehicleHighlightingLayer);
		VisManager.registerLayer(screenRecordingLayer);
		VisManager.registerLayer(screenCaputreLayer);
		vehicleHighlightingLayer.setVehicleLayer(onDemandVehicleLayer);
		VisManager.registerLayer(nodeIdLayer);
	}

	@Override
	protected void initGraphLayers() {
//		VisManager.registerLayer(backgroundLayer);
		VisManager.registerLayer(simpleBackgroundLayer);
		VisManager.registerLayer(layerManagementLayer.createManageableLayer("Map Tiles", mapTilesLayer));
//		VisManager.registerLayer(highwayLayer);
//		VisManager.registerLayer(bufferedHighwayLayer);
	}
	
	
}
