/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod.visual.ridesharing.vga.common;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.SingleVehicleDARPSolver;
import java.io.File;

/**
 *
 * @author fido
 */
public class TestModule extends StandardAgentPolisModule{
	
	private final SimodConfig SimodConfig;
	
	protected int roadWidth;

	public TestModule(SimodConfig SimodConfig, File localConfigFile) {
		super(SimodConfig, localConfigFile, "agentpolis"); 
		this.SimodConfig = SimodConfig;
		agentpolisConfig.visio.showVisio = VisualTests.SHOW_VISIO;
		this.SimodConfig.ridesharing.vga.groupGeneratorLogFilepath = new File("").getAbsolutePath();
		
		SimodConfig.startTime = 0;
		SimodConfig.tripsMultiplier = 1.0;
		
		SimodConfig.startTime = 0;
		SimodConfig.tripsMultiplier = 1.0;
		SimodConfig.ridesharing.on = true;
		SimodConfig.ridesharing.vga.logPlanComputationalTime = false;
		SimodConfig.ridesharing.vga.exportGroupData = false;
		SimodConfig.ridesharing.vga.groupGenerationTimeLimit = 0; //turns off limit for all tests
		SimodConfig.ridesharing.maxProlongationInSeconds = 300;
		SimodConfig.stations.on = false; 
		
		agentpolisConfig.simulationDuration.days = 0;
		agentpolisConfig.simulationDuration.hours = 0;
		agentpolisConfig.simulationDuration.minutes = 0;
		agentpolisConfig.simulationDuration.seconds = 120;
		
		roadWidth = 24;
	}

	
	
	
	@Override
	protected void configureNext() {
		super.configureNext();
		bind(SimodConfig.class).toInstance(SimodConfig);
		install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
			.build(DemandAgent.DemandAgentFactory.class));
		bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);
		bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
		bind(StationsDispatcher.class).to(RidesharingDispatcher.class);
		bind(DARPSolver.class).to(VehicleGroupAssignmentSolver.class);
//		bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
		bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
		bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
		install(new FactoryModuleBuilder().implement(DefaultPlanComputationRequest.class, DefaultPlanComputationRequest.class)
				.build(DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory.class));
		bind(SingleVehicleDARPSolver.class).to(ArrayOptimalVehiclePlanFinder.class);
//		bind(OptimalVehiclePlanFinder.class).to(PlanBuilderOptimalVehiclePlanFinder.class);

		bind(int.class).annotatedWith(Names.named("HighwayLayer edge width")).toInstance(roadWidth);
	}
	

	@Override
	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(VGATestVisioInitializer.class);
	}
	
}
