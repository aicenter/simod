/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.visual.ridesharing.insertionheuristic.common;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.EventOrderStorage;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.rebalancing.RebalancingOnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.VGATestVisioInitializer;
import java.io.File;

/**
 *
 * @author fido
 */
public class TestModule extends StandardAgentPolisModule{
	
	private final AmodsimConfig amodsimConfig;

	public TestModule(AmodsimConfig amodsimConfig, File localConfigFile) {
		super(amodsimConfig, localConfigFile, "agentpolis"); 
		this.amodsimConfig = amodsimConfig;
		agentpolisConfig.visio.showVisio = VisualTests.SHOW_VISIO;
                
		amodsimConfig.startTime = 0;
		amodsimConfig.tripsMultiplier = 1.0;  //1.0
                amodsimConfig.ridesharing.maxProlongationInSeconds = 300;                
                
		agentpolisConfig.simulationDuration.days = 0;
		agentpolisConfig.simulationDuration.hours = 0;
		agentpolisConfig.simulationDuration.minutes = 0;
		agentpolisConfig.simulationDuration.seconds = 120;
                
	}

	@Override
	protected void configureNext() {
		super.configureNext();
		bind(AmodsimConfig.class).toInstance(amodsimConfig);
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
