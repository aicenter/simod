/*
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.init.StatisticInitializer;
import cz.cvut.fel.aic.amodsim.io.RebalancingLoader;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class OnDemandVehiclesSimulationTaxify {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehiclesSimulationTaxify.class);
    
    public static void main(String[] args) throws MalformedURLException {
        new OnDemandVehiclesSimulationTaxify().run(args);
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
 
        injector.getInstance(DARPSolver.class).solve();
        injector.getInstance(StatisticInitializer.class).initialize();
 

    }
}
