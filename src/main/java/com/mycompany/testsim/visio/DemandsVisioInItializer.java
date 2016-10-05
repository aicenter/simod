/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.AgentStorage;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.BikewayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.MetrowayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.PedestrianNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.RailwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.TramwayNetwork;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.visualization.visio.Projection;
import cz.agents.agentpolis.simulator.visualization.visio.DefaultVisioInitializer;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.VisManager;

/**
 *
 * @author fido
 */
public class DemandsVisioInItializer extends DefaultVisioInitializer{
    
    private final DemandVisualisationLayer demandVisualisationLayer;
    
    private final TrafficDensityLayer trafficDensityLayer;
    
    @Inject
    public DemandsVisioInItializer(PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork, 
            HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork, 
            RailwayNetwork railwayNetwork, EntityStorage<AgentPolisEntity> entityStorage, AgentStorage agentStorage, 
            VehicleStorage vehicleStorage, AgentPositionModel agentPositionModel, 
            VehiclePositionModel vehiclePositionModel, AllNetworkNodes allNetworkNodes, 
            SimulationCreator simulationCreator, DemandVisualisationLayer demandVisualisationLayer,
            TrafficDensityLayer trafficDensityLayer) {
        super(pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, railwayNetwork, 
                entityStorage, agentStorage, vehicleStorage, agentPositionModel, vehiclePositionModel, allNetworkNodes, 
                simulationCreator);
        this.demandVisualisationLayer = demandVisualisationLayer;
        this.trafficDensityLayer = trafficDensityLayer;
    }

    @Override
    protected void initEntityLayers(Simulation simulation, Projection projection) {
        VisManager.registerLayer(demandVisualisationLayer);
    }

    @Override
    protected void initLayersBeforeEntityLayers() {
        VisManager.registerLayer(trafficDensityLayer);
    }
    
    
    
}
