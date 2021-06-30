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
package cz.cvut.fel.aic.simod.visual.ridesharing.insertionheuristic.common;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.rebalancing.RebalancingOnDemandVehicleStation;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.visual.ridesharing.vga.common.VGATestVisioInitializer;
import java.io.File;

/**
 *
 * @author fido
 */
public class TestModule extends StandardAgentPolisModule{
	
	private final SimodConfig SimodConfig;

	public TestModule(SimodConfig SimodConfig, File localConfigFile) {
		super(SimodConfig, localConfigFile, "agentpolis"); 
		this.SimodConfig = SimodConfig;
		agentpolisConfig.visio.showVisio = VisualTests.SHOW_VISIO;
                
		SimodConfig.startTime = 0;
		SimodConfig.tripsMultiplier = 1.0;  //1.0
                SimodConfig.ridesharing.maxProlongationInSeconds = 300;                
                
		agentpolisConfig.simulationDuration.days = 0;
		agentpolisConfig.simulationDuration.hours = 0;
		agentpolisConfig.simulationDuration.minutes = 0;
		agentpolisConfig.simulationDuration.seconds = 120;
                
	}

	@Override
	protected void configureNext() {
		super.configureNext();
		bind(SimodConfig.class).toInstance(SimodConfig);
                bind(AgentpolisConfig.class).toInstance(agentpolisConfig);
		install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
			.build(DemandAgent.DemandAgentFactory.class));
		bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);
		bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
		bind(StationsDispatcher.class).to(RidesharingDispatcher.class);
		bind(DARPSolver.class).to(InsertionHeuristicSolver.class);
//		bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
		bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
		bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
		install(new FactoryModuleBuilder().implement(DefaultPlanComputationRequest.class, DefaultPlanComputationRequest.class)
				.build(DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory.class));
                
                
                install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, RebalancingOnDemandVehicleStation.class)
				.build(RebalancingOnDemandVehicleStation.OnDemandVehicleStationFactory.class));
	}
	

	@Override
	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(VGATestVisioInitializer.class);
	}
	
}
