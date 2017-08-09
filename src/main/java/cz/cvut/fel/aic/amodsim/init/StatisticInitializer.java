/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.statistics.StatisticEvent;

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
        eventProcessor.addEvent(StatisticEvent.TICK, statistics, null, null);
    }
}
