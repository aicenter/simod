package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.EdgeShape;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.MainModule;
import cz.cvut.fel.aic.amodsim.MapInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author F.I.D.O.
 */
public class CreateCH {
	public static void main(String[] args) throws IOException {
		AmodsimConfig config = new AmodsimConfig();
        
        File localConfigFile = args.length > 0 ? new File(args[1]) : null;
        
        Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();

        
		
		Graph<SimulationNode,SimulationEdge> graph = getGraph(injector);
		
		// CH
		CHGraph cHGraph = new CHGraph(graph);
		
		cHGraph.prepare();
		
		BinaryFormat bf = new BinaryFormat();
		bf.write(cHGraph.getChNodes(), "contracted-nodes.dat", "contracted-ways.dat");
	}
	
	public static Graph<SimulationNode,SimulationEdge> getTestGraph(Injector injector) {
		GraphBuilder<SimulationNode, SimulationEdge> graphBuilder = new GraphBuilder<>();

        SimulationNode node0 = new SimulationNode(0, 0, 0, 0, 0, 0, 0);
        SimulationNode node1 = new SimulationNode(1, 0, 0, 0, 0, 10000, 0);
        SimulationNode node2 = new SimulationNode(2, 0, 0, 0, 10000, 10000, 0);
        SimulationNode node3 = new SimulationNode(3, 0, 0, 0, 0, 20000, 0);

        graphBuilder.addNode(node0);
        graphBuilder.addNode(node1);
        graphBuilder.addNode(node2);
        graphBuilder.addNode(node3);

        SimulationEdge edge1 = new SimulationEdge(node0, node1, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node0, node1)));
        SimulationEdge edge2 = new SimulationEdge(node1, node0, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node1, node0)));
        SimulationEdge edge3 = new SimulationEdge(node1, node2, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node1, node2)));
        SimulationEdge edge4 = new SimulationEdge(node2, node1, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node2, node1)));
        SimulationEdge edge5 = new SimulationEdge(node1, node3, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node1, node3)));
        SimulationEdge edge6 = new SimulationEdge(node3, node1, 0, 0, 0, 100, 40, 1, new EdgeShape(Arrays.asList(node3, node1)));

        graphBuilder.addEdge(edge1);
        graphBuilder.addEdge(edge2);
        graphBuilder.addEdge(edge3);
        graphBuilder.addEdge(edge4);
        graphBuilder.addEdge(edge5);
        graphBuilder.addEdge(edge6);
        
        Graph<SimulationNode, SimulationEdge> graph = graphBuilder.createGraph();
		
		return graph;
	}

	public static Graph<SimulationNode,SimulationEdge> getGraph(Injector injector) {
		// prepare map, entity storages...
        MapData mapData = injector.getInstance(MapInitializer.class).getMap();
		return mapData.graphByType.get(EGraphType.HIGHWAY);
	}
}
