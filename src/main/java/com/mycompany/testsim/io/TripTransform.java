/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.io;

import com.google.common.collect.Sets;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GTDGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.util.List;

/**
 *
 * @author F-I-D-O
 */
public class TripTransform {
	
	public static List<Trip<Long>> GPSTripsToOSMNodeTrips(List<Trip<GPSLocation>> GPSTrips, String osmFilePath){
		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(new Transformer(epsg), mapFile, Sets.immutableEnumSet
                (ModeOfTransport.CAR), null, null);
        Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();
	}
}
