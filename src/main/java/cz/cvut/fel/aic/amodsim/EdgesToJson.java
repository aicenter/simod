/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;

/**
 *
 * @author fido
 */
public class EdgesToJson {
	
	public static void main(String[] args) {
		
		AmodsimConfig config = new AmodsimConfig();
		
		File localConfigFile = args.length > 0 ? new File(args[0]) : null;
		
		Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();
		
		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);

//		Graph<RoadNode, RoadEdge> roadGraph = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);

// TODO - modify OsmUtilTo Support new MapInitializer
		Graph<SimulationNode, SimulationEdge> roadGraph 
				= OsmUtil.getSimulationGraph(mapInitializer);
		String modifier = config.simplifyGraph ? "-simplified" : "";
		OsmUtil.edgesToJson(roadGraph, new File(config.edgesFilePath + modifier + ".json"));
		OsmUtil.edgePairsToJson(roadGraph, new File(config.edgePairsFilePath + modifier + ".json"));
	}
}
