/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.simulator.creator.SimulationFinishedListener;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.statistics.Statistics;
import org.junit.Assert;

/**
 *
 * @author fido
 */
@Singleton
public class StatisticControl  implements SimulationFinishedListener, EventHandler{
    private int demandFinishDrivingCounter;
    
    private final Statistics statistics;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
//    private final SimulationCreator simulationCreator;
    
    
    

    @Inject
    public StatisticControl(Statistics statistics, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            SimulationCreator simulationCreator, EventProcessor eventProcessor) {
        this.statistics = statistics;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.demandFinishDrivingCounter = 0;
        simulationCreator.addSimulationFinishedListener(this);
        eventProcessor.addEventHandler(this);
    }
            
    public void incrementDemandFinishDrivingCounter(){
        demandFinishDrivingCounter++;
    }

    @Override
    public void simulationFinished() {
        
        // compares demand count in results with demands which really left the station
        Assert.assertEquals(onDemandVehicleStationsCentral.getDemandsCount(),
                statistics.getNumberOfVehiclsLeftStationToServeDemand());
        
        // compares demand count in results with demands which MOD vehicle reached target station
        Assert.assertEquals(onDemandVehicleStationsCentral.getDemandsCount(),
                demandFinishDrivingCounter);
    }

    @Override
    public EventProcessor getEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleEvent(Event event) {
//       if()
    }
}