package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Iterator;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class AstarTravelTimeProvider implements TravelTimeProvider{
	
	
	private final TripsUtil tripsUtil;
	
	
	private final Graph<SimulationNode, SimulationEdge> graph;

	
	@Inject
	public AstarTravelTimeProvider(TripsUtil tripsUtil, TransportNetworks transportNetworks) {
		this.tripsUtil = tripsUtil;
		this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
	}
	
	
	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB){
		
		if(positionA == positionB){
			return 0;
		}
		
		Trip<SimulationNode> trip = tripsUtil.createTrip(positionA.id, positionB.id);
		long totalDuration = 0;
		
		Iterator<SimulationNode> nodeIterator = trip.getLocations().iterator();
		int fromNodeId = nodeIterator.next().id;
		while (nodeIterator.hasNext()) {
			int toNodeId = nodeIterator.next().id;
			
			SimulationEdge edge = graph.getEdge(fromNodeId, toNodeId);
			
			totalDuration += MoveUtil.computeDuration(entity, edge);
			
			fromNodeId = toNodeId;
		}
		return totalDuration;
	}
}
