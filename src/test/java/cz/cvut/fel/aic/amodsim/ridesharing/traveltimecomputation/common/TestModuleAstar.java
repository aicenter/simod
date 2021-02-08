/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.common;


import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import java.io.File;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 *
 * @author travnja5
 */
public class TestModuleAstar extends TestStandardAgentPolisModule{

    public TestModuleAstar(AmodsimConfig amodsimConfig, File localConfigFile) {
        super(amodsimConfig, localConfigFile, "agentpolis");
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
