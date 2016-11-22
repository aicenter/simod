/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.AgentStorage;
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
import cz.agents.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.VisManager;

/**
 *
 * @author fido
 */
public class DemandsVisioInitializer extends DefaultVisioInitializer{
    
    private final OnDemandVehicleLayer onDemandVehicleLayer;
    
    private final TrafficDensityLayer trafficDensityLayer;
    
    private final NodeIdLayer nodeIdLayer;
    
    private final DemandLayer demandLayer;
    
    private final OnDemandVehicleStationsLayer onDemandVehicleStationsLayer;
	
	private final OnDemandVehiclePlanLayer onDemandVehiclePlanLayer;
    
    private final HighwayLayer highwayLayer;
    
    private final BufferedHighwayLayer bufferedHighwayLayer;

    private final TrafficDensityByDirectionLayer trafficDensityByDirectionLayer;

    
    
    
    @Inject
    public DemandsVisioInitializer(PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
                                   HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork,
                                   RailwayNetwork railwayNetwork, AgentStorage agentStorage,
                                   VehicleStorage vehicleStorage, AgentPositionModel agentPositionModel,
                                   VehiclePositionModel vehiclePositionModel, AllNetworkNodes allNetworkNodes,
                                   SimulationCreator simulationCreator, OnDemandVehicleLayer onDemandVehicleLayer,
                                   TrafficDensityLayer trafficDensityLayer, NodeIdLayer nodeIdLayer,
                                   OnDemandVehicleStationsLayer onDemandVehicleStationsLayer, DemandLayer demandLayer,
                                   OnDemandVehiclePlanLayer onDemandVehiclePlanLayer, HighwayLayer highwayLayer,
                                   BufferedHighwayLayer bufferedHighwayLayer, SimulationControlLayer simulationControlLayer,
                                   TrafficDensityByDirectionLayer trafficDensityByDirectionLayer,
                                   Projection projection) {
        super(pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, railwayNetwork, 
                agentStorage, vehicleStorage, agentPositionModel, vehiclePositionModel, allNetworkNodes, 
                simulationCreator, simulationControlLayer, projection);
        this.onDemandVehicleLayer = onDemandVehicleLayer;
        this.trafficDensityLayer = trafficDensityLayer;
        this.nodeIdLayer = nodeIdLayer;
        this.onDemandVehicleStationsLayer = onDemandVehicleStationsLayer;
        this.demandLayer = demandLayer;
		this.onDemandVehiclePlanLayer = onDemandVehiclePlanLayer;
        this.highwayLayer = highwayLayer;
        this.bufferedHighwayLayer = bufferedHighwayLayer;
        this.trafficDensityByDirectionLayer = trafficDensityByDirectionLayer;
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
        VisManager.registerLayer(trafficDensityByDirectionLayer);
    }

    @Override
    protected void initLayersAfterEntityLayers() {
//        VisManager.registerLayer(nodeIdLayer);
    }

    @Override
    protected void initGraphLayers(Projection projection) {
        VisManager.registerLayer(bufferedHighwayLayer);
    }
    
    
}
