/*
 */
package com.mycompany.testsim;

import cz.agents.agentpolis.simmodel.environment.model.delaymodel.impl.InfinityDelayingSegmentCapacityDeterminer;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.importer.init.VehicleDataModelFactory;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;

/**
 *
 * @author F-I-D-O
 */
public class MapLoader {
	
	private static final String EXPERIMENT_DIR = "C:\\AIC data\\cr";
	
	public static void main(String[] args) throws MalformedURLException, ConfigReaderException {
//		File experimentDir = new File(EXPERIMENT_DIR);
//        ConfigReader scenario = ConfigReader.initConfigReader(new File(experimentDir, "scenario.groovy").toURI().toURL());
//        MyParams parameters = new MyParams(experimentDir, scenario);
//
//        File resultFolder = parameters.dirForResults;
//
//        SimulationCreator creator = new SimulationCreator(
//                new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer()),
//                parameters);
//		
//		creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));
//
//        creator.addAgentInit(new MyAgentInitFactory());
//
//        // set up visual appearance of agents
//        creator.addEntityStyleVis(DemandSimulationEntityType.TEST_TYPE, Color.GREEN, 8);
//
//
//        // start it up
//        creator.prepareSimulation(new MyMapInitFactory(2065));
//        
//        creator.startSimulation();
//
//        System.exit(0);
	}
}
