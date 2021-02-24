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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.OptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.mock.TestTimeProvider;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.File;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 *
 * @author fido
 */
public class TestModuleNoVisio extends TestModule{
	
	private final AmodsimConfig amodsimConfig;


	public TestModuleNoVisio(AmodsimConfig amodsimConfig, File localConfigFile) {
		super(amodsimConfig, localConfigFile); 
		this.amodsimConfig = amodsimConfig;
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("mapSrid")).to(agentpolisConfig.srid);

		bind(Transformer.class).toInstance(new Transformer(agentpolisConfig.srid));

		bind(AgentpolisConfig.class).toInstance(agentpolisConfig);
		
		bind(TimeProvider.class).to(TestTimeProvider.class);
		
		bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
		
		bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
		
		configureNext();
	}


	@Override
	protected void configureNext() {
		super.configureNext();
		bind(AmodsimConfig.class).toInstance(amodsimConfig);
		bind(MapInitializer.class).to(GeojsonMapInitializer.class);
		bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
		install(new FactoryModuleBuilder().implement(DefaultPlanComputationRequest.class, DefaultPlanComputationRequest.class)
				.build(DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory.class));
		bind(OptimalVehiclePlanFinder.class).to(ArrayOptimalVehiclePlanFinder.class);
	}
	

	@Override
	protected void bindVisioInitializer() {
//		bind(VisioInitializer.class).to(VGATestVisioInitializer.class);
	}
	
}
