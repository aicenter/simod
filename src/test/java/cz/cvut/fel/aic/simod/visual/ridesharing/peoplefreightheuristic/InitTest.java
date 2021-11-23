package cz.cvut.fel.aic.simod.visual.ridesharing.peoplefreightheuristic;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightHeuristicSolver;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import org.junit.Test;

import java.util.Map;

public class InitTest
{
    @Test
    public void run() throws Throwable
    {
        OnDemandVehicleStorage vehicleStorage = new OnDemandVehicleStorage();
//        TravelTimeProvider travelTimeProvider = null;
//        TimeProvider timeProvider = null;
//        PlanCostProvider travelCostProvider = null;
//        DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory = null;
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
        Map<GraphType, Graph<SimulationNode, SimulationEdge>> map = null;
        int citySRID = 32618;
        Transformer transformer = new Transformer(citySRID);
        Graph<SimulationNode, SimulationEdge> graph = Utils.getCompleteGraph(4, transformer);
        AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
        MoveUtil moveUtil = new MoveUtil(agentpolisConfig);
        AstarTravelTimeProvider astarTravelTimeProvider = new AstarTravelTimeProvider(timeProvider1, null, graph, moveUtil);
        OnDemandvehicleStationStorage onDemandvehicleStationStorage = new OnDemandvehicleStationStorage(transformer);
        DroppedDemandsAnalyzer droppedDemandsAnalyzer = new DroppedDemandsAnalyzer(vehicleStorage, positionUtil, astarTravelTimeProvider, config, onDemandvehicleStationStorage, agentpolisConfig);





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
    }
}
