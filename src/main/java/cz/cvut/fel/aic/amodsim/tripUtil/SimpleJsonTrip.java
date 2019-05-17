/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.tripUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.TripItem;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
public class SimpleJsonTrip extends Trip<JsonTripItem>{
	
	public static LinkedList<JsonTripItem> getLocationList(int[] locationsArray){
		LinkedList<JsonTripItem> locationList = new LinkedList<>();
		for (int i = 0; i < locationsArray.length; i++) {
			locationList.add(new JsonTripItem(locationsArray[i]));
		}
		return locationList;
	}
	
//	@JsonCreator
//	public SimpleJsonTrip(@JsonProperty("locations") LinkedList<JsonTripItem> locations) {
//		super(locations);
//	}
	
	public SimpleJsonTrip(LinkedList<JsonTripItem> locations) {
		super(locations);
	}
	
	@JsonCreator
	public SimpleJsonTrip(int[] locationsArray) {
		this(getLocationList(locationsArray));
	}
	
	

	@JsonValue
	public int[] serialize(){
		int[] locationsArray = new int[locations.size()];
		for (int i = 0; i < locationsArray.length; i++) {
			
			// this does not work due to some misterious bug in jackson
//			positions[i] = locations.get(i).tripPositionByNodeId;

			TripItem tripItem = locations.get(i);
			locationsArray[i] = tripItem.tripPositionByNodeId;
		}
		return locationsArray;
	}
	
}
