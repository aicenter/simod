package cz.cvut.fel.aic.simod.visual.ridesharing.peoplefreightheuristic;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PlanComputationRequestPeople;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightHeuristicSolver;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightVehicle;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PhysicalPFVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import ninja.fido.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTest
{
    private static final int LENGTH = 4;

    public void run() throws Throwable
    {
//        TravelTimeProvider travelTimeProvider = null;
//        TimeProvider timeProvider = null;
//        PlanCostProvider travelCostProvider = null;
//        DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory = null;
        TypedSimulation eventProcessor = new TypedSimulation(120);
        SimodConfig simodConfig = new SimodConfig();
        IdGenerator tripIdGenerator = new IdGenerator();
        IdGenerator idGenerator2 = new IdGenerator();
        IdGenerator idGenerator3 = new IdGenerator();

        TimeProvider timeProvider1 = new TimeProvider() {
            @Override
            public long getCurrentSimTime() {
                return 0;
            }
        };
        PositionUtil positionUtil = new PositionUtil();

        // creating MAP of city: 4columns x 3rows  grid
        int citySRID = 32618;
        Transformer transformer = new Transformer(citySRID);
        Graph<SimulationNode, SimulationEdge> graph = Utils.getGridGraph(4, transformer, 3);
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> gridMap = new HashMap<>();
        gridMap.put(EGraphType.HIGHWAY, graph);

        // setting up Agentpolis config
        AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
        Configuration.load(agentpolisConfig, simodConfig, null, "agentpolis");

        // TripsUtil initialization
        MoveUtil moveUtil = new MoveUtil(agentpolisConfig);
        TransportNetworks transportNetworks = new TransportNetworks(gridMap);
        AStarShortestPathPlanner aStarPlanner = new AStarShortestPathPlanner(
                transportNetworks,
                new EuclideanTraveltimeHeuristic(positionUtil),
                agentpolisConfig,
                moveUtil
        );
        NearestElementUtils nearestElementUtils = new NearestElementUtils(transportNetworks, transformer);
        TripsUtil tripsUtil = new TripsUtil(
                aStarPlanner,
                nearestElementUtils,
                new HighwayNetwork(graph),
                tripIdGenerator
        );

        // Time providers
        AstarTravelTimeProvider astarTravelTimeProvider = new AstarTravelTimeProvider(timeProvider1, tripsUtil, graph, moveUtil);
        StandardTimeProvider standardTimeProvider = new StandardTimeProvider(eventProcessor);

        StandardPlanCostProvider travelCostProvider = new StandardPlanCostProvider(simodConfig);


        // Storages
        OnDemandVehicleStorage onDemandVehicleStorage = new OnDemandVehicleStorage();
        PhysicalTransportVehicleStorage physicalVehicleStorage = new PhysicalTransportVehicleStorage();
        OnDemandvehicleStationStorage onDemandvehicleStationStorage = new OnDemandvehicleStationStorage(transformer);

        DroppedDemandsAnalyzer droppedDemandsAnalyzer = null; // new DroppedDemandsAnalyzer( vehicleStorage, positionUtil, astarTravelTimeProvider, config, onDemandvehicleStationStorage, agentpolisConfig);

        // start position of 1st vehicle
        // create Vehicle_1
        SimulationNode startPos_1 = graph.getNode(0);  // [0, 0]
        int parcelCapacity_1 = 10;
        PeopleFreightVehicle vehicle_1 = new PeopleFreightVehicle(
                physicalVehicleStorage,
                tripsUtil,
                null,
                null,
                null,
                tripIdGenerator,
                eventProcessor,
                standardTimeProvider,
                idGenerator2,
                simodConfig,
                idGenerator3,
                agentpolisConfig,
                "PFVehicle_1",
                startPos_1,
                parcelCapacity_1,
                new PhysicalPFVehicle<>(
                        "PFVehicle_1",
                        DemandSimulationEntityType.VEHICLE,
                        LENGTH,
                        parcelCapacity_1,
                        EGraphType.HIGHWAY,
                        startPos_1,
                        agentpolisConfig.maxVehicleSpeedInMeters)
        );
        onDemandVehicleStorage.addEntity(vehicle_1);

        PeopleFreightHeuristicSolver solver = new PeopleFreightHeuristicSolver(
                astarTravelTimeProvider,
                travelCostProvider,
                onDemandVehicleStorage,
                positionUtil,
                simodConfig,
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
        long startTime = 0;
        long endTime = 1000;
        SimulationNode[] locations = {origin_1, destination_1};
        DemandAgent demandAgent_0 = new DemandAgent(
                null,
                eventProcessor,
                null,
                standardTimeProvider,
                tripsUtil,
                "agent_00",
                0,
                new TimeTrip<SimulationNode>(
                        0,
                        startTime,
                        endTime,
                        locations
                )
        );
        PlanComputationRequestPeople request_1 = new PlanComputationRequestPeople(astarTravelTimeProvider, 0, simodConfig, origin_1, destination_1, demandAgent_0);
        List<PlanComputationRequestPeople> requestsPeople = new ArrayList<>();
        requestsPeople.add(request_1);

        // call solve method
        Map<PeopleFreightVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solution = solver.solve(requestsPeople, null, null, null);
        System.out.println("Solution:");
        System.out.println(solution.values().toString());
    }
}
