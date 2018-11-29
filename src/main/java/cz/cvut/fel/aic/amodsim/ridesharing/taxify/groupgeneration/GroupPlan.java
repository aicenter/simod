/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class GroupPlan {
	final Set<Request> requests;
	
	private final Plan plan;

	GroupPlan(Set<Request> requests) {
		this(requests, null);
	}

	GroupPlan(Set<Request> requests, Plan plan) {
		this.requests = requests;
		this.plan = plan;
	}
	
//	long getStartTime(){
//		return plan.startTime;
//	}
//	
//	long getEndTime(){
//		return plan.endTime;
//	}
	
	boolean overlaps(Request request){
		return request.time < plan.endTime && request.maxDropOffTime > plan.startTime;
	}
	
	public long getDuration(){
		return plan.endTime - plan.startTime;
	}
	
	public int getStartTimeInSeconds(){
		return (int) plan.startTime;
	}
		
	public double[] getCoordinates(){
		TripTaxify fromTrip = plan.actions.getFirst().getTrip();
		TripTaxify toTrip = plan.actions.getLast().getTrip();
		
		double[] coords = new double[4];
		coords[0] = fromTrip.getGpsCoordinates()[0];
		coords[1] = fromTrip.getGpsCoordinates()[1];
		coords[2] = toTrip.getGpsCoordinates()[2];
		coords[3] = toTrip.getGpsCoordinates()[3];
		
		return coords;
	}
	
	public double getRideValue(){
		double rideValue = 0;
		for(Action action: plan.actions){
			rideValue += action.getTrip().getRideValue();
		}
		return rideValue;
	}
	
	public Map<Integer, Double> getStartNodeMap(){
		TripTaxify fromTrip = plan.actions.getFirst().getTrip();
		return (Map<Integer,Double>)  fromTrip.nodes.get(0);	
	}
	
	public Map<Integer, Double> getTargetNodeMap(){
		TripTaxify toTrip = plan.actions.getLast().getTrip();
		return (Map<Integer,Double>)  toTrip.nodes.get(1);	
	}
	
	public List<GPSLocation> getLocations(){
		TripTaxify<GPSLocation> fromTrip = plan.actions.getFirst().getTrip();
		TripTaxify<GPSLocation> toTrip = plan.actions.getLast().getTrip();
		
		List<GPSLocation> locations = new ArrayList<>();
		locations.add(fromTrip.getLocations().getFirst());
		locations.add(toTrip.getLocations().getLast());
		
		return locations;
	}
}
