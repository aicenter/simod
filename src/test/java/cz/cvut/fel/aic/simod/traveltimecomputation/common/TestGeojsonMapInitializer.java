/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod.traveltimecomputation.common;

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
		String package_path = "cz/cvut/fel/aic/simod/traveltimecomputation/";
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
