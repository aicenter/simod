/*
 */
package cz.agents.amodsim;


import com.google.inject.Guice;
import com.google.inject.Injector;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.io.TripTransform;
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
public class PrecomputedDemandSimulation {

    private static File EXPERIMENT_DIR = new File("data/Prague");

    private static final String INPUT_FILE_PATH = "trips.json";

	private static final int SRID = 2065;
	
	private static final int START_TIME = 25200000; // 7h
	
	public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
        if (args.length >= 1) {
            EXPERIMENT_DIR = new File(args[0]);
        }
        new PrecomputedDemandSimulation().run();
    }
	
	public void run() throws ConfigReaderException{
		try {
            List<TimeTrip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(EXPERIMENT_DIR, INPUT_FILE_PATH), Long.class);

            ConfigReader scenario = ConfigReader.initConfigReader(new File(EXPERIMENT_DIR, "scenario.groovy").toURI().toURL());
            MyParams parameters = new MyParams(EXPERIMENT_DIR, scenario);
//			SimpleEnvinromentFactory envinromentFactory = new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer());

			
//			Injector injector = Guice.createInjector(new StandardAgentPolisModule(envinromentFactory, parameters));
			

//			SimulationCreator creator = new SimulationCreator(
//					new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer()), parameters);
//			
//			SimulationCreator creator = injector.getInstance(SimulationCreator.class);
//			
//			creator.setMainEnvironment(injector);

	//		creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));

	//        creator.addAgentInit(new MyAgentInitFactory());
	
//			creator.addInitFactory(new DemendsInitFactory(osmNodesList, creator));

			// set up visual appearance of agents
//			creator.addEntityStyleVis(DemandSimulationEntityType.DEMAND, Color.GREEN, 8);
//
//			// start it up
//			creator.prepareSimulation(new MyMapInitFactory(SRID));
//            
//            creator.startSimulation();

			System.exit(0);
			
		} catch (IOException ex) {
			Logger.getLogger(PrecomputedDemandSimulation.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	
}