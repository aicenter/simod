/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.amodsim.storage.OnDemandvehicleStationStorage;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
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
    
    private final AgentPositionModel agentPositionModel;
    
    private final VehiclePositionModel vehiclePositionModel;
    
    private final VehiclePositionUtil vehiclePositionUtil;
    
    private final Transformer transformer;
    
    private final PositionUtil positionUtil;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;

    @Inject
    public OnDemandVehicleStationFactory(Config config, EventProcessor eventProcessor, OnDemandVehicleFactory 
            onDemandVehicleFactory, NearestElementUtils nearestElementUtils, OnDemandvehicleStationStorage 
                    onDemandVehicleStationStorage, OnDemandVehicleStorage onDemandVehicleStorage,
                    AgentPositionModel agentPositionModel, VehiclePositionModel vehiclePositionModel, 
                    VehiclePositionUtil vehiclePositionUtil, Transformer transformer, PositionUtil positionUtil, 
                    OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
                    Map<Long, Node> nodesMappedByNodeSourceIds) {
        this.config = config;
        this.eventProcessor = eventProcessor;
        this.onDemandVehicleFactory = onDemandVehicleFactory;
        this.nearestElementUtils = nearestElementUtils;
        this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.agentPositionModel = agentPositionModel;
        this.vehiclePositionModel = vehiclePositionModel;
        this.vehiclePositionUtil = vehiclePositionUtil;
        this.transformer = transformer;
        this.positionUtil = positionUtil;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
    }
    
    
    
    
    public OnDemandVehicleStation create(String id, Node node, int initialVehicleCount){
        return new OnDemandVehicleStation(config, eventProcessor, onDemandVehicleFactory, nearestElementUtils,
                onDemandVehicleStationStorage, onDemandVehicleStorage, id, agentPositionModel, node, 
                initialVehicleCount, vehiclePositionModel, vehiclePositionUtil, transformer, positionUtil, 
                onDemandVehicleStationsCentral, nodesMappedByNodeSourceIds);
    }
}
