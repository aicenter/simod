/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.HighwayLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.cvut.fel.aic.alite.vis.VisManager;
import cz.cvut.fel.aic.alite.vis.layer.common.ColorLayer;
import cz.cvut.fel.aic.amodsim.visio.AmodsimVisioInItializer;
import cz.cvut.fel.aic.amodsim.visio.BufferedHighwayLayer;
import cz.cvut.fel.aic.amodsim.visio.DemandLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.cvut.fel.aic.amodsim.visio.OnDemandVehicleLayer;
import cz.cvut.fel.aic.amodsim.visio.OnDemandVehiclePlanLayer;
import cz.cvut.fel.aic.amodsim.visio.OnDemandVehicleStationsLayer;
import cz.cvut.fel.aic.amodsim.visio.TrafficDensityByDirectionLayer;
import cz.cvut.fel.aic.amodsim.visio.TrafficDensityLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.GridLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.MapTilesLayer;
import java.awt.Color;

/**
 *
 * @author fido
 */
@Singleton
public class MapVisualizerVisioInItializer extends AmodsimVisioInItializer{
    
    @Inject
    public MapVisualizerVisioInItializer(PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork, 
            HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork, 
            RailwayNetwork railwayNetwork, AgentStorage agentStorage, VehicleStorage vehicleStorage,
            AllNetworkNodes allNetworkNodes, SimulationCreator simulationCreator, 
            OnDemandVehicleLayer onDemandVehicleLayer, TrafficDensityLayer trafficDensityLayer, 
            NodeIdLayer nodeIdLayer, OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, 
            DemandLayer demandLayer, OnDemandVehiclePlanLayer onDemandVehiclePlanLayer, HighwayLayer highwayLayer, 
            BufferedHighwayLayer bufferedHighwayLayer, SimulationControlLayer simulationControlLayer, 
            TrafficDensityByDirectionLayer trafficDensityByDirectionLayer, GridLayer gridLayer, MapTilesLayer mapTilesLayer) {
        super(pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, 
                railwayNetwork, agentStorage, vehicleStorage, allNetworkNodes, simulationCreator, onDemandVehicleLayer, trafficDensityLayer, nodeIdLayer, 
                onDemandVehicleStationsLayer, demandLayer, onDemandVehiclePlanLayer, highwayLayer, 
                bufferedHighwayLayer, simulationControlLayer, trafficDensityByDirectionLayer, gridLayer, mapTilesLayer);
    }

    @Override
    protected void initGraphLayers() {
        VisManager.registerLayer(ColorLayer.create(Color.white));
        VisManager.registerLayer(gridLayer);
        VisManager.registerLayer(highwayLayer);
//        VisManager.registerLayer();
//        VisManager.registerLayer(bufferedHighwayLayer);
    }
    
    @Override
    protected void initLayersAfterEntityLayers() {
//        VisManager.registerLayer(nodeIdLayer);
    }
    
    
}
