/*
 */
package cz.agents.amodsim;

import com.google.inject.Injector;
import cz.agents.amodsim.init.EventInitializer;
import cz.agents.amodsim.init.StatisticInitializer;
import cz.agents.amodsim.io.RebalancingLoader;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.io.TripTransform;
import cz.agents.agentpolis.AgentPolisInitializer;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import cz.agents.amodsim.config.Config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author David Fiedler
 */
public class OnDemandVehiclesSimulation {

    public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
        new OnDemandVehiclesSimulation().run();
    }


    public void run() throws ConfigReaderException {
        Config config = new Configuration().load();
        AmodsimAgentPolisConfiguration configuration = new AmodsimAgentPolisConfiguration(config);
        
        Injector injector = new AgentPolisInitializer(configuration, new MainModule(config)).initialize();

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
       
        System.exit(0);

    }
}
