/*
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.init.EventInitializer;
import cz.cvut.fel.aic.amodsim.init.StatisticInitializer;
import cz.cvut.fel.aic.amodsim.io.RebalancingLoader;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;
import ninja.fido.config.Configuration;

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

    public static void main(String[] args) throws MalformedURLException {
        new OnDemandVehiclesSimulation().run(args);
    }


    public void run(String[] args) {
        AmodsimConfig config = new AmodsimConfig();
        
        File localConfigFile = null;
        if(args.length > 0){
            localConfigFile = new File(args[0]);
        }

        Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();

        SimulationCreator creator = injector.getInstance(SimulationCreator.class);

        // prepare map, entity storages...
        creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

//        List<TimeTrip<Long>> osmNodesList;
        try {
//            osmNodesList = TripTransform.jsonToTrips(new File(config.amodsim.preprocessedTrips), Long.class);
            TripTransform tripTransform = injector.getInstance(TripTransform.class);
            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
            rebalancingLoader.load(new File(config.rebalancing.policyFilePath));

            //  injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

            injector.getInstance(EventInitializer.class).initialize(
                    tripTransform.loadTripsFromTxt(new File(config.amodsim.tripsPath)),
                    rebalancingLoader.getRebalancingTrips(), config);

            injector.getInstance(StatisticInitializer.class).initialize();

            // start it up
            creator.startSimulation();

            if (config.amodsim.useTripCache) {
                injector.getInstance(TripsUtilCached.class).saveNewTrips();
            }
            injector.getInstance(Statistics.class).simulationFinished();

        } catch (IOException ex) {
            Logger.getLogger(OnDemandVehiclesSimulation.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
