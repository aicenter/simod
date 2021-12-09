package cz.cvut.fel.aic.simod.visual.ridesharing.peoplefreightheuristic;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightVehicle;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PhysicalPFVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalPFVehicleStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import org.junit.Test;

import java.util.Map;

public class SimpleTest
{
    private static final int LENGTH = 4;

    @Test
    public void run() throws Throwable
    {
        PhysicalTransportVehicleStorage vehicleStorage = new PhysicalTransportVehicleStorage();


//        TravelTimeProvider travelTimeProvider = null;
//        TimeProvider timeProvider = null;
//        PlanCostProvider travelCostProvider = null;
//        DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory = null;
        TypedSimulation eventProcessor = new TypedSimulation(120);
        SimodConfig config = new SimodConfig();
        IdGenerator idGenerator = new IdGenerator();

        TimeProvider timeProvider1 = new TimeProvider() {
            @Override
            public long getCurrentSimTime() {
                return 0;
            }
        };
        PositionUtil positionUtil = new PositionUtil();
        TripsUtil tripsUtil = null;

        int citySRID = 32618;
        Transformer transformer = new Transformer(citySRID);
        Graph<SimulationNode, SimulationEdge> graph = Utils.getGridGraph(4, transformer, 3);
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> map = null;

        AstarTravelTimeProvider astarTravelTimeProvider = new AstarTravelTimeProvider(timeProvider1, null, graph, moveUtil);

        AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
        MoveUtil moveUtil = new MoveUtil(agentpolisConfig);
        DemandAgent demandAgent = new DemandAgent(
                null,
                eventProcessor,
                null,
                astarTravelTimeProvider,
                tripsUtil,
                "agent007",
                7,

        );

        OnDemandvehicleStationStorage onDemandvehicleStationStorage = new OnDemandvehicleStationStorage(transformer);
        DroppedDemandsAnalyzer droppedDemandsAnalyzer = null; // new DroppedDemandsAnalyzer( vehicleStorage, positionUtil, astarTravelTimeProvider, config, onDemandvehicleStationStorage, agentpolisConfig);

        // start position of 1st vehicle
        SimulationNode startPos = graph.getNode(0);

        // adding vehicle
        vehicleStorage.addEntity(new PhysicalPFVehicle(
                "1 - vehicle",
                DemandSimulationEntityType.VEHICLE,
                LENGTH,
                10,
                EGraphType.HIGHWAY,
                startPos,
                agentpolisConfig.maxVehicleSpeedInMeters,
                1
        ));



        PeopleFreightHeuristicSolver solver = new PeopleFreightHeuristicSolver(
                                                    astarTravelTimeProvider,
                                                    null,
                                                    vehicleStorage,
                                                    positionUtil,
                                                    config,
                                                    timeProvider1,
                                                    null,
                                                    eventProcessor,
                                                    droppedDemandsAnalyzer,
                                                    onDemandvehicleStationStorage,
                                                    agentpolisConfig
                );

        // simple testing instance
        // create requests
        SimulationNode origin_1 = graph.getNode(3);
        SimulationNode destination_1 = graph.getNode(11);
        DefaultPlanComputationRequest request_1 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 0, null, origin_1, destination_1, )

        // call solve method
    }
}
