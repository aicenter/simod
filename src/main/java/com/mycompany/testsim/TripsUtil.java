package com.mycompany.testsim;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.DemandAgent;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanners;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trips;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.basestructures.Node;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fido
 */
@Singleton
public class TripsUtil {
    
    private static final Set<GraphType> GRAPH_TYPES = new HashSet(Arrays.asList(EGraphType.HIGHWAY));
    
    
    
    private final ShortestPathPlanners pathPlanners;
    
    private ShortestPathPlanner pathPlanner;

    
    
    
    @Inject
    public TripsUtil(ShortestPathPlanners pathPlanners) {
        this.pathPlanners = pathPlanners;
    }

    
    
    
    public Trips locationsToTrips(List<Node> locations, boolean precomputedPaths, Vehicle vehicle){
        if(!precomputedPaths && pathPlanner == null){
            pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES);
        }
        
        Trips finalTrips = new Trips();
        int startNodeId = locations.get(0).getId();
		
		for (int i = 1; i < locations.size(); i++) {
            int targetNodeId = locations.get(i).getId();
			if(precomputedPaths){
				LinkedList<TripItem> tripItems = new LinkedList<>();
				tripItems.add(new TripItem(startNodeId));
				tripItems.add(new TripItem(targetNodeId));

				finalTrips.addEndCurrentTrips(new VehicleTrip(tripItems, EGraphType.HIGHWAY, vehicle.getId()));
			}
			else{
				try {
					Trips trips = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
					for (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<?> trip : trips) {
						finalTrips.addEndCurrentTrips(trip);
					}
					
				} catch (TripPlannerException ex) {
					Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			startNodeId = targetNodeId;
		}
    
        return finalTrips;
    }
    
    public Trips createTrips(int startNodeId, int targetNodeId, Vehicle vehicle){
        if(pathPlanner == null){
            pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES);
        }
        
        Trips finalTrips = new Trips();
        
        try {
            Trips trips = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
            for (cz.agents.agentpolis.siminfrastructure.planner.trip.Trip<?> trip : trips) {
                finalTrips.addEndCurrentTrips(trip);
            }
        } catch (TripPlannerException ex) {
            Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        return finalTrips;
    }
}
