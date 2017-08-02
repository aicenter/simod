/*
 */
package cz.agents.amodsim;

import ninja.fido.config.Configuration;
import com.google.inject.Injector;
import cz.agents.amodsim.init.EventInitializer;
import cz.agents.amodsim.init.StatisticInitializer;
import cz.agents.amodsim.io.RebalancingLoader;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.io.TripTransform;
import cz.cvut.fel.aic.agentpolis.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
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

    public static void main(String[] args) throws MalformedURLException{
        new OnDemandVehiclesSimulation().run();
    }


    public void run() {
        Config config = Configuration.load(new Config());
        
        Injector injector = new AgentPolisInitializer(new MainModule(config)).initialize();

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
