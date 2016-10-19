package com.mycompany.testsim;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.DemandAgent;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanners;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripException;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
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

    
    
    
    public VehicleTrip locationsToTrips(List<Node> locations, boolean precomputedPaths, Vehicle vehicle){
        if(!precomputedPaths && pathPlanner == null){
            pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES);
        }
        
        LinkedList<TripItem> tripItems = new LinkedList<>();
        
        int startNodeId = locations.get(0).getId();
        
        tripItems.add(new TripItem(startNodeId));
		VehicleTrip finalTrip = null;
		for (int i = 1; i < locations.size(); i++) {
			int targetNodeId = locations.get(i).getId();
            if(startNodeId == targetNodeId){
                try {
                    throw new Exception("There can't be two identical locations in a row");
                } catch (Exception ex) {
                    Logger.getLogger(TripsUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
			if(precomputedPaths){
                tripItems.add(new TripItem(targetNodeId));
			}
			else{
				try {
					VehicleTrip partialTrip = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
                    while (partialTrip.hasNextTripItem()) {
                        tripItems.add(partialTrip.getAndRemoveFirstTripItem());
                    }
					
				} catch (TripPlannerException ex) {
					Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			startNodeId = targetNodeId;
		}
        
		finalTrip = new VehicleTrip(tripItems, EGraphType.HIGHWAY, vehicle.getId());
        
    
        return finalTrip;
    }
    
    public VehicleTrip createTrip(int startNodeId, int targetNodeId, Vehicle vehicle){
        if(startNodeId == targetNodeId){
            try {
                throw new Exception("Start node cannot be the same as end node");
            } catch (Exception ex) {
                Logger.getLogger(TripsUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(pathPlanner == null){
            pathPlanner = pathPlanners.getPathPlanner(GRAPH_TYPES);
        }
        
        VehicleTrip finalTrip = null;
        try {
            finalTrip = pathPlanner.findTrip(vehicle.getId(), startNodeId, targetNodeId);
        } catch (TripPlannerException ex) {
            Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        return finalTrip;
    }
	
	public static VehicleTrip mergeTrips(VehicleTrip... trips){
		int i = 0;
		VehicleTrip firstTrip = null;
		do{
			firstTrip = trips[i];
			i++;
		}while(firstTrip == null);
		
		VehicleTrip newTrip = new VehicleTrip(new LinkedList<>(), firstTrip.getGraphType(), firstTrip.getVehicleId());
		
		for(int j = 0; j < trips.length; j++){
			VehicleTrip trip = trips[j];
			if(trip != null){
				for (TripItem location : trip.getLocations()) {
					newTrip.extendTrip(location);
				}
			}
		}
		return newTrip;
	}
}
