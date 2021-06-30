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
package cz.cvut.fel.aic.simod.traveltimecomputation.common;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 *
 * @author fido
 */
public class TestModule extends TestStandardAgentPolisModule{
	
	private final SimodConfig SimodConfig;

	public TestModule(SimodConfig SimodConfig) {
		super(SimodConfig, null, "agentpolis"); 
		this.SimodConfig = SimodConfig;
	}
	
	@Override
	protected void configureNext() {
		super.configureNext();
		bind(SimodConfig.class).toInstance(SimodConfig);
		bind(MapInitializer.class).to(TestGeojsonMapInitializer.class);
		bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
		bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
	}
      
	@Override
	protected void bindVisioInitializer() {
		
	}
	
}
