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
