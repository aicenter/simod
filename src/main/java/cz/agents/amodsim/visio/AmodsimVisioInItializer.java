/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.visio;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.HighwayLayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.AgentStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.*;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.DefaultVisioInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.Projection;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.VisManager;
import cz.agents.alite.vis.layer.VisLayer;
import cz.agents.alite.vis.layer.common.ColorLayer;
import java.awt.Color;

/**
 *
 * @author fido
 */
@Singleton
public class AmodsimVisioInItializer extends DefaultVisioInitializer{
    
    private final OnDemandVehicleLayer onDemandVehicleLayer;
    
    private final TrafficDensityLayer trafficDensityLayer;
    
    protected final NodeIdLayer nodeIdLayer;
    
    private final DemandLayer demandLayer;
    
    private final OnDemandVehicleStationsLayer onDemandVehicleStationsLayer;
	
	private final OnDemandVehiclePlanLayer onDemandVehiclePlanLayer;
    
    protected final HighwayLayer highwayLayer;
    
    protected final BufferedHighwayLayer bufferedHighwayLayer;

    private final TrafficDensityByDirectionLayer trafficDensityByDirectionLayer;

    private final VisLayer backgroundLayer;
    
    
    
    @Inject
    public AmodsimVisioInItializer(PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
                                   HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork,
                                   RailwayNetwork railwayNetwork, AgentStorage agentStorage,
                                   VehicleStorage vehicleStorage, AllNetworkNodes allNetworkNodes,
                                   SimulationCreator simulationCreator, OnDemandVehicleLayer onDemandVehicleLayer,
                                   TrafficDensityLayer trafficDensityLayer, NodeIdLayer nodeIdLayer,
                                   OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, DemandLayer demandLayer,
                                   OnDemandVehiclePlanLayer onDemandVehiclePlanLayer, HighwayLayer highwayLayer,
                                   BufferedHighwayLayer bufferedHighwayLayer, SimulationControlLayer simulationControlLayer,
                                   TrafficDensityByDirectionLayer trafficDensityByDirectionLayer,
                                   Projection projection) {
        super(pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, railwayNetwork, 
                simulationControlLayer);
        this.onDemandVehicleLayer = onDemandVehicleLayer;
        this.trafficDensityLayer = trafficDensityLayer;
        this.nodeIdLayer = nodeIdLayer;
        this.onDemandVehicleStationsLayer = onDemandVehicleStationsLayer;
        this.demandLayer = demandLayer;
		this.onDemandVehiclePlanLayer = onDemandVehiclePlanLayer;
        this.highwayLayer = highwayLayer;
        this.bufferedHighwayLayer = bufferedHighwayLayer;
        this.trafficDensityByDirectionLayer = trafficDensityByDirectionLayer;
        this.backgroundLayer = ColorLayer.create(Color.white);
    }

    @Override
    protected void initEntityLayers(Simulation simulation, Projection projection) {
        VisManager.registerLayer(onDemandVehicleStationsLayer);
        VisManager.registerLayer(onDemandVehicleLayer);
        VisManager.registerLayer(demandLayer);
		VisManager.registerLayer(onDemandVehiclePlanLayer);
    }

    @Override
    protected void initLayersBeforeEntityLayers() {
//        VisManager.registerLayer(trafficDensityLayer);
//        VisManager.registerLayer(trafficDensityByDirectionLayer);
    }

    @Override
    protected void initLayersAfterEntityLayers() {
//        VisManager.registerLayer(nodeIdLayer);
    }

    @Override
    protected void initGraphLayers(Projection projection) {
        VisManager.registerLayer(backgroundLayer);
//        VisManager.registerLayer(highwayLayer);
        VisManager.registerLayer(bufferedHighwayLayer);
    }
    
    
}
