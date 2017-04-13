/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.agents.agentpolis.siminfrastructure.time.TimeProvider;
import cz.agents.agentpolis.simmodel.activity.activityFactory.DriveActivityFactory;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.tripUtil.TripsUtil;
import cz.agents.basestructures.Node;
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
    public TestOnDemandVehicle(Map<Long, Node> nodesMappedByNodeSourceIds, VehicleStorage vehicleStorage,
            TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            DriveActivityFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor, 
            TimeProvider timeProvider, StatisticControl statisticControl, 
            @Named("precomputedPaths") boolean precomputedPaths, @Assisted String vehicleId, 
            @Assisted Node startPosition) {
        super(nodesMappedByNodeSourceIds, vehicleStorage, tripsUtil, onDemandVehicleStationsCentral,
                driveActivityFactory, positionUtil, eventProcessor, timeProvider, precomputedPaths, vehicleId, 
                startPosition);
        this.statisticControl = statisticControl;
        demandTripsFinished = 0;
        tripsToStationFinished = 0;
        droppOffCount = 0;
    }

    @Override
    public void finishedDriving() {
        OnDemandVehicleState stateBeforeActions = state;   
        
        // normal actions
        super.finishedDriving(); 
        
        if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_STATION || 
                (stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION && tripToStation == null)){
            tripsToStationFinished++;
            Assert.assertEquals(droppOffCount, tripsToStationFinished);
            
            statisticControl.incrementDemandFinishDrivingCounter();
            
            Assert.assertEquals(0, cargo.size());
        }
        if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION){
            demandTripsFinished++;
        }
        Assert.assertTrue(demandTripsFinished == tripsToStationFinished 
                || demandTripsFinished == tripsToStationFinished + 1);
    }

    @Override
    protected void dropOffDemand() {
        droppOffCount++;
        super.dropOffDemand(); 
    }
    
    
    
    
    
}
