/*
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import java.io.File;

import java.net.MalformedURLException;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class MapVisualizer {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapVisualizer.class);
    
    public static void main(String[] args) throws MalformedURLException {
        new MapVisualizer().run(args);
    }


    public void run(String[] args) {
        AmodsimConfig config = new AmodsimConfig();
        
        File localConfigFile = args.length > 0 ? new File(args[0]) : null;
        
        Injector injector = new AgentPolisInitializer(new MapVisualiserModule(config, localConfigFile)).initialize();

        SimulationCreator creator = injector.getInstance(SimulationCreator.class);

        // prepare map, entity storages...
        creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

//        List<TimeTrip<Long>> osmNodesList;
//        try {
//            osmNodesList = TripTransform.jsonToTrips(new File(config.amodsim.preprocessedTrips), Long.class);
//            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
//            rebalancingLoader.load(new File(config.rebalancing.policyFilePath));
//
//            //  injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());
//
//            injector.getInstance(EventInitializer.class).initialize(osmNodesList,
//                    rebalancingLoader.getRebalancingTrips(), config);
//
//            injector.getInstance(StatisticInitializer.class).initialize();

            // start it up
            creator.startSimulation();
        
//        } catch (IOException ex) {
//            LOGGER.error(null, ex);
//        }
       

    }
}
