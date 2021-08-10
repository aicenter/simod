/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.simod.traveltimecomputation.DistanceMatrixTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TNRTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TNRAFTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.CHTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.CongestedDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.VehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.rebalancing.RebalancingOnDemandVehicleStation;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.SingleVehicleDARPSolver;
import cz.cvut.fel.aic.simod.tripUtil.TripsUtilCached;
import cz.cvut.fel.aic.simod.visio.AmodsimVisioInItializer;
import cz.cvut.fel.aic.simod.visio.DemandLayer;
import cz.cvut.fel.aic.simod.visio.DemandLayerWithJitter;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
public class MainModule extends StandardAgentPolisModule{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MainModule.class);
	
	private final SimodConfig SimodConfig;
	
	public MainModule(SimodConfig SimodConfig, File localConfigFile) {
		super(SimodConfig, localConfigFile, "agentpolis");
		deleteFiles(new File(SimodConfig.simodExperimentDir));
		setLoggerFilePath(SimodConfig.simodExperimentDir);
		this.SimodConfig = SimodConfig;      
	}

	@Override
	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(AmodsimVisioInItializer.class);
	}

	@Override
	protected void configureNext() {	  
		bind(new TypeLiteral<Set<TransportMode>>(){}).toInstance(Sets.immutableEnumSet(TransportMode.CAR));
		bind(SimodConfig.class).toInstance(SimodConfig);
		bind(EntityStorage.class).to(VehicleStorage.class);
		bind(MapInitializer.class).to(GeojsonMapInitializer.class);
		
		if(SimodConfig.useTripCache){
			bind(TripsUtil.class).to(TripsUtilCached.class);
		}
		bind(DemandLayer.class).to(DemandLayerWithJitter.class);
		
		if(agentpolisConfig.congestionModel.on){
				bind(PhysicalVehicleDriveFactory.class).to(CongestedDriveFactory.class);
		} else {
				bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);
		}
	
		switch(SimodConfig.travelTimeProvider){
			case "dm":
				bind(TravelTimeProvider.class).to(DistanceMatrixTravelTimeProvider.class);
				break;
			case "euclidean":
				bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
				break;
			case "ch":
				bind(TravelTimeProvider.class).to(CHTravelTimeProvider.class);
				break;
			case "tnr":
				bind(TravelTimeProvider.class).to(TNRTravelTimeProvider.class);
				break;
			case "tnraf":
				bind(TravelTimeProvider.class).to(TNRAFTravelTimeProvider.class);
				break;
			case "astar":
			default:
				bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
				break;
		}

		if(SimodConfig.ridesharing.on){
			bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
			bind(StationsDispatcher.class).to(RidesharingDispatcher.class);
			bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
			install(new FactoryModuleBuilder().implement(DefaultPlanComputationRequest.class, DefaultPlanComputationRequest.class)
						.build(DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory.class));	
			switch(SimodConfig.ridesharing.method){
				case "insertion-heuristic":
					bind(DARPSolver.class).to(InsertionHeuristicSolver.class);
					break;
				case "vga":
					bind(DARPSolver.class).to(VehicleGroupAssignmentSolver.class);
					bind(SingleVehicleDARPSolver.class).to(ArrayOptimalVehiclePlanFinder.class);
		//			bind(OptimalVehiclePlanFinder.class).to(PlanBuilderOptimalVehiclePlanFinder.class);
					break;
			}
		}
		else{
			bind(OnDemandVehicleFactorySpec.class).to(OnDemandVehicleFactory.class);
		}
		install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
			.build(DemandAgentFactory.class));
		
		if(SimodConfig.rebalancing.on){
			install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, RebalancingOnDemandVehicleStation.class)
				.build(RebalancingOnDemandVehicleStation.OnDemandVehicleStationFactory.class));
		}
		else{
			install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, OnDemandVehicleStation.class)
				.build(OnDemandVehicleStation.OnDemandVehicleStationFactory.class));
		}
	} 
	
	private void deleteFiles(File folder) {
		if(folder.exists()){
			File[] files = folder.listFiles();
			for (final File fileEntry : files) {
				if (fileEntry.isDirectory() && !fileEntry.getName().startsWith("trip_cache")) {
					deleteFiles(fileEntry);
					fileEntry.delete();
				} else if(!fileEntry.isDirectory()){
					try {
						Files.delete(Paths.get(fileEntry.getAbsolutePath()));
					} catch (IOException ex) {
						LOGGER.error(ex.getMessage());
					}
				}
			}
		}
	}
	
	private void setLoggerFilePath(String experiments_path) {         
			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			FileAppender appender = (FileAppender) lc.getLogger("ROOT").getAppender("FILE");
			String logPath = experiments_path+"/log/log.txt";
			LOGGER.info("Setting log filepath to: {}", logPath);
			appender.setFile(logPath);
			appender.start();
	}
           
}
