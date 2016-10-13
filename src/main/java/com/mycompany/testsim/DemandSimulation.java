/*
 */
package com.mycompany.testsim;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mycompany.testsim.init.EventInitializer;
import com.mycompany.testsim.io.RebalancingLoader;
import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.io.TripTransform;
import cz.agents.agentpolis.AgentPolisInitializer;
import cz.agents.agentpolis.simmodel.environment.model.delaymodel.impl.InfinityDelayingSegmentCapacityDeterminer;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class DemandSimulation {
	
	private static final String EXPERIMENT_PATH = "data/Prague";
	
	private static final String INPUT_FILE_PATH = "data/Prague/trips.json";
    
    private static final String REBALANCING_FILE_PATH = "data/Prague/policy.json";
	
	private static final int SRID = 2065;
    
    
    
	
	public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
		new DemandSimulation().run();
	}
    
    
    
	
	public void run() throws ConfigReaderException{
		try {
			
			File experimentDir = new File(EXPERIMENT_PATH);

			ConfigReader scenario = ConfigReader.initConfigReader(new File(experimentDir, "scenario.groovy").toURI().toURL());
			MyParams parameters = new MyParams(experimentDir, scenario);
            ZonedDateTime initDate = ZonedDateTime.now();
			SimpleEnvinromentFactory envinromentFactory = new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer());

			
			Injector injector 
                    = new AgentPolisInitializer(envinromentFactory, parameters, initDate, new MainModule()).initialize();
			
			SimulationCreator creator = injector.getInstance(SimulationCreator.class);

			creator.setMainEnvironment(injector);

			// set up visual appearance of agents
			creator.addEntityStyleVis(DemandSimulationEntityType.DEMAND, Color.GREEN, 8);
            
            // prepare map, entity storages...
            creator.prepareSimulation(new MyMapInitFactory(SRID));
            
            List<Trip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(INPUT_FILE_PATH), Long.class);
            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
            rebalancingLoader.load(new File(REBALANCING_FILE_PATH));
            
//            injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

            injector.getInstance(EventInitializer.class).initialize(osmNodesList, 
                    rebalancingLoader.getRebalancingTrips());
            
			// start it up
			creator.startSimulation();

			System.exit(0);
			
		} catch (IOException ex) {
			Logger.getLogger(PrecomputedDemandSimulation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
