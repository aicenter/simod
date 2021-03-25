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
package cz.cvut.fel.aic.simod.ridesharing.traveltimecomputation.common;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.utils.ResourceReader;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 * A test module that is used for the tests that use the shortestPaths library. This ensures that the graph used
 * for the precomputed structures will be loaded and used for the tests.
 *
 * @author Michal Cvach
 */
public class ShortestPathsTestModule extends TestStandardAgentPolisModule {
    private final SimodConfig SimodConfig;

    public ShortestPathsTestModule(SimodConfig SimodConfig) {
        super(SimodConfig, null, "agentpolis");
        this.SimodConfig = SimodConfig;
		
		String package_path = "/cz/cvut/fel/aic/amodsim/ridesharing/traveltimecomputation/";

		String CHPath = ResourceReader.getAbsoultePathToResource(package_path + "ch.ch");
		SimodConfig.shortestpaths.chFilePath = CHPath;
		
		String TNRPath = ResourceReader.getAbsoultePathToResource(package_path + "tnr.tnrg");
		SimodConfig.shortestpaths.tnrFilePath = TNRPath;
		
		String TNRAFPath = ResourceReader.getAbsoultePathToResource(package_path + "tnraf.tgaf");
		SimodConfig.shortestpaths.tnrafFilePath = TNRAFPath;
		
		String mappingPath = ResourceReader.getAbsoultePathToResource(package_path + "mapping.xeni");
		SimodConfig.shortestpaths.mappingFilePath = mappingPath;
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
