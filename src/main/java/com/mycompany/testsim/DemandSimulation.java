/*
 */
package com.mycompany.testsim;

import com.mycompany.testsim.initfactory.DemendsInitFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mycompany.testsim.io.RebalancingLoader;
import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.io.TripTransform;
import cz.agents.agentpolis.simmodel.environment.StandardAgentPolisModule;
import cz.agents.agentpolis.simmodel.environment.model.delaymodel.impl.InfinityDelayingSegmentCapacityDeterminer;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
			List<Trip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(INPUT_FILE_PATH), Long.class);
            RebalancingLoader rebalancingLoader = new RebalancingLoader();
            rebalancingLoader.load(new File(REBALANCING_FILE_PATH));
			
			File experimentDir = new File(EXPERIMENT_PATH);

			ConfigReader scenario = ConfigReader.initConfigReader(new File(experimentDir, "scenario.groovy").toURI().toURL());
			MyParams parameters = new MyParams(experimentDir, scenario);
			SimpleEnvinromentFactory envinromentFactory = new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer());

			
			Injector injector = Guice.createInjector(new MainModule(envinromentFactory, parameters));
			
            injector.getInstance(DemandEntityInitializer.class).initialize(osmNodesList, 
                    rebalancingLoader.getOnDemandVehicleStations(), rebalancingLoader.getRebalancingTrips());
			
			SimulationCreator creator = injector.getInstance(SimulationCreator.class);
			
			creator.setMainEnvironment(injector);

	//		creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));

	//        creator.addAgentInit(new MyAgentInitFactory());
	
			creator.addInitFactory(new DemendsInitFactory(osmNodesList, creator));

			// set up visual appearance of agents
			creator.addEntityStyleVis(DemandSimulationEntityType.DEMAND, Color.GREEN, 8);

			// start it up
			creator.startSimulation(new MyMapInitFactory(SRID));

			System.exit(0);
			
		} catch (IOException ex) {
			Logger.getLogger(PrecomputedDemandSimulation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
