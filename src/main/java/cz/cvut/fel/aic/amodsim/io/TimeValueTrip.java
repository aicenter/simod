package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;

/**
 * Extends TimeTripe class to include ride value (price paid for the trip
 * by the client).
 * 
 * 
 * 
 * @author olga
 * @param <L> location type
 */
public class TimeValueTrip<L> extends TimeTrip<L>{
    private final double rideValue;

	public TimeValueTrip(LinkedList<L> locations, long startTime, double rideValue){
		this(locations, startTime, 0, rideValue);
	}
    
    public TimeValueTrip(L startLocation, L endLocation, long startTime, double rideValue){
        super(startLocation, endLocation, startTime);
        this.rideValue = rideValue;
	}
    
	@JsonCreator
	public TimeValueTrip(@JsonProperty("locations") LinkedList<L> locations, @JsonProperty("startTime") long startTime, 
			@JsonProperty("endTime") long endTime, @JsonProperty("rideValue") double rideValue){
		super(locations, startTime, endTime);
        this.rideValue = rideValue;
	}
    
    public double getRideValue(){
        return rideValue;
    }

    @Override
    @JsonIgnore
    public L getAndRemoveFirstLocation() {
        return super.getAndRemoveFirstLocation();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty(); 
    }
}
