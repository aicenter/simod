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
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import java.io.File;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 *
 * @author travnja5
 */
public class TestModuleAstar extends TestStandardAgentPolisModule{

    public TestModuleAstar(SimodConfig SimodConfig, File localConfigFile) {
        super(SimodConfig, localConfigFile, "agentpolis");
    }

    @Override
    protected void configureNext() {
            super.configureNext(); 
            bind(MapInitializer.class).to(TestGeojsonMapInitializerAstar.class);
            bind(TravelTimeProvider.class).to(AstarTravelTimeProvider.class);
            bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
            bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
            bind(PlanCostProvider.class).to(StandardPlanCostProvider.class);
            bind(TimeProvider.class).to(StandardTimeProvider.class);
            bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
            
            
    }
    
}
