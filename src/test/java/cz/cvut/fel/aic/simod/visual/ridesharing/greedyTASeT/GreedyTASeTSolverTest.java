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
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.WaitActivityFactory;
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
import cz.cvut.fel.aic.simod.WaitTransferActivityFactory;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.*;
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
        SimulationNode startPos = graph.getNode(6);  // top left corner

        WaitTransferActivityFactory waitTransferActivityFactory = new WaitTransferActivityFactory();
        WaitActivityFactory waitActivityFactory = new WaitActivityFactory();


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
                waitTransferActivityFactory,
                waitActivityFactory,
                "1",
                startPos

        );
        RideSharingOnDemandVehicle vehicle_2 = new RideSharingOnDemandVehicle(
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
                waitTransferActivityFactory,
                waitActivityFactory,
                "2",
                startPos
        );

        onDemandVehicleStorage.addEntity(vehicle_1);
        onDemandVehicleStorage.addEntity(vehicle_2);

        SimulationNode transferPoint = graph.getNode(6);
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
        SimulationNode origin_1 = graph.getNode(2);
        SimulationNode destination_1 = graph.getNode(8);
        long startTime = 0;
        long endTime = 100;
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

        SimulationNode origin_2 = graph.getNode(5);
        SimulationNode destination_2 = graph.getNode(10);
        long startTime2 = 0;
        long endTime2 = 100;
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

        SimulationNode origin_3 = graph.getNode(2);
        SimulationNode destination_3 = graph.getNode(10);
        long startTime3 = 0;
        long endTime3 = 100;
        SimulationNode[] locations3 = {origin_3, destination_3};
        DemandAgent demandAgent_2 = new DemandAgent(
                null,
                eventProcessor,
                null,
                standardTimeProvider,
                tripsUtil,
                "agent_02",
                1,
                new TimeTrip<SimulationNode>(
                        0,
                        startTime3,
                        endTime3,
                        locations3
                )
        );

        SimulationNode origin_4 = graph.getNode(5);
        SimulationNode destination_4 = graph.getNode(8);
        long startTime4 = 0;
        long endTime4 = 100;
        SimulationNode[] locations4 = {origin_4, destination_4};
        DemandAgent demandAgent_3 = new DemandAgent(
                null,
                eventProcessor,
                null,
                standardTimeProvider,
                tripsUtil,
                "agent_03",
                1,
                new TimeTrip<SimulationNode>(
                        0,
                        startTime4,
                        endTime4,
                        locations4
                )
        );




        DefaultPlanComputationRequest request_1 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 0, simodConfig, origin_1, destination_1, demandAgent_0);
        DefaultPlanComputationRequest request_2 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 1, simodConfig, origin_2, destination_2, demandAgent_1);
        DefaultPlanComputationRequest request_3 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 1, simodConfig, origin_3, destination_3, demandAgent_2);
        DefaultPlanComputationRequest request_4 = new DefaultPlanComputationRequest(astarTravelTimeProvider, 1, simodConfig, origin_4, destination_4, demandAgent_3);
        List<PlanComputationRequest> requestsPeople = new ArrayList<>();
        requestsPeople.add(request_2);
        requestsPeople.add(request_1);
        requestsPeople.add(request_3);
        requestsPeople.add(request_4);

        // call solve method
        Map<RideSharingOnDemandVehicle, DriverPlan> solution = solver.solve(requestsPeople, null);
        System.out.println("Solution:");
        System.out.println(solution.values().toString());



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

    @Test
    public void removeCurrentPositionActions() {

        List<PlanAction> listOfActionsWithPositions = new ArrayList<>();

        PlanActionCurrentPosition a1 = new PlanActionCurrentPosition(null);
        PlanActionPickup a2 = new PlanActionPickup(null, null, 0);
        PlanActionDropoffTransfer a3 = new PlanActionDropoffTransfer(null, null, 0);

        listOfActionsWithPositions.add(a1);
        listOfActionsWithPositions.add(a2);
        listOfActionsWithPositions.add(a3);

        List<PlanAction> copyOfList = new ArrayList<>();
        copyOfList.addAll(listOfActionsWithPositions);
        for (PlanAction action : listOfActionsWithPositions) {
            if (action instanceof PlanActionCurrentPosition) {
                copyOfList.remove(action);
            }
        }
        System.out.println(copyOfList.size());
    }
}
