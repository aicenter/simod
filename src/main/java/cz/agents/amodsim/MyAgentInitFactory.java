/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import com.google.inject.Injector;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simulator.creator.initializator.AgentInitFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class MyAgentInitFactory implements AgentInitFactory{

	@Override
	public List<Agent> initAllAgentLifeCycles(Injector injector) {
		List<Agent> agents = new ArrayList<Agent>();
		
		agents.add(new MyAgent("tets", DemandSimulationEntityType.TEST_TYPE, injector));
		
		return agents;
	}
	
}
