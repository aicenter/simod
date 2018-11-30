/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public class NormalDemand extends Demand<TripTaxify<GPSLocation>>{
       
       
       
	public NormalDemand(TravelTimeProvider travelTimeProvider, ConfigTaxify config, 
			List<TripTaxify<GPSLocation>> demand, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, demand, graph);
        
	}

	@Override
	void prepareDemand(List<TripTaxify<GPSLocation>> demand) {
        int buffer = config.timeBuffer/2;
        for (TripTaxify<GPSLocation> trip : demand) {
            int bestTime = travelTimeProvider.getTravelTimeInMillis(trip);
			addTripToIndex(trip, bestTime, buffer);
        }
    }
	
	// helpers for prepareDemand
    private void addTripToIndex(TripTaxify<GPSLocation> trip, int bestTime, int buffer){
        int ind = lastInd;
        //index[trip.id] = ind;
        revIndex[ind] = trip.id;
        startTimes[ind] = (int) trip.getStartTime() + buffer;
        bestTimes[ind] = bestTime;
        gpsCoordinates[ind] = trip.getGpsCoordinates();
 //       LOGGER.debug(trip.id +" "+Arrays.toString(trip.getGpsCoordinates()));
        values[ind] = trip.getRideValue();

        Map<Integer,Double> nodeMap = (Map<Integer,Double>)  trip.nodes.get(0);
        addNodesToIndex(nodeMap, startNodes, ind);
        nodeMap = (Map<Integer,Double>)  trip.nodes.get(1);
        addNodesToIndex(nodeMap, endNodes, ind);
        addCoordinatesToIndex(trip.getLocations(), ind);
        demand.add(trip);
        lastInd++;
    }
}
