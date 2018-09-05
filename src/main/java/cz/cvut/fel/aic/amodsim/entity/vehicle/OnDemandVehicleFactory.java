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
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleFactory implements OnDemandVehicleFactorySpec{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehicleFactory.class);
    
    protected final TripsUtil tripsUtil;
    protected final boolean precomputedPaths;
    protected final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    protected final PhysicalVehicleDriveFactory driveActivityFactory;
    protected final PositionUtil positionUtil;
    protected final EventProcessor eventProcessor;
    protected final StandardTimeProvider timeProvider;
    protected final IdGenerator rebalancingIdGenerator;
    protected final PhysicalTransportVehicleStorage vehicleStorage;
    protected final AmodsimConfig config;
    
    private int vehicleCounter;
 
    
    
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
        this.vehicleCounter = 0;
    }
    
    
    
    @Override
    public OnDemandVehicle create(String vehicleId, SimulationNode startPosition){
        
        LOGGER.info("%d vehicle (id = %s)", ++this.vehicleCounter, vehicleId);
        return new OnDemandVehicle(vehicleStorage, tripsUtil, 
                onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, 
                precomputedPaths, rebalancingIdGenerator, config, vehicleId, startPosition);
                
    }
}
