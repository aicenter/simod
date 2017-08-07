/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import ninja.fido.config.Configuration;
import cz.agents.amodsim.MainModule;
import cz.agents.amodsim.MapInitializer;
import cz.agents.amodsim.OnDemandVehiclesSimulation;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.init.EventInitializer;
import cz.agents.amodsim.init.StatisticInitializer;
import cz.agents.amodsim.io.RebalancingLoader;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.io.TripTransform;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
public class FullTest {
    public static void runFullTest(int duration, int startTime, long timeForFinishingEvents){
        Config config = Configuration.load(new Config());
        
        //config overwrite
        config.agentpolis.simulationDurationInMillis = duration;
        config.agentpolis.startTime = startTime;
//        config.agentpolis.showVisio = true;
        Common.setTestResultsDir(config, "test");
        
        // Guice configuration
        AgentPolisInitializer agentPolisInitializer = new AgentPolisInitializer(new MainModule(config));
        agentPolisInitializer.overrideModule(new TestModule());
        Injector injector = agentPolisInitializer.initialize();

        SimulationCreator creator = injector.getInstance(SimulationCreator.class);

        // prepare map, entity storages...
        creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

        List<TimeTrip<Long>> osmNodesList;
        try {
            osmNodesList = TripTransform.jsonToTrips(new File(config.agentpolis.preprocessedTrips), Long.class);
            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
            rebalancingLoader.load(new File(config.rebalancing.policyFilePath));

            //  injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

            injector.getInstance(EventInitializer.class).initialize(osmNodesList,
                    rebalancingLoader.getRebalancingTrips(), config);

            injector.getInstance(StatisticInitializer.class).initialize();

            // start it up
            creator.startSimulation();
        
        } catch (IOException ex) {
            Logger.getLogger(OnDemandVehiclesSimulation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
