/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.AgentStorage;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.BikewayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.MetrowayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.PedestrianNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.RailwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.TramwayNetwork;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.visualization.visio.HighwayLayer;
import cz.agents.agentpolis.simulator.visualization.visio.Projection;
import cz.agents.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.agents.alite.vis.VisManager;
import cz.agents.alite.vis.layer.common.ColorLayer;
import cz.agents.amodsim.visio.AmodsimVisioInItializer;
import cz.agents.amodsim.visio.BufferedHighwayLayer;
import cz.agents.amodsim.visio.DemandLayer;
import cz.agents.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.agents.amodsim.visio.OnDemandVehicleLayer;
import cz.agents.amodsim.visio.OnDemandVehiclePlanLayer;
import cz.agents.amodsim.visio.OnDemandVehicleStationsLayer;
import cz.agents.amodsim.visio.TrafficDensityByDirectionLayer;
import cz.agents.amodsim.visio.TrafficDensityLayer;
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
            AgentPositionModel agentPositionModel, VehiclePositionModel vehiclePositionModel, 
            AllNetworkNodes allNetworkNodes, SimulationCreator simulationCreator, 
            OnDemandVehicleLayer onDemandVehicleLayer, TrafficDensityLayer trafficDensityLayer, 
            NodeIdLayer nodeIdLayer, OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, 
            DemandLayer demandLayer, OnDemandVehiclePlanLayer onDemandVehiclePlanLayer, HighwayLayer highwayLayer, 
            BufferedHighwayLayer bufferedHighwayLayer, SimulationControlLayer simulationControlLayer, 
            TrafficDensityByDirectionLayer trafficDensityByDirectionLayer, Projection projection) {
        super(pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, 
                railwayNetwork, agentStorage, vehicleStorage, agentPositionModel, vehiclePositionModel, 
                allNetworkNodes, simulationCreator, onDemandVehicleLayer, trafficDensityLayer, nodeIdLayer, 
                onDemandVehicleStationsLayer, demandLayer, onDemandVehiclePlanLayer, highwayLayer, 
                bufferedHighwayLayer, simulationControlLayer, trafficDensityByDirectionLayer, projection);
    }

    @Override
    protected void initGraphLayers(Projection projection) {
        VisManager.registerLayer(ColorLayer.create(Color.white));
        VisManager.registerLayer(highwayLayer);
//        VisManager.registerLayer();
//        VisManager.registerLayer(bufferedHighwayLayer);
    }
    
    @Override
    protected void initLayersAfterEntityLayers() {
        VisManager.registerLayer(nodeIdLayer);
    }
    
    
}
