/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.google.common.collect.Sets;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.CongestedDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;
import cz.cvut.fel.aic.amodsim.visio.DemandLayer;
import cz.cvut.fel.aic.amodsim.visio.DemandLayerWithJitter;
import cz.cvut.fel.aic.amodsim.visio.AmodsimVisioInItializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.VehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.InsertionHeuristicSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.RidesharingStationsCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.geographtools.TransportMode;
import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cz.cvut.fel.aic.amodsim.ridesharing.InsertionSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.tabusearch.TabuSearchSolver;

/**
 *
 * @author fido
 */
public class MainModule extends StandardAgentPolisModule{
    
    private final AmodsimConfig amodsimConfig;
    
    public MainModule(AmodsimConfig amodsimConfig, File localConfigFile) {
        super(amodsimConfig, localConfigFile, "agentpolis");
        this.amodsimConfig = amodsimConfig;
    }

    @Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(AmodsimVisioInItializer.class);
    }

    @Override
    protected void configureNext() {
        bindConstant().annotatedWith(Names.named("precomputedPaths")).to(false);

//        bind(File.class).annotatedWith(Names.named("osm File")).toInstance(new File(amodsimConfig.mapFilePath));
        
        bind(new TypeLiteral<Set<TransportMode>>(){}).toInstance(Sets.immutableEnumSet(TransportMode.CAR));
        bind(AmodsimConfig.class).toInstance(amodsimConfig);

        bind(EntityStorage.class).to(VehicleStorage.class);
        
        if(amodsimConfig.amodsim.useTripCache){
            bind(TripsUtil.class).to(TripsUtilCached.class);
        }
        bind(DemandLayer.class).to(DemandLayerWithJitter.class);

        
//      bind(PhysicalVehicleDriveFactory.class).to(CongestedDriveFactory.class);
        bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);

        if(amodsimConfig.amodsim.ridesharing.on){
            bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
            bind(OnDemandVehicleStationsCentral.class).to(RidesharingStationsCentral.class);
            bind(DARPSolver.class).to(InsertionHeuristicSolver.class);
            //bind(DARPSolver.class).to(InsertionSolver.class);
            //bind(DARPSolver.class).to(TabuSearchSolver.class);
            bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
        } else{
           bind(OnDemandVehicleFactorySpec.class).to(OnDemandVehicleFactory.class);
        }
        install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
            .build(DemandAgentFactory.class));
    }
    
    @Provides
	@Singleton
	Map<Long,SimulationNode> provideNodesMappedByNodeSourceIds(
            HighwayNetwork highwayNetwork, AllNetworkNodes allNetworkNodes) {
            Map<Long,Integer> nodeIdsMappedByNodeSourceIds = highwayNetwork.getNetwork().createSourceIdToNodeIdMap();
            Map<Long,SimulationNode> nodesMappedByNodeSourceIds = new HashMap<>();
        
            for (Map.Entry<Long, Integer> entry : nodeIdsMappedByNodeSourceIds.entrySet()) {
                Long key = entry.getKey();
                Integer value = entry.getValue();
                nodesMappedByNodeSourceIds.put(key, allNetworkNodes.getNode(value));
            }
            return nodesMappedByNodeSourceIds;
	}
    
    
}
