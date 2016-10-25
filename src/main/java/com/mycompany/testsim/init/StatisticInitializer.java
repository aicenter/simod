/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.statistics.Statistics;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.alite.common.event.EventProcessor;

/**
 *
 * @author fido
 */
@Singleton
public class StatisticInitializer {
    
    private final Statistics statistics;
    
    private final SimulationCreator simulationCreator;
    
    private final EventProcessor eventProcessor;

    
    
    @Inject
    public StatisticInitializer(Statistics statistics, SimulationCreator simulationCreator,
            EventProcessor eventProcessor) {
        this.statistics = statistics;
        this.simulationCreator = simulationCreator;
        this.eventProcessor = eventProcessor;
    }
    
    
    
    
    
    
    public void initialize(){
        simulationCreator.addSimulationFinishedListener(statistics);
        eventProcessor.addEvent(statistics);
    }
}
