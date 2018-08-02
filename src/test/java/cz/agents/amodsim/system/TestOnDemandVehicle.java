/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.util.Map;
import org.junit.Assert;

/**
 *
 * @author fido
 */
public class TestOnDemandVehicle extends OnDemandVehicle{
    
    private final StatisticControl statisticControl;
    
    private int tripsToStationFinished;
    
    private int demandTripsFinished;
    
    private int droppOffCount;
    
    @Inject
    public TestOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage,
            TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            StandardDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor, 
            StandardTimeProvider timeProvider, StatisticControl statisticControl,
            @Named("precomputedPaths") boolean precomputedPaths, IdGenerator rebalancingIdGenerator, AmodsimConfig config, 
            @Assisted String vehicleId, @Assisted SimulationNode startPosition) {
        super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral,
                driveActivityFactory, positionUtil, eventProcessor, timeProvider, 
                precomputedPaths, rebalancingIdGenerator, config, vehicleId, startPosition);
        this.statisticControl = statisticControl;
        demandTripsFinished = 0;
        tripsToStationFinished = 0;
        droppOffCount = 0;
    }

    /*@Override
    public void finishedDriving() {
        OnDemandVehicleState stateBeforeActions = state;   
        
        // normal actions
        //super.finishedDriving();
        
        if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_STATION || 
                (stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION && tripToStation == null)){
            tripsToStationFinished++;
            Assert.assertEquals(droppOffCount, tripsToStationFinished);
            
            statisticControl.incrementDemandFinishDrivingCounter();
            
//            Assert.assertEquals(0, cargo.size());
        }
        if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION){
            demandTripsFinished++;
        }
        Assert.assertTrue(demandTripsFinished == tripsToStationFinished 
                || demandTripsFinished == tripsToStationFinished + 1);
    }*/

    @Override
    protected void dropOffDemand() {
        droppOffCount++;
        super.dropOffDemand(); 
    }
    
    
    
    
    
}
