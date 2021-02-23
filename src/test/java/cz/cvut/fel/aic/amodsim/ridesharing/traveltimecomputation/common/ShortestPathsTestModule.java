package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.utils.ResourceReader;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 * A test module that is used for the tests that use the shortestPaths library. This ensures that the graph used
 * for the precomputed structures will be loaded and used for the tests.
 *
 * @author Michal Cvach
 */
public class ShortestPathsTestModule extends TestStandardAgentPolisModule {
    private final AmodsimConfig amodsimConfig;

    public ShortestPathsTestModule(AmodsimConfig amodsimConfig) {
        super(amodsimConfig, null, "agentpolis");
        this.amodsimConfig = amodsimConfig;
		
		String package_path = "/cz/cvut/fel/aic/amodsim/ridesharing/traveltimecomputation/";

		String CHPath = ResourceReader.getAbsoultePathToResource(package_path + "ch.ch");
		amodsimConfig.shortestpaths.chFilePath = CHPath;
		
		String TNRPath = ResourceReader.getAbsoultePathToResource(package_path + "tnr.tnrg");
		amodsimConfig.shortestpaths.tnrFilePath = TNRPath;
		
		String TNRAFPath = ResourceReader.getAbsoultePathToResource(package_path + "tnraf.tgaf");
		amodsimConfig.shortestpaths.tnrafFilePath = TNRAFPath;
		
		String mappingPath = ResourceReader.getAbsoultePathToResource(package_path + "mapping.xeni");
		amodsimConfig.shortestpaths.mappingFilePath = mappingPath;
    }

    @Override
    protected void configureNext() {
        super.configureNext();
        bind(AmodsimConfig.class).toInstance(amodsimConfig);
        bind(MapInitializer.class).to(TestGeojsonMapInitializer.class);
        bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
        bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
    }

    @Override
    protected void bindVisioInitializer() {

    }

}
