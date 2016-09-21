/*
 */
package com.mycompany.testsim;

import com.mycompany.testsim.io.Trip;
import com.mycompany.testsim.io.TripTransform;
import cz.agents.agentpolis.simmodel.environment.model.delaymodel.impl.InfinityDelayingSegmentCapacityDeterminer;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.importer.init.VehicleDataModelFactory;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F-I-D-O
 */
public class PrecomputedDemandSimulation {
	
	private static final String EXPERIMENT_PATH = "C:\\AIC data\\prague\\";
	
	private static final String INPUT_FILE_PATH = "C:\\AIC data\\Prague\\trips.json";
	
	private static final int SRID = 2065;
	
	private static final int START_TIME = 25200000; // 7h
	
	public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
		try {
			List<Trip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(INPUT_FILE_PATH), Long.class);
			
			File experimentDir = new File(EXPERIMENT_PATH);

			ConfigReader scenario = ConfigReader.initConfigReader(new File(experimentDir, "scenario.groovy").toURI().toURL());
			MyParams parameters = new MyParams(experimentDir, scenario);

			SimulationCreator creator = new SimulationCreator(
					new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer()), parameters);

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
