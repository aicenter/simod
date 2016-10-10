package com.mycompany.testsim;

import cz.agents.agentpolis.simmodel.environment.model.delaymodel.impl.InfinityDelayingSegmentCapacityDeterminer;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.importer.init.VehicleDataModelFactory;
import cz.agents.agentpolis.simulator.creator.SimulationCreator;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String[] args) throws ConfigReaderException, IOException {

        File experimentDir;
        if (args.length >= 1) {
            experimentDir = new File(args[0]);
        } else {
            throw new RuntimeException("The first argument should be path to the directory containing scenario configuration.");
        }

        LOGGER.debug("Will run experiment in directory " + experimentDir.getAbsolutePath());
        ConfigReader scenario = ConfigReader.initConfigReader(new File(experimentDir, "config/scenario.groovy").toURI().toURL());
        MyParams parameters = new MyParams(experimentDir, scenario);

        File resultFolder = parameters.dirForResults;
		

//        TestbedLogAnalyser testbedLogAnalyser = new TestbedLogAnalyser(
//                new File(resultFolder, "result_" + parameters.darpResultFileName + ".txt"),
//                System.currentTimeMillis());

//        final LogHandler logHandler = new LogHandler();

        SimulationCreator creator = new SimulationCreator(
                new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer()),
                parameters);
		
		creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));

//        creator.addLogger(testbedLogAnalyser);
//        creator.addLogger(logHandler);

//        creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));

//        TestbedAnalyserProcessorInit processorInit = new TestbedAnalyserProcessorInit(testbedLogAnalyser);
//        creator.addInitFactory(processorInit);

//        Set<Class<? extends LogItem>> logItems = Sets.newHashSet();
//        logItems.add(PassengerGetInVehicleLogItem.class);
//        logItems.add(PassengerGetOffVehicleLogItem.class);

//        creator.addAllowEventForEventViewer(logItems);

//        // generate a random seed for the whole application
//        if (args.length > 1) {
//            GlobalParams.setUseResultsFile(false);
//            GlobalParams.setRandomSeed(Long.parseLong(args[1]));
//            GlobalParams.setTimerDriverInterval(Integer.parseInt(args[2]));
//            GlobalParams.setTimerPassengerInterval(Integer.parseInt(args[3]));
//            if (args.length > 4) {
//                GlobalParams.setTimerDispatchingInterval(Integer.parseInt(args[4]));
//            }
//
//        } else {
            // default (in-source) settings of parameters
//            GlobalParams.setUseResultsFile(true);
//            GlobalParams.setRandomSeed(4);
//            GlobalParams.setTimerDispatchingInterval(10);
//            GlobalParams.setTimerDriverInterval(1); // 30
//            GlobalParams.setTimerPassengerInterval(1); // 35
//        }

        // following parameters are usually the same
//        GlobalParams.setVelocityInKmph(15);
//        GlobalParams.setDriverReturnsBack(false);
//        GlobalParams.setPricePerKilometer(1000);

//        LOGGER.info("Using seed: " + GlobalParams.getRandomSeed());

//        creator.addInitModuleFactory(new NearestNodeInitModuleFactory(parameters.epsg));
//        creator.addInitModuleFactory(new TestbedPlannerModuleFactory());

        // add agents into the environment
//        final CentralizedLogicConstructor logicConstructor = new CentralizedLogicConstructor();
        creator.addAgentInit(new MyAgentInitFactory());
        // creator.addAgentInit(new PassengerInitFactory());
//        creator.addAgentInit(new PassengerForBenchmarkInitFactory(parameters.passengerPopulationPath,
//                logicConstructor));

        // initialize the dispatching and timers
//        creator.addInitModuleFactory(new DispatchingAndTimersInitFactory(logicConstructor));

        // set up visual appearance of agents
        creator.addEntityStyleVis(DemandSimulationEntityType.TEST_TYPE, Color.GREEN, 8);


        // start it up
        creator.prepareSimulation(new MyMapInitFactory(2000));
        
        creator.startSimulation();

        // after finishing the simulation, report statistics
//        testbedLogAnalyser.processResult();

        LOGGER.info("FINISHED!");
        System.exit(0);
    }

    private static boolean uni(int r) {
        return r != -1;
    }
}
