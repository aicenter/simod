/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.agentpolis.siminfrastructure.Log;
import com.google.common.collect.Sets;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import cz.agents.amodsim.entity.DemandAgent;
import cz.agents.amodsim.entity.DemandAgent.DemandAgentFactory;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.entity.OnDemandVehicleStation;
import cz.agents.amodsim.entity.OnDemandVehicleStation.OnDemandVehicleStationFactory;
import cz.agents.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.agents.agentpolis.simmodel.activity.activityFactory.CongestedDriveFactory;
import cz.agents.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.agents.amodsim.tripUtil.TripsUtilCached;
import cz.agents.amodsim.visio.DemandLayer;
import cz.agents.amodsim.visio.DemandLayerWithJitter;
import cz.agents.amodsim.visio.AmodsimVisioInItializer;
import cz.agents.agentpolis.simmodel.environment.StandardAgentPolisModule;
import cz.agents.agentpolis.simmodel.environment.model.EntityPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;
import cz.agents.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.agents.amodsim.entity.vehicle.RideSharingOnDemandVehicle;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author fido
 */
public class MainModule extends StandardAgentPolisModule{
    
    private final Config config;
    
    public MainModule(Config config) {
        super();
        this.config = config;
    }

    @Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(AmodsimVisioInItializer.class);
    }

    @Override
    protected void configureNext() {
        bindConstant().annotatedWith(Names.named("precomputedPaths")).to(false);

        bind(File.class).annotatedWith(Names.named("osm File")).toInstance(new File(config.mapFilePath));
        
        bind(new TypeLiteral<Set<ModeOfTransport>>(){}).toInstance(Sets.immutableEnumSet(ModeOfTransport.CAR));
        bind(Config.class).toInstance(config);
        bind(Transformer.class).toInstance(new Transformer(config.srid));
        
        bind(EntityPositionModel.class).to(VehiclePositionModel.class);
        bind(EntityStorage.class).to(VehicleStorage.class);
        
        if(config.agentpolis.useTripCache){
            bind(TripsUtil.class).to(TripsUtilCached.class);
        }
        bind(DemandLayer.class).to(DemandLayerWithJitter.class);
        
        bind(PhysicalVehicleDriveFactory.class).to(CongestedDriveFactory.class);

        if(config.agentpolis.ridesharing){
            install(new FactoryModuleBuilder().implement(OnDemandVehicle.class, RideSharingOnDemandVehicle.class)
                .build(OnDemandVehicleFactorySpec.class));
        }
        else{
            install(new FactoryModuleBuilder().implement(OnDemandVehicle.class, OnDemandVehicle.class)
                .build(OnDemandVehicleFactorySpec.class));
        }
//        install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, OnDemandVehicleStation.class)
//            .build(OnDemandVehicleStationFactory.class));
        install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
            .build(DemandAgentFactory.class));
    }
    
    @Provides
	@Singleton
	Map<Long,Node> provideNodesMappedByNodeSourceIds(HighwayNetwork highwayNetwork, AllNetworkNodes allNetworkNodes) {
        Map<Long,Integer> nodeIdsMappedByNodeSourceIds = highwayNetwork.getNetwork().createSourceIdToNodeIdMap();
        Map<Long,Node> nodesMappedByNodeSourceIds = new HashMap<>();
        
        for (Map.Entry<Long, Integer> entry : nodeIdsMappedByNodeSourceIds.entrySet()) {
            Long key = entry.getKey();
            Integer value = entry.getValue();
            nodesMappedByNodeSourceIds.put(key, allNetworkNodes.getNode(value));
        }
        
		return nodesMappedByNodeSourceIds;
	}
    
    
}
