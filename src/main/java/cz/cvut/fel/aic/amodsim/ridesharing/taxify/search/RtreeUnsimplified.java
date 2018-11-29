/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;
import com.github.davidmoten.rtree.geometry.Rectangle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.Collection;

/**
 *
 * @author F.I.D.O.
 */
public class RtreeUnsimplified extends Rtree{

	public RtreeUnsimplified(Collection<SimulationNode> nodes, Collection<SimulationEdge> edges) {
		super(nodes, edges);
	}

	@Override
	void fillEdgeTree(Collection<SimulationEdge> edges) {
		for(SimulationEdge edge:edges){
			GPSLocation previousLocation = null;
			for(GPSLocation location: edge.shape){
				if(previousLocation != null){
					Rectangle mbr = rectangle(Math.min(previousLocation.getLongitudeProjected(), location.getLongitudeProjected()),
                                       Math.min(previousLocation.getLatitudeProjected(), location.getLatitudeProjected()),
                                       Math.max(previousLocation.getLongitudeProjected(), location.getLongitudeProjected()),
                                       Math.max(previousLocation.getLatitudeProjected(), location.getLatitudeProjected()));
					edgeTree = edgeTree.add(new int[]{edge.fromNode.id, edge.toNode.id}, mbr);
				}
				previousLocation = location;
			}
        }
	}

	
	
}
