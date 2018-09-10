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
    public final int id;
    private final double value;
    private int[] path;

	public TimeValueTrip(int tripId, LinkedList<L> locations, long startTime, double rideValue){
        super(locations, startTime);
		id = tripId;
        value = rideValue;
	}
    
    public TimeValueTrip(int tripId, L startLocation, L endLocation, long startTime, double rideValue){
        super(startLocation, endLocation, startTime);
        this.value = rideValue;
        this.id = tripId;
	}
    
   
    public double getRideValue(){
        return value;
    }
    
    public boolean hasPath(){
        return path != null;
    }
    
    public int[] getPath(){
        if(hasPath()) return path;
        return new int[0];
    }
    
    public void setPath(int[] path){
        this.path = path;
    }

    @Override
    public L getAndRemoveFirstLocation() {
        return super.getAndRemoveFirstLocation();
    }
  
    @Override
    public boolean isEmpty() {
        return super.isEmpty(); 
    }
}
