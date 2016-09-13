/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.google.inject.Injector;
import cz.agents.agentpolis.simmodel.environment.AgentPolisEnvironmentModule;
import cz.agents.agentpolis.simmodel.environment.EnvironmentFactory;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.delaymodel.factory.DelayingSegmentCapacityDeterminer;
import cz.agents.alite.simulation.Simulation;
import cz.agents.basestructures.Graph;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author david
 */
public class MyEnvironmentFactory implements EnvironmentFactory{
	
	private final DelayingSegmentCapacityDeterminer delayingSegmentCapacityDeterminer;

    public MyEnvironmentFactory(DelayingSegmentCapacityDeterminer delayingSegmentCapacityDeterminer) {
        super();

        this.delayingSegmentCapacityDeterminer = delayingSegmentCapacityDeterminer;
    }

	@Override
	public Injector injectEnvironment(Injector injector, Simulation simulation, long seed, Map<GraphType, 
			Graph<SimulationNode, SimulationEdge>> graphByGraphType, Map<Integer, SimulationNode> nodesFromAllGraphs) {
		ZonedDateTime initDate = ZonedDateTime.now();
		injector = injector.createChildInjector(new AgentPolisEnvironmentModule(simulation, new Random(),
                graphByGraphType, nodesFromAllGraphs, delayingSegmentCapacityDeterminer, initDate));

        return injector.createChildInjector(new MyEnvironment());
	}
	
}
