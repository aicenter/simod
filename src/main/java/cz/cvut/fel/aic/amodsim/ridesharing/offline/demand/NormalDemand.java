/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Demand;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.LoggerFactory;


/**
 *
 * @author F.I.D.O.
 */
public class NormalDemand extends Demand<TripTaxify<SimulationNode>>{
        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NormalDemand.class);
       
       
	public NormalDemand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, 
			List<TripTaxify<SimulationNode>> demand, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, demand, graph);
        prepareDemand(demand);
//        for (int i = 0; i < 10; i++){
//            System.out.println(getTripByIndex(i).id +": "+getStartNodeId(i) + " -> " + getEndNodeId(i) + 
//                "; start time " + getStartTime(i) + ", shortest route "+getBestTime(i));
//            System.out.println("unproj: " +getStartLongitude(i) + ", " + getStartLatitude(i)+"; proj: " +
//               getTargetLongitudeProj(i) + ", " +getTargetLatitudeProj(i));
//        }
    }

	private void prepareDemand(List<TripTaxify<SimulationNode>> demand) {
        Collections.sort(demand, Comparator.comparing(TripTaxify::getStartTime));
        for (TripTaxify<SimulationNode> trip : demand) {
//            System.out.println("Start lon"+trip.getStartNode().getLongitude() + " proj "+ trip.getStartNode().getLongitudeProjected());
//            System.out.println("Start lat"+trip.getStartNode().getLatitude() + " proj "+ trip.getStartNode().getLatitudeProjected());
//            System.out.println("End lon"+trip.getEndNode().getLongitude() + " proj "+ trip.getEndNode().getLongitudeProjected());
//            System.out.println("End lat"+trip.getEndNode().getLatitude() + " proj "+ trip.getEndNode().getLatitudeProjected());
//            
            int bestTime = (int) travelTimeProvider.getExpectedTravelTime(trip.getStartNode(), 
                trip.getEndNode());
//             System.out.println("Best time "+ bestTime);
			addTripToIndex(trip, bestTime);
        }
        
    }

    private void addTripToIndex(TripTaxify<SimulationNode> trip, int bestTime){
        int ind = lastInd++;
        updateIndex(trip.id, ind);
        updateTime(ind, (int) trip.getStartTime(), bestTime);
        updateNodes(ind, trip.getStartNode().id, trip.getEndNode().id);
    }

}
