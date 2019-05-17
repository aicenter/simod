package cz.cvut.fel.aic.amodsim.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import java.util.LinkedList;

/**
 *
 * @author F-I-D-O
 * @param <L> location type
 */
public class TimeTrip<L> extends Trip<L>{
		
	private final long startTime;
	
	private final long endTime;

	
	
	
	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
	
	
	@JsonCreator
	public TimeTrip(@JsonProperty("locations") LinkedList<L> locations, @JsonProperty("startTime") long startTime, 
			@JsonProperty("endTime") long endTime){
		super(locations);
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public TimeTrip(LinkedList<L> locations, long startTime){
		this(locations, startTime, 0);
	}
	
	public TimeTrip(L startLocation, L endLocation, long startTime){
		super(startLocation, endLocation);
		this.startTime = startTime;
		this.endTime = 0;
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
