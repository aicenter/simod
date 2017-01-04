/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.tripUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trip;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
public class SimpleJsonTrip extends Trip<JsonTripItem>{
    
    @JsonCreator
    public SimpleJsonTrip(@JsonProperty("locations") LinkedList<JsonTripItem> locations) {
        super(locations);
    }
    
}
