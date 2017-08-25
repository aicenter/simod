/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.Config;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.util.Map;

/**
 *
 * @author fido
 */

@Singleton
public class OnDemandVehicleStationFactory {
    
    private final Config config;
    
    private final EventProcessor eventProcessor;
    
    private final OnDemandVehicleFactory onDemandVehicleFactory;
    
    private final NearestElementUtils nearestElementUtils;
    
    private final OnDemandvehicleStationStorage onDemandVehicleStationStorage;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    private final Transformer transformer;
    
    private final PositionUtil positionUtil;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final Map<Long,SimulationNode> nodesMappedByNodeSourceIds;

    @Inject
    public OnDemandVehicleStationFactory(Config config, EventProcessor eventProcessor, OnDemandVehicleFactory 
            onDemandVehicleFactory, NearestElementUtils nearestElementUtils, OnDemandvehicleStationStorage 
                    onDemandVehicleStationStorage, OnDemandVehicleStorage onDemandVehicleStorage,
                    Transformer transformer, PositionUtil positionUtil, 
                    OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
                    Map<Long, SimulationNode> nodesMappedByNodeSourceIds) {
        this.config = config;
        this.eventProcessor = eventProcessor;
        this.onDemandVehicleFactory = onDemandVehicleFactory;
        this.nearestElementUtils = nearestElementUtils;
        this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.transformer = transformer;
        this.positionUtil = positionUtil;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
    }
    
    
    
    
    public OnDemandVehicleStation create(String id, SimulationNode node, int initialVehicleCount){
        return new OnDemandVehicleStation(config, eventProcessor, onDemandVehicleFactory, nearestElementUtils,
                onDemandVehicleStationStorage, onDemandVehicleStorage, id, node, 
                initialVehicleCount, transformer, positionUtil, 
                onDemandVehicleStationsCentral, nodesMappedByNodeSourceIds);
    }
}
