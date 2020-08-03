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

/**
 *
 * @author fido
 */
public class SimpleJsonTrip extends Trip<Integer>{
	
//	public static JsonTripItem[] getLocationList(int[] locationsArray){
//		JsonTripItem[] locationList = new JsonTripItem[locationsArray.length];
//		for (int i = 0; i < locationsArray.length; i++) {
//			locationList[i] = new JsonTripItem(locationsArray[i]);
//		}
//		return locationList;
//	}
	
	
	@JsonCreator
	public SimpleJsonTrip(int tripId,Integer... locations) {
		super(tripId,locations);
	}

//	@JsonCreator
//	public SimpleJsonTrip(int[] locationsArray) {
//		super(locationsArray);
//	}
	
	

	@JsonValue
	public Integer[] serialize(){
//		for (int i = 0; i < locations.length; i++) {
//			
//			// this does not work due to some misterious bug in jackson
////			positions[i] = locations.get(i).tripPositionByNodeId;
//
//			TripItem tripItem = locations[i];
//			locationsArray[i] = tripItem.tripPositionByNodeId;
//		}
		return locations;
	}
	
}
