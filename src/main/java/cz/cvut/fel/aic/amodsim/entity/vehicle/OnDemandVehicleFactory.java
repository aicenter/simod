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
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.util.Map;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleFactory implements OnDemandVehicleFactorySpec{
    
    protected final TripsUtil tripsUtil;
    
    private final boolean precomputedPaths;
    
    protected final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    protected final PhysicalVehicleDriveFactory driveActivityFactory;
    
    private final PositionUtil positionUtil;
    
    private final EventProcessor eventProcessor;
    
    private final StandardTimeProvider timeProvider;
    
    private final IdGenerator rebalancingIdGenerator;
    
    private final PhysicalTransportVehicleStorage vehicleStorage;
    
    private final AmodsimConfig config;

    
    
    
    @Inject
    public OnDemandVehicleFactory(PhysicalTransportVehicleStorage vehicleStorage, 
            TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            PhysicalVehicleDriveFactory driveActivityFactory, PositionUtil positionUtil, EventProcessor eventProcessor,
            StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, 
            @Named("precomputedPaths") boolean precomputedPaths, AmodsimConfig config) {
        this.tripsUtil = tripsUtil;
        this.precomputedPaths = precomputedPaths;
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
                precomputedPaths, rebalancingIdGenerator, config, vehicleId, startPosition);
    }
}
