/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.mapInitialization.MapInitializer;
import ninja.fido.config.Configuration;
import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
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

//        Graph<RoadNode, RoadEdge> roadGraph = OsmUtil.getHigwayGraph(new File(config.mapFilePath), config.srid);

// TODO - modify OsmUtilTo Support new MapInitializer
        Graph<SimulationNode, SimulationEdge> roadGraph 
                = OsmUtil.getSimulationGraph(mapInitializer);
        String modifier = config.amodsim.simplifyGraph ? "-simplified" : "";
        OsmUtil.edgesToJson(roadGraph, new File(config.amodsim.edgesFilePath + modifier + ".json"));
        OsmUtil.edgePairsToJson(roadGraph, new File(config.amodsim.edgePairsFilePath + modifier + ".json"));
    }
}
