package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.io.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Extends TimeTripe class to include ride value (price paid for the trip
 * by the client).
 * 
 * 
 * 
 * @author olga
 * @param <L> location type
 */
public class TripTaxify<L> extends TimeTrip<L>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransformTaxify.class);
    public final int id;
    private final double value;
    private double shortestLength;
    private double[] gpsCoordinates; // real gpsCoordinates of pickup and drop off
    public List<Map<Integer, Double>> nodes;
    
       
    public TripTaxify(int tripId, L startLocation, L endLocation, long startTime, double rideValue){
        super(startLocation, endLocation, startTime);
        this.value = rideValue;
        this.id = tripId;
	}
    public void addNodeMaps(Map<Integer, Double> start,Map<Integer, Double> target){
        nodes = new ArrayList<>();
        nodes.add(0, start);
        nodes.add(1, target); 
    }

    public double[] getGpsCoordinates() {
        //LOGGER.debug(Arrays.toString(gpsCoordinates));
        return gpsCoordinates;
    }

    public void setCoordinates(double[] coordinates) {
        //LOGGER.debug(Arrays.toString(coordinates));
        this.gpsCoordinates = coordinates;
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
