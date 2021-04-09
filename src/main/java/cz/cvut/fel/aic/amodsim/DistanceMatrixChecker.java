/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.mapVisualization.MapVisualiserModule;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.DistanceMatrixTravelTimeProvider;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.util.Map;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

/**
 * This executable tests whether the distance matrix travel times correspond to the travel times computed by 
 * the AgentPolis AStar planner. If not, then the distance matrix cannot be used for SiMoD.
 * @author Fido
 */
public class DistanceMatrixChecker {
	public static class VehicleType implements EntityType{

		@Override
		public String getDescriptionEntityType() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
		
	}
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixChecker.class);
	
	private static final int TOLERANCE = 3;
	
	public static void main(String[] args) {
		AmodsimConfig config = new AmodsimConfig();
		File localConfigFile = args.length > 0 ? new File(args[0]) : null;
		
		// Guice configuration
		Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();

		// prepare map
		MapInitializer mapInitializer = injector.getInstance(MapInitializer.class);
		MapData mapData = mapInitializer.getMap();
		injector.getInstance(AllNetworkNodes.class).setAllNetworkNodes(mapData.nodesFromAllGraphs);
		injector.getInstance(Graphs.class).setGraphs(mapData.graphByType);
		
		// travel time providers
		AstarTravelTimeProvider astarTravelTimeProvider = injector.getInstance(AstarTravelTimeProvider.class);
		DistanceMatrixTravelTimeProvider distanceMatrixTravelTimeProvider 
						= injector.getInstance(DistanceMatrixTravelTimeProvider.class);
		
		Graph<SimulationNode,SimulationEdge> graph = mapData.graphByType.get(EGraphType.HIGHWAY);
		LOGGER.info("Edge count: {}", graph.getAllEdges().size());
		
		AgentpolisConfig agentpolisConfig = injector.getInstance(AgentpolisConfig.class);
		Vehicle veh = new PhysicalVehicle("Test vehicle", new DistanceMatrixChecker.VehicleType(), 4, EGraphType.HIGHWAY, graph.getNode(0),
				agentpolisConfig.maxVehicleSpeedInMeters);
		
		// first check all edges if it fails here, there is something wrong with the traveltime computation, or Astar 
		// uses some edges not present in the adjectency matrix used in dm generation.
		LOGGER.info("Checking distances for edges");
		for(SimulationEdge edge: graph.getAllEdges()){
			SimulationNode from = edge.fromNode;
			SimulationNode to = edge.toNode;
			double durationAstar = astarTravelTimeProvider.getTravelTime(veh, from, to);
			double durationDm = distanceMatrixTravelTimeProvider.getTravelTime(veh, from, to);
			LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", 
					from,
					from.getIndex(), 
					to, 
					to.getIndex(), 
					durationAstar, 
					durationDm, 
					durationAstar - durationDm);
			if(Math.abs(durationAstar - durationDm) > TOLERANCE){
				LOGGER.info("Distances in Astar and DM differ too much ({} ms): Astar: {}, DM: {}. Distance From {} to {}",
					durationAstar - durationDm, durationAstar, durationDm, from.getIndex(), to.getIndex());
			}
		}
		
		// custom checks for debugging
		LOGGER.info("Checking distances for edges");
		Map<Integer, SimulationNode> map = injector.getInstance(AllNetworkNodes.class).getAllNetworkNodes();
		int[] from_ids = {2123, 2123, 3110};
		int[] to_ids = {1568, 3110, 1568};
		for (int i = 0; i < to_ids.length; i++) {
			int to_id = to_ids[i];
			int from_id = from_ids[i];
			SimulationNode from = map.get(from_id);
			SimulationNode to = map.get(to_id);
			double durationAstar = astarTravelTimeProvider.getTravelTime(veh, from, to);
			double durationDm = distanceMatrixTravelTimeProvider.getTravelTime(veh, from, to);
			LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", 
					from,
					from.getIndex(), 
					to, 
					to.getIndex(), 
					durationAstar, 
					durationDm, 
					durationAstar - durationDm);
			if(Math.abs(durationAstar - durationDm) > TOLERANCE){
				LOGGER.error("Distances in Astar and DM differ too much ({} ms): Astar: {}, DM: {}. Distance From {} to {}",
					durationAstar - durationDm, durationAstar, durationDm, from.getIndex(), to.getIndex());
			}
			LOGGER.error("Distances: Astar: {}, DM: {}. Distance From {} to {}",
					durationAstar, durationDm, from.getIndex(), to.getIndex());
		}
		
		LOGGER.info("Checking distances for longer trips");
		
		ProgressBar pb = new ProgressBar("Processing all origin-destination combinations", map.size());
		for (int i = 0; i < map.size(); i++) {
			SimulationNode from = map.get(i);
			for (int j = 0; j < map.size(); j++) {
				if(j == i){
					continue;
				}
				SimulationNode to = map.get(j);
				double durationAstar = astarTravelTimeProvider.getTravelTime(veh, from, to);
				double durationDm = distanceMatrixTravelTimeProvider.getTravelTime(veh, from, to);
				LOGGER.trace("From {}(index {}) to {}(index {}), astar distance: {}, dm distance: {}, difference {}", 
						from,
						from.getIndex(), 
						to, 
						to.getIndex(), 
						durationAstar, 
						durationDm, 
						durationAstar - durationDm);
				if(Math.abs(durationAstar - durationDm) > TOLERANCE){
					LOGGER.error("Distances in Astar and DM differ too much ({} ms): Astar: {}, DM: {}. Distance From {} to {}",
						durationAstar - durationDm, durationAstar, durationDm, from.getIndex(), to.getIndex());
				}
			}
			pb.step();
		}
	}
}
