package cz.cvut.fel.aic.simod.visual.ridesharing.greedyTASeT;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.greedyTASeT.GreedyTASeTSolver;
import cz.cvut.fel.aic.simod.visual.ridesharing.vga.mock.TestPlanRequest;
import org.junit.Test;

import java.util.*;

public class GreedyTASeTSolverTest {

    @Test
    public void run() {
        OnDemandVehicleStorage vehicleStorage = new OnDemandVehicleStorage();
        TravelTimeProvider travelTimeProvider = null;
        TimeProvider timeProvider = null;
        PlanCostProvider travelCostProvider = null;
        DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory = null;
        TypedSimulation eventProcessor = new TypedSimulation(120);
        SimodConfig config = new SimodConfig();
        TimeProvider timeProvider1 = new TimeProvider() {
            @Override
            public long getCurrentSimTime() {
                return 0;
            }
        };
        PositionUtil positionUtil = new PositionUtil();
        TripsUtil tripsUtil = null;
        // TripsUtil(ShortestPathPlanner pathPlanner, NearestElementUtils nearestElementUtils, HighwayNetwork network, IdGenerator tripIdGenerator)
        ShortestPathPlanner pathPlanner = null;
        NearestElementUtils nearestElementUtils = new NearestElementUtils(null, null);
        HighwayNetwork highwayNetwork = new HighwayNetwork(null);


        Map<GraphType, Graph<SimulationNode, SimulationEdge>> map = null;
        int citySRID = 32618;
        Transformer transformer = new Transformer(citySRID);
        Graph<SimulationNode, SimulationEdge>  graph = Utils.getCompleteGraph(4, transformer);
        AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
        MoveUtil moveUtil = new MoveUtil(agentpolisConfig);
//        AStarShortestPathPlanner astarShortestPathPlanner =

        AstarTravelTimeProvider astarTravelTimeProvider = new AstarTravelTimeProvider(timeProvider1, null, graph, moveUtil);
        OnDemandvehicleStationStorage onDemandvehicleStationStorage = new OnDemandvehicleStationStorage(transformer);
        DroppedDemandsAnalyzer droppedDemandsAnalyzer = new DroppedDemandsAnalyzer(vehicleStorage, positionUtil, astarTravelTimeProvider, config, onDemandvehicleStationStorage, agentpolisConfig);


        GreedyTASeTSolver solver = new GreedyTASeTSolver(vehicleStorage, astarTravelTimeProvider, null, null,
                eventProcessor, config, timeProvider1, positionUtil, null,
                onDemandvehicleStationStorage, agentpolisConfig, transferPoints);

//        TODO idk
//        requestFactory =

        SimulationNode origin = new SimulationNode(1, 2, 55, 45, 55, 45, 200, 0);
        SimulationNode destination = new SimulationNode(2, 3, 56, 46, 56, 46, 210, 0);
        // null pointer exception because trips util is null
        TestPlanRequest r1 = new TestPlanRequest(2, config, origin, destination, 0, false, astarTravelTimeProvider);
        SimulationNode origin2 = new SimulationNode(1, 2, 55, 46, 55, 46, 200, 0);
        SimulationNode destination2 = new SimulationNode(2, 3, 56, 45, 56, 5, 210, 0);
        TestPlanRequest r2 = new TestPlanRequest(2, config, origin2, destination2, 0, false, astarTravelTimeProvider);
        List<PlanComputationRequest> req = new ArrayList<>();
        PlanComputationRequest rp1 = (PlanComputationRequest)r1;
        PlanComputationRequest rp2 = (PlanComputationRequest)r2;
        req.add(rp1);
        req.add(rp2);
        List<PlanComputationRequest> rr = new ArrayList<>();
//
        Map<RideSharingOnDemandVehicle, DriverPlan> retMap = solver.solve(req, rr);
        

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
