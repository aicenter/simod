/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimulationEdgeFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimulationNodeFactory;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.GraphCreator;
import cz.cvut.fel.aic.graphimporter.geojson.GeoJSONReader;

/**
 *
 * @author matal
 */
public class TestGeojsonMapInitializer  extends MapInitializer{
	
	private final Transformer projection;
	
	
	
	@Inject
	public TestGeojsonMapInitializer(Transformer projection, AgentpolisConfig config) {
		super(config);
		this.projection = projection;
	}
	
	
	

	@Override
	protected Graph<SimulationNode, SimulationEdge> getGraph() {
                String package_path = "cz/agents/amodsim/ridesharing/traveltimecomputation/";
                String nodeFile = getClass().getClassLoader().getResource(package_path + "nodes.geojson").getPath();
		String edgeFile = getClass().getClassLoader().getResource(package_path + "edges.geojson").getPath();
		String serializedGraphFile = config.pathToSerializedGraph;
		GeoJSONReader importer = new GeoJSONReader(edgeFile, nodeFile, serializedGraphFile, projection);

		// beware that the simplifiction is alredy done in python preprocessing an is broken in java
		GraphCreator<SimulationNode, SimulationEdge> graphCreator = new GraphCreator(
				true, false, importer, new SimulationNodeFactory(), new SimulationEdgeFactory());

		return graphCreator.getMap();
	}
	
}
