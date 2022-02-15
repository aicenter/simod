package cz.cvut.fel.aic.simod.visual.ridesharing.greedyTASeT;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
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
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.greedyTASeT.GreedyTASeTSolver;
import cz.cvut.fel.aic.simod.visual.ridesharing.vga.mock.TestPlanRequest;
import ninja.fido.config.Configuration;
import org.junit.Test;

import java.util.*;

public class GreedyTASeTSolverTest {

    @Test
    public void run() {

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
        SimulationNode startPos = graph.getNode(1);  // top left corner

        RideSharingOnDemandVehicle vehicle_1 = new RideSharingOnDemandVehicle(
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
                "1",
                startPos
        );
        onDemandVehicleStorage.addEntity(vehicle_1);

        SimulationNode transferPoint = graph.getNode(7);
        List<SimulationNode> transferPoints = new ArrayList<>();
        transferPoints.add(transferPoint);

        GreedyTASeTSolver solver = new GreedyTASeTSolver( onDemandVehicleStorage,
                astarTravelTimeProvider,
                travelCostProvider,
                null,
                eventProcessor,
                simodConfig,
                timeProvider1,
                positionUtil,
                droppedDemandsAnalyzer,
                onDemandvehicleStationStorage,
                agentpolisConfig
                );

        solver.setTransferPoints(transferPoints);

        // create requests
        SimulationNode origin_1 = graph.getNode(1);
        SimulationNode destination_1 = graph.getNode(3);
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

        SimulationNode origin_2 = graph.getNode(3);
        SimulationNode destination_2 = graph.getNode(6);
        long startTime2 = 1050;
        long endTime2 = 2500;
        SimulationNode[] locations2 = {origin_2, destination_2};
        DemandAgent demandAgent_1 = new DemandAgent(
                null,
                eventProcessor,
                null,
                standardTimeProvider,
                tripsUtil,
                "agent_01",
                1,
                new TimeTrip<SimulationNode>(
                        0,
                        startTime2,
                        endTime2,
                        locations2
                )
        );

        DefaultPlanComputationRequest request_1 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 0, simodConfig, origin_1, destination_1, demandAgent_0);
        DefaultPlanComputationRequest request_2 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 1, simodConfig, origin_2, destination_2, demandAgent_1);
        List<PlanComputationRequest> requestsPeople = new ArrayList<>();
        requestsPeople.add(request_2);
        requestsPeople.add(request_1);

        // call solve method
        Map<RideSharingOnDemandVehicle, cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan> solution = solver.solve(requestsPeople, null);
        System.out.println("Solution:");
        System.out.println(solution.values().toString());



//        OnDemandVehicleStorage vehicleStorage = new OnDemandVehicleStorage();
//        TravelTimeProvider travelTimeProvider = null;
//        TimeProvider timeProvider = null;
//        PlanCostProvider travelCostProvider = null;
//        DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory = null;
//        TypedSimulation eventProcessor = new TypedSimulation(120);
//        SimodConfig config = new SimodConfig();
//        TimeProvider timeProvider1 = new TimeProvider() {
//            @Override
//            public long getCurrentSimTime() {
//                return 0;
//            }
//        };
//        PositionUtil positionUtil = new PositionUtil();
//        TripsUtil tripsUtil = null;
//        // TripsUtil(ShortestPathPlanner pathPlanner, NearestElementUtils nearestElementUtils, HighwayNetwork network, IdGenerator tripIdGenerator)
//        ShortestPathPlanner pathPlanner = null;
//        NearestElementUtils nearestElementUtils = new NearestElementUtils(null, null);
//        HighwayNetwork highwayNetwork = new HighwayNetwork(null);
//
//
//        Map<GraphType, Graph<SimulationNode, SimulationEdge>> map = null;
//        int citySRID = 32618;
//        Transformer transformer = new Transformer(citySRID);
//        Graph<SimulationNode, SimulationEdge>  graph = Utils.getCompleteGraph(4, transformer);
//        AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
//        MoveUtil moveUtil = new MoveUtil(agentpolisConfig);
////        AStarShortestPathPlanner astarShortestPathPlanner =
//
//        AstarTravelTimeProvider astarTravelTimeProvider = new AstarTravelTimeProvider(timeProvider1, null, graph, moveUtil);
//        OnDemandvehicleStationStorage onDemandvehicleStationStorage = new OnDemandvehicleStationStorage(transformer);
//        DroppedDemandsAnalyzer droppedDemandsAnalyzer = new DroppedDemandsAnalyzer(vehicleStorage, positionUtil, astarTravelTimeProvider, config, onDemandvehicleStationStorage, agentpolisConfig);
//
//
//        GreedyTASeTSolver solver = new GreedyTASeTSolver(vehicleStorage, astarTravelTimeProvider, null, null,
//                eventProcessor, config, timeProvider1, positionUtil, null,
//                onDemandvehicleStationStorage, agentpolisConfig, transferPoints);
//
////        TODO idk
////        requestFactory =
//
//        SimulationNode origin = new SimulationNode(1, 2, 55, 45, 55, 45, 200, 0);
//        SimulationNode destination = new SimulationNode(2, 3, 56, 46, 56, 46, 210, 0);
//        // null pointer exception because trips util is null
//        TestPlanRequest r1 = new TestPlanRequest(2, config, origin, destination, 0, false, astarTravelTimeProvider);
//        SimulationNode origin2 = new SimulationNode(1, 2, 55, 46, 55, 46, 200, 0);
//        SimulationNode destination2 = new SimulationNode(2, 3, 56, 45, 56, 5, 210, 0);
//        TestPlanRequest r2 = new TestPlanRequest(2, config, origin2, destination2, 0, false, astarTravelTimeProvider);
//        List<PlanComputationRequest> req = new ArrayList<>();
//        PlanComputationRequest rp1 = (PlanComputationRequest)r1;
//        PlanComputationRequest rp2 = (PlanComputationRequest)r2;
//        req.add(rp1);
//        req.add(rp2);
//        List<PlanComputationRequest> rr = new ArrayList<>();
////
//
//
//        Map<RideSharingOnDemandVehicle, DriverPlan> retMap = solver.solve(req, rr);
//

    }

    @Test
    public void testSort() {
        int [] array = {10, 2, 5, 9, 3};
        List<Double> list = new ArrayList<>();
        Double one = 1d;
        Double two = 2d;
        Double three = 3d;
        Double four = 4d;
        Double five = 5d;
        list.add(one);
        list.add(two);
        list.add(three);
        list.add(four);
        list.add(five);

        System.out.println("sort");
        List<Double> listCopy = new ArrayList<>(list);
        list.sort(Comparator.comparing(x -> array[listCopy.indexOf(x)]));
        Collections.reverse(list);
        System.out.println(Arrays.toString(array));
        System.out.println(list);

    }
}
