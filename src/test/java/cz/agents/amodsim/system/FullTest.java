/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import org.slf4j.LoggerFactory;
/**
 *
 * @author fido
 */
public class FullTest {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FullTest.class);
    
    public static void runFullTest(int duration, int startTime, long timeForFinishingEvents){
      /*  AmodsimConfig config = Configuration.load(new AmodsimConfig());
        
        //config overwrite
        config.simulationDurationInMillis = duration;
        config.startTime = startTime;
//        config.showVisio = true;
        Common.setTestResultsDir(config, "test");
        
        // Guice configuration
        AgentPolisInitializer agentPolisInitializer = new AgentPolisInitializer(new MainModule(config));
        agentPolisInitializer.overrideModule(new TestModule());
        Injector injector = agentPolisInitializer.initialize();

        SimulationCreator creator = injector.getInstance(SimulationCreator.class);

        // prepare map, entity storages...
        creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

        List<TimeTrip<Long>> osmNodesList;
        try {
            osmNodesList = TripTransform.jsonToTrips(new File(config.preprocessedTrips), Long.class);
            RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
            rebalancingLoader.load(new File(config.rebalancing.policyFilePath));

            //  injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

            injector.getInstance(EventInitializer.class).initialize(osmNodesList,
                    rebalancingLoader.getRebalancingTrips(), config);

            injector.getInstance(StatisticInitializer.class).initialize();

            // start it up
            creator.startSimulation();

            injector.getInstance(StatisticControl.class).simulationFinished();


        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }*/
    }
}
