/*
 */
package com.mycompany.testsim;

import com.google.inject.Injector;
import com.mycompany.testsim.init.EventInitializer;
import com.mycompany.testsim.init.StatisticInitializer;
import com.mycompany.testsim.io.RebalancingLoader;
import com.mycompany.testsim.io.TimeTrip;
import com.mycompany.testsim.io.TripTransform;
import cz.agents.agentpolis.AgentPolisInitializer;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
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
	
	private static final File EXPERIMENT_DIR = new File("data/Prague");
	
	private static final String INPUT_FILE_PATH = "data/Prague/trips.json";
    
    private static final String REBALANCING_FILE_PATH = "data/Prague/policy.json";
	
	private static final int SRID = 2065;


	public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
		new OnDemandVehiclesSimulation().run();
	}

	
	public void run() throws ConfigReaderException{
		try {
			ConfigReader scenario = ConfigReader.initConfigReader(new File(EXPERIMENT_DIR, "scenario.groovy").toURI().toURL());
			MyParams parameters = new MyParams(EXPERIMENT_DIR, scenario);
			
			Injector injector = new AgentPolisInitializer(parameters, new MainModule()).initialize();
			
			SimulationCreator creator = injector.getInstance(SimulationCreator.class);
            
            // prepare map, entity storages...
            creator.prepareSimulation(new MyMapInitFactory(SRID));
            
            List<TimeTrip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(INPUT_FILE_PATH), Long.class);
            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
            rebalancingLoader.load(new File(REBALANCING_FILE_PATH));
            
//            injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

            injector.getInstance(EventInitializer.class).initialize(osmNodesList, 
                    rebalancingLoader.getRebalancingTrips());
            
            injector.getInstance(StatisticInitializer.class).initialize();
            
			// start it up
			creator.startSimulation();

			System.exit(0);
			
		} catch (IOException ex) {
			Logger.getLogger(PrecomputedDemandSimulation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
