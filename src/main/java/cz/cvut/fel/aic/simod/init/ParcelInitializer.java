package cz.cvut.fel.aic.simod.init;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationUtils;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandIdGenerator;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.ParcelAgent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class ParcelInitializer {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ParcelInitializer.class);

    private static final long TRIP_MULTIPLICATION_TIME_SHIFT = 240_000;

    private static final long MAX_EVENTS = 0;

    private static final int RANDOM_SEED = 1;

    private final EventProcessor eventProcessor;

    private final ParcelEventHandler parcelEventHandler;

    private final StationsDispatcher onDemandVehicleStationsCentral;

    private final cz.cvut.fel.aic.simod.config.SimodConfig SimodConfig;

    private final AgentpolisConfig agentpolisConfig;

    private final SimulationUtils simulationUtils;

    private long eventCount;

    private long impossibleTripsCount;


    @Inject
    public ParcelInitializer(EventProcessor eventProcessor,
                             StationsDispatcher onDemandVehicleStationsCentral, SimodConfig config,
                             ParcelEventHandler parcelEventHandler, AgentpolisConfig agentpolisConfig,
                             SimulationUtils simulationUtils) {
        this.eventProcessor = eventProcessor;
        this.parcelEventHandler = parcelEventHandler;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.SimodConfig = config;
        this.agentpolisConfig = agentpolisConfig;
        this.simulationUtils = simulationUtils;
        eventCount = 0;
        impossibleTripsCount = 0;
    }


    public void initialize(List<TimeTrip<SimulationNode>> trips, List<TimeTrip<OnDemandVehicleStation>> rebalancingTrips){
        Random random = new Random(RANDOM_SEED);

        for (TimeTrip<SimulationNode> trip : trips) {
            long startTime = trip.getStartTime() - SimodConfig.startTime;
            // trip have to start at least 1ms after start of the simulation and no later then last
            if(startTime < 1 || startTime > simulationUtils.computeSimulationDuration()){
                impossibleTripsCount++;
//				LOGGER.info("Trip out of simulation time. Total: {}", impossibleTripsCount);
                continue;
            }

            for(int i = 0; i < SimodConfig.tripsMultiplier; i++){
                if(i + 1 >= SimodConfig.tripsMultiplier){
                    double randomNum = random.nextDouble();
                    if(randomNum > SimodConfig.tripsMultiplier - i){
                        break;
                    }
                }

                startTime = startTime + i * TRIP_MULTIPLICATION_TIME_SHIFT;
                eventProcessor.addEvent(null, parcelEventHandler, null, trip, startTime);
                eventCount++;
                if(MAX_EVENTS != 0 && eventCount >= MAX_EVENTS){
                    return;
                }
            }
        }
        if(rebalancingTrips != null){
            for (TimeTrip<OnDemandVehicleStation> rebalancingTrip : rebalancingTrips) {
                long startTime = rebalancingTrip.getStartTime() - SimodConfig.startTime;
                if(startTime < 1 || startTime > simulationUtils.computeSimulationDuration()){
                    impossibleTripsCount++;
                    continue;
                }
                eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.REBALANCING, onDemandVehicleStationsCentral,
                        null, rebalancingTrip, startTime);
            }
        }

        LOGGER.info("{} trips discarded because they are not within simulation time bounds", impossibleTripsCount);
    }




    public static class ParcelEventHandler extends EventHandlerAdapter {

        private final DemandIdGenerator demandIdGenerator;

        private final ParcelAgent.ParcelAgentFactory parcelAgentFactory;




        @Inject
        public ParcelEventHandler(DemandIdGenerator demandIdGenerator, ParcelAgent.ParcelAgentFactory parcelAgentFactory,
                                  SimulationCreator simulationCreator) {
            this.demandIdGenerator = demandIdGenerator;
            this.parcelAgentFactory = parcelAgentFactory;
        }





        @Override
        public void handleEvent(Event event) {
            TimeTrip<SimulationNode> trip = (TimeTrip<SimulationNode>) event.getContent();

            int id = demandIdGenerator.getId();

            ParcelAgent parcelAgent = parcelAgentFactory.create("Demand " + Integer.toString(id), id, trip);

            parcelAgent.born();
        }
    }
}
