package cz.cvut.fel.aic.amodsim.visio;

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
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NodeIdLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.SimulationControlLayer;
import cz.cvut.fel.aic.alite.simulation.Simulation;
import cz.cvut.fel.aic.alite.vis.VisManager;
import cz.cvut.fel.aic.alite.vis.layer.common.ColorLayer;
import java.awt.Color;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class MapVisualizerVisioInitializer  extends DefaultVisioInitializer{
	
	private final HighwayLayer highwayLayer;
	
	private final NodeIdLayer nodeIdLayer;
	
	@Inject
	public MapVisualizerVisioInitializer(Simulation simulation, PedestrianNetwork pedestrianNetwork, BikewayNetwork bikewayNetwork,
			HighwayNetwork highwayNetwork, TramwayNetwork tramwayNetwork, MetrowayNetwork metrowayNetwork, 
			RailwayNetwork railwayNetwork, SimulationControlLayer simulationControlLayer, GridLayer gridLayer,
			HighwayLayer highwayLayer, NodeIdLayer nodeIdLayer, AgentpolisConfig config) {
		super(simulation, pedestrianNetwork, bikewayNetwork, highwayNetwork, tramwayNetwork, metrowayNetwork, railwayNetwork,
				simulationControlLayer, gridLayer, config);
		this.highwayLayer = highwayLayer;
		this.nodeIdLayer = nodeIdLayer;
	}
	
	@Override
	protected void initGraphLayers() {
		VisManager.registerLayer(ColorLayer.create(Color.white));
		super.initGraphLayers();
		VisManager.registerLayer(highwayLayer);
	}

	@Override
	protected void initEntityLayers(Simulation simulation) {
	}
	
	@Override
	protected void initLayersAfterEntityLayers() {
		VisManager.registerLayer(nodeIdLayer);
	}
	
}
