package cz.cvut.fel.aic.amodsim.ridesharing.offline.entities;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.io.*;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTransformTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link cz.cvut.fel.aic.amodsim.io.TimeTripe} to include trip id, and  ride value earned by operator.
 * 
 * @author Olga Kholkovskaia
 * @param <L> location type
 */
public class TripTaxify<L> extends TimeTrip<L>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransformTaxify.class);

    /**
     * unique id of the trip from input .csv file
     */
    public final int id;
//    private SimulationNode startNode;
//    private SimulationNode endNode;

    public L getStartNode() {
        return locations[0];
    }
//
//    public void setStartNode(SimulationNode startNode) {
//        this.startNode = startNode;
//    }
//
    public L getEndNode() {
        return locations[locations.length - 1];
    }
    
    /**
     * Creates new trip.
     * 
     * @param tripId
     * @param startTime
     * @param locations
     */
    public TripTaxify(int tripId, long startTime, L... locations){
        super(startTime, locations);
        this.id = tripId;
    }
 }

