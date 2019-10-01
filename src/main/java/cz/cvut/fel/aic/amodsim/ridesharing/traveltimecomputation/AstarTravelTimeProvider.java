/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.Iterator;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class AstarTravelTimeProvider extends TravelTimeProvider{
	
	
	private final TripsUtil tripsUtil;
	
	
	private final Graph<SimulationNode, SimulationEdge> graph;

	
	@Inject
	public AstarTravelTimeProvider(TimeProvider timeProvider, TripsUtil tripsUtil, TransportNetworks transportNetworks) {
		super(timeProvider);
		this.tripsUtil = tripsUtil;
		this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
	}
	
	
	@Override
	public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB){
		
		if(positionA == positionB){
			return 0;
		}
		
		Trip<SimulationNode> trip = tripsUtil.createTrip(positionA.id, positionB.id);
		long totalDuration = 0;
		
		Iterator<SimulationNode> nodeIterator = trip.getLocations().iterator();
		Node fromNode = nodeIterator.next();
		while (nodeIterator.hasNext()) {
			Node toNode = nodeIterator.next();
			
			SimulationEdge edge = graph.getEdge(fromNode, toNode);
			
			if(entity == null){
				totalDuration += MoveUtil.computeMinDuration(edge);
			}
			else{
				totalDuration += MoveUtil.computeDuration(entity, edge);
			}
			
			fromNode = toNode;
		}
		return totalDuration;
	}

	
}
