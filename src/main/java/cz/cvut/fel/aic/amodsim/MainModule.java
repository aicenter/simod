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
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent.DemandAgentFactory;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.*;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;
import cz.cvut.fel.aic.amodsim.visio.AmodsimVisioInItializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.VehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.rebalancing.RebalancingOnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.ridesharing.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.OptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.DistanceMatrixTravelTimeProvider;
import cz.cvut.fel.aic.geographtools.TransportMode;

import java.io.File;

import java.util.Set;

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
		bind(new TypeLiteral<Set<TransportMode>>(){}).toInstance(Sets.immutableEnumSet(TransportMode.CAR));
		bind(AmodsimConfig.class).toInstance(amodsimConfig);
		bind(EntityStorage.class).to(VehicleStorage.class);
		bind(MapInitializer.class).to(GeojsonMapInitializer.class);
		
		if(amodsimConfig.useTripCache){
			bind(TripsUtil.class).to(TripsUtilCached.class);
		}
//		bind(DemandLayer.class).to(DemandLayerWithJitter.class);
		
//		bind(PhysicalVehicleDriveFactory.class).to(CongestedDriveFactory.class);
		bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);

		if(amodsimConfig.ridesharing.on){
			bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
			bind(StationsDispatcher.class).to(RidesharingDispatcher.class);
//			bind(TravelTimeProvider.class).to(DistanceMatrixTravelTimeProvider.class);
			bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
//			bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
			bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
			install(new FactoryModuleBuilder().implement(DefaultPlanComputationRequest.class, DefaultPlanComputationRequest.class)
						.build(DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory.class));
			
			switch(amodsimConfig.ridesharing.method){
				case "insertion-heuristic":
					bind(DARPSolver.class).to(InsertionHeuristicSolver.class);
					break;
				case "vga":
					bind(DARPSolver.class).to(VehicleGroupAssignmentSolver.class);
					bind(OptimalVehiclePlanFinder.class).to(ArrayOptimalVehiclePlanFinder.class);
		//			bind(OptimalVehiclePlanFinder.class).to(PlanBuilderOptimalVehiclePlanFinder.class);
					break;
			}

		}
		else{
		   bind(OnDemandVehicleFactorySpec.class).to(OnDemandVehicleFactory.class);
		}
		install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
			.build(DemandAgentFactory.class));
		
		if(amodsimConfig.rebalancing.on){
			install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, RebalancingOnDemandVehicleStation.class)
				.build(RebalancingOnDemandVehicleStation.OnDemandVehicleStationFactory.class));
		}
	} 
	
}
