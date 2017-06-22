/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.visio;

import cz.agents.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.agents.agentpolis.simulator.visualization.visio.HighwayLayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.AgentStorage;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.*;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.visualization.visio.DefaultVisioInitializer;
import cz.agents.agentpolis.simulator.visualization.visio.Projection;
import cz.agents.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.agents.alite.simulation.Simulation;
import cz.agents.alite.vis.VisManager;

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

    
    
    
    @Inject
    public AmodsimVisioInItializer(PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
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
//        VisManager.registerLayer(trafficDensityByDirectionLayer);
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
