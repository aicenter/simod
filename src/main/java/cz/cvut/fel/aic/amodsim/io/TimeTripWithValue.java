package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Extends TimeTripe class to include ride value (price paid for the trip
 * by the client).
 * 
 * 
 * 
 * @author olga
 * @param <L> location type
 */
public class TimeTripWithValue<L> extends TimeTrip<L>{
    public final int id;
    private final double value;
    private double shortestLength;
    public List<Map<Integer, Double>> nodes;
    
    public TimeTripWithValue(int tripId, LinkedList<L> locations, long startTime, double rideValue) {
        super(locations, startTime);
        id = tripId;
        value = rideValue;
    }
    
    public TimeTripWithValue(int tripId, L startLocation, L endLocation, long startTime, double rideValue){
        super(startLocation, endLocation, startTime);
        this.value = rideValue;
        this.id = tripId;
	}
    public void addNodeMaps(Map<Integer, Double> start,Map<Integer, Double> target){
        nodes = new ArrayList<>();
        nodes.add(0, start);
        nodes.add(1, target); 
    }
    
    public double getShortestLength(){
        return shortestLength;
    }
    public void setShortestLength(double x){
        shortestLength = x;
    }
    public double getRideValue(){
        return value;
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
