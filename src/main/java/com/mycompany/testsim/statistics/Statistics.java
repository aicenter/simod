/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.AllEdgesLoad;
import cz.agents.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
@Singleton
public class Statistics extends EventHandlerAdapter implements SimulationFinishedListener {
    
    public static final long STATISTIC_INTERVAL = 60000;
    
    private static final File RESULT_FILE = new File("result.json");
    
    
    
    private final EventProcessor eventProcessor;
    
    private final LinkedList<Double> averageEdgeLoad;
    
    private final Provider<AllEdgesLoad> allEdgesLoadProvider;
    
    
    private long tickCount;
    
    private int maxLoad;
    
    
    
    
    @Inject
    public Statistics(EventProcessor eventProcessor, Provider<AllEdgesLoad> allEdgesLoadProvider) {
        this.eventProcessor = eventProcessor;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        tickCount = 0;
        averageEdgeLoad = new LinkedList<>();
        maxLoad = 0;
    }
    
    
    

    @Override
    public void handleEvent(Event event) {
        handleTick();
    }

    
    private void handleTick() {
        tickCount++;
        measure();
        eventProcessor.addEvent(this, STATISTIC_INTERVAL);
    }

    private void measure() {
        countEdgeLoadForInterval();
    }
    
    
    private void saveResult(){
        double averageLoadTotal = countAverageEdgeLoad();
        
        Result result = new Result(tickCount, averageLoadTotal, maxLoad);
        
        ObjectMapper mapper = new ObjectMapper();
		
        try {
            mapper.writeValue(RESULT_FILE, result);
        } catch (IOException ex) {
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void simulationFinished() {
        saveResult();
    }

    @Inject
    private void countEdgeLoadForInterval() {
        AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();
        int edgeLoadTotal = 0;
        for (int edgeLoad : allEdgesLoad.loadsIterator) {
            edgeLoadTotal += edgeLoad;
            if(edgeLoad > maxLoad){
                maxLoad = edgeLoad;
            }
        }
        averageEdgeLoad.add((double) edgeLoadTotal / allEdgesLoad.getNumberOfEdges());
    }

    private double countAverageEdgeLoad() {
        double totalAverageLoad = 0;
        for (double loadForFrame : averageEdgeLoad) {
            totalAverageLoad += loadForFrame;
        }
        return totalAverageLoad / averageEdgeLoad.size();
    }
    
    
}
