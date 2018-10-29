/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.entity.vehicle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleFactory implements OnDemandVehicleFactorySpec{
    
    protected final TripsUtil tripsUtil;
    
    protected final StationsDispatcher onDemandVehicleStationsCentral;
    
    protected final PhysicalVehicleDriveFactory driveActivityFactory;
    
    protected final PositionUtil positionUtil;
    
    protected final EventProcessor eventProcessor;
    
    protected final StandardTimeProvider timeProvider;
    
    protected final IdGenerator rebalancingIdGenerator;
    
    protected final PhysicalTransportVehicleStorage vehicleStorage;
    
    protected final AmodsimConfig config;

    
    
    
    @Inject
    public OnDemandVehicleFactory(PhysicalTransportVehicleStorage vehicleStorage, 
            TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
            PhysicalVehicleDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor,
            StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, AmodsimConfig config) {
        this.tripsUtil = tripsUtil;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.driveActivityFactory = driveActivityFactory;
        this.positionUtil = positionUtil;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        this.rebalancingIdGenerator = rebalancingIdGenerator;
        this.vehicleStorage = vehicleStorage;
        this.config = config;
    }
    
    
    
    @Override
    public OnDemandVehicle create(String vehicleId, SimulationNode startPosition){
        return new OnDemandVehicle(vehicleStorage, tripsUtil, 
                onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, 
                rebalancingIdGenerator, config, vehicleId, startPosition);
    }
}
