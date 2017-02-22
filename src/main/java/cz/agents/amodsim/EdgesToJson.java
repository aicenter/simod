/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import com.google.inject.Injector;
import cz.agents.agentpolis.AgentPolisInitializer;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.amodsim.config.Config;
import cz.agents.basestructures.Graph;
import java.io.File;

/**
 *
 * @author fido
 */
public class EdgesToJson {
    
    public static void main(String[] args) {
        
        Config config = new Configuration().load();
        AmodsimAgentPolisConfiguration configuration = new AmodsimAgentPolisConfiguration(config);
        
        Injector injector = new AgentPolisInitializer(configuration, new MainModule(config)).initialize();
        
        MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);

//        Graph<RoadNode, RoadEdge> roadGraph = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);

// TODO - modify OsmUtilTo Support new MapInitializer
        Graph<SimulationNode, SimulationEdge> roadGraph 
                = OsmUtil.getSimulationGraph(mapInitializer);
        String modifier = config.agentpolis.simplifyGraph ? "-simplified" : "";
        OsmUtil.edgesToJson(roadGraph, new File(config.agentpolis.edgesFilePath + modifier + ".json"));
        OsmUtil.edgePairsToJson(roadGraph, new File(config.agentpolis.edgePairsFilePath + modifier + ".json"));
    }
}
