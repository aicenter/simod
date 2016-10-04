/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.visio.DemandsVisioInItializer;
import cz.agents.agentpolis.simmodel.environment.EnvironmentFactory;
import cz.agents.agentpolis.simmodel.environment.StandardAgentPolisModule;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.AgentStorage;
import cz.agents.agentpolis.simmodel.environment.model.EntityPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import cz.agents.agentpolis.simulator.creator.SimulationParameters;
import cz.agents.agentpolis.simulator.visualization.visio.VisioInitializer;

/**
 *
 * @author fido
 */
public class MainModule extends StandardAgentPolisModule{
    
    public MainModule(EnvironmentFactory envinromentFactory, SimulationParameters parameters) {
        super(envinromentFactory, parameters);
    }

    @Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(DemandsVisioInItializer.class);
    }

    @Override
    protected void configureNext() {
        bind(EntityPositionModel.class).to(AgentPositionModel.class);
        bind(EntityStorage.class).to(AgentStorage.class);
    }
    
    
}
