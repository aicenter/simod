package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.MainModule;
import cz.cvut.fel.aic.amodsim.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public class TestCH {
	public static void main(String[] args) throws IOException, TripPlannerException {
		AmodsimConfig config = new AmodsimConfig();
        
        File localConfigFile = args.length > 0 ? new File(args[1]) : null;
        
        Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();

        
        // prepare map, entity storages...
        MapData mapData = injector.getInstance(MapInitializer.class).getMap();
		
		BinaryFormat bf = new BinaryFormat();
		Map<Long,CHNode> readback = bf.read("contracted-nodes.dat", "contracted-ways.dat", new StdoutStatusMonitor());
		
		CHGraph cHGraph = new CHGraph(readback);
		
		initAPGraphs(injector);
		
		TripsUtil tripsUtil = injector.getInstance(TripsUtil.class);
		
		Graph<SimulationNode, SimulationEdge> graph = injector.getInstance(HighwayNetwork.class).getNetwork();
		
		int fromId = 0;
		int toId = 2;
		
		Trip<SimulationNode> trip = tripsUtil.createTrip(fromId, toId);
		
				
		SimulationNode fromNode = graph.getNode(fromId);
		SimulationNode toNode = graph.getNode(toId);
		Trip<SimulationNode> chTrip = cHGraph.query(fromNode, toNode);
		
		System.out.println("Trip equals: " + Arrays.equals(trip.getLoacationIds(), chTrip.getLoacationIds()));
		System.out.println("Astar trip: ");
		System.out.println(trip.locationIdsToString());
		
		System.out.println("CH trip: ");
		System.out.println(chTrip.locationIdsToString());
	}

	private static void initAPGraphs(Injector injector) {
		Map<GraphType,Graph<SimulationNode, SimulationEdge>> graphs = new HashMap<>();
        graphs.put(EGraphType.HIGHWAY, CreateCH.getGraph(injector));
        
        
        //map init
        injector.getInstance(Graphs.class).setGraphs(graphs);
	}
}
