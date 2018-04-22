/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.graphbuilder.SimulationEdgeFactory;
import cz.cvut.fel.aic.amodsim.graphbuilder.SimulationNodeFactory;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.GraphCreator;
import cz.cvut.fel.aic.graphimporter.geojson.GeoJSONReader;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author david
 */
@Singleton
public class MapInitializer {

    private static final Logger LOGGER = Logger.getLogger(MapInitializer.class);

    private final Transformer projection;

    private final Set<TransportMode> allowedOsmModes;
	
	private final AmodsimConfig amodsimConfig;


    @Inject
    public MapInitializer(Transformer projection, Set<TransportMode> allowedOsmModes, AmodsimConfig amodsimConfig) {
        this.projection = projection;
        this.allowedOsmModes = allowedOsmModes;
		this.amodsimConfig = amodsimConfig;
    }


    /**
     * init map
     *
     * @return map data with simulation graph
     */
    public MapData getMap() {
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphs = new HashMap<>();
//        OsmImporter importer = new OsmImporter(mapFile, allowedOsmModes, projection);
        GeoJSONReader importer = new GeoJSONReader(amodsimConfig.mapEdgesFilepath, amodsimConfig.mapNodesFilepath,
				projection);

        GraphCreator<SimulationNode, SimulationEdge> graphCreator = new GraphCreator(
                true, true, importer, new SimulationNodeFactory(), new SimulationEdgeFactory());

        graphs.put(EGraphType.HIGHWAY, graphCreator.getMap());

        Map<Integer, SimulationNode> nodes = createAllGraphNodes(graphs);

        LOGGER.info("Graphs imported, highway graph details: " + graphs.get(EGraphType.HIGHWAY));
        return new MapData(graphs, nodes);
    }

    /**
     * Build map data
     */
    private Map<Integer, SimulationNode> createAllGraphNodes(Map<GraphType, Graph<SimulationNode, SimulationEdge>> graphByGraphType) {

        Map<Integer, SimulationNode> nodesFromAllGraphs = new HashMap<>();

        for (GraphType graphType : graphByGraphType.keySet()) {
            Graph<SimulationNode, SimulationEdge> graphStorageTmp = graphByGraphType.get(graphType);
            for (SimulationNode node : graphStorageTmp.getAllNodes()) {
                nodesFromAllGraphs.put(node.getId(), node);
            }

        }

        return nodesFromAllGraphs;

    }
}
