/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.Graph;

/**
 *
 * @author travnja5
 */
public class TestMapInitializer extends MapInitializer{
    private final Graph<SimulationNode, SimulationEdge> graph;

    public TestMapInitializer(Graph<SimulationNode, SimulationEdge> graph, AgentpolisConfig config) {
        super(config);
        this.graph = graph;
    }

    @Override
    protected Graph<SimulationNode, SimulationEdge> getGraph() {
        return graph;
    }
    
}
