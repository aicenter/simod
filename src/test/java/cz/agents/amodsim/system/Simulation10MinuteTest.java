/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.Injector;
import com.google.inject.util.Modules;
import cz.agents.agentpolis.AgentPolisInitializer;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import cz.agents.amodsim.AmodsimAgentPolisConfiguration;
import cz.agents.amodsim.Configuration;
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
import org.junit.Test;

/**
 *
 * @author fido
 */
public class Simulation10MinuteTest {
    
    private static final int TEN_MINUTES_IN_MILIS = 600000;
    
    // we expect trips to be no longer then 40 minutes
    private static final int TRIP_MAX_DURATION = 2400000;
    
    private static final int START_TIME_MILIS = 25200000;
    
    @Test
    public void run() throws ConfigReaderException {
        Config config = new Configuration().load();
        
        //config overwrite
        config.agentpolis.simulationDurationInMillis = TEN_MINUTES_IN_MILIS;
        config.agentpolis.startTime = START_TIME_MILIS;
//        config.agentpolis.showVisio = true;
        Common.setTestResultsDir(config, "test");
        
        // Guice configuration
        AmodsimAgentPolisConfiguration configuration = new AmodsimAgentPolisConfiguration(config, 
                config.agentpolis.simulationDurationInMillis + TRIP_MAX_DURATION);
        AgentPolisInitializer agentPolisInitializer = new AgentPolisInitializer(configuration, new MainModule(config));
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
