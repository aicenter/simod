package cz.agents.amodsim.ridesharing.traveltimecomputation;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

public class ShortestPathsTestModule extends TestStandardAgentPolisModule {
    private final AmodsimConfig amodsimConfig;

    public ShortestPathsTestModule(AmodsimConfig amodsimConfig) {
        super(amodsimConfig, null, "agentpolis");
        this.amodsimConfig = amodsimConfig;
    }

    @Override
    protected void configureNext() {
        super.configureNext();
        bind(AmodsimConfig.class).toInstance(amodsimConfig);
        bind(MapInitializer.class).to(GeojsonMapInitializer.class);
        bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
        bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
    }

    @Override
    protected void bindVisioInitializer() {

    }

}
