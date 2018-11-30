/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.GroupPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.HopcroftKarp;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F.I.D.O.
 */
public class GroupDemand extends Demand<GroupPlan>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupDemand.class);
    
    
	public GroupDemand(TravelTimeProvider travelTimeProvider, ConfigTaxify config, 
			List<GroupPlan> demand, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, demand, graph);
   	}

	@Override
	void prepareDemand(List<GroupPlan> demand) {
		int buffer = config.timeBuffer/2;
        for (GroupPlan groupPlan : demand) {
            int bestTime = (int) (groupPlan.getDuration() / 1000);
			addGroupPlanToIndex(groupPlan, bestTime, buffer);
        }
	}

	private void addGroupPlanToIndex(GroupPlan groupPlan, int bestTime, int buffer) {
		int ind = lastInd;
        //index[trip.id] = ind;
        //revIndex[ind] = trip.id;
        startTimes[ind] = (int) groupPlan.getStartTimeInSeconds() + buffer;
        bestTimes[ind] = bestTime;
        gpsCoordinates[ind] = groupPlan.getCoordinates();
 //       LOGGER.debug(trip.id +" "+Arrays.toString(trip.getGpsCoordinates()));
        values[ind] = groupPlan.getRideValue();

        Map<Integer,Double> nodeMap = groupPlan.getStartNodeMap();
        addNodesToIndex(nodeMap, startNodes, ind);
        nodeMap = (Map<Integer,Double>)  groupPlan.getTargetNodeMap();
        addNodesToIndex(nodeMap, endNodes, ind);
        addCoordinatesToIndex(groupPlan.getLocations(), ind);
//        demand.add(groupPlan);
        lastInd++;
	}
    
    protected int[] findMapCover(int sigma){
        HopcroftKarp hp = new HopcroftKarp(N);
        int[] pair_u = hp.findMapCover(buildAdjacency(sigma));
        return pair_u;
    }
   
    private int[][] buildAdjacency(int sigma) {
        int maxWaitTime = (int) (config.maxWaitTime * 0.3);
        LOGGER.debug("sigma in millis " + sigma);
        LOGGER.debug("timeLine length: " + startTimes.length);
        int[][] adjacency = new int[N][];
        
        for (int tripInd = 0; tripInd < N; tripInd++) {
            List<Integer> neighbors = new ArrayList<>();
           // LOGGER.debug("trip = "+ind2id(tripInd) +"; start "+getStartTime(tripInd)+"; end "+getEndTime(tripInd));
            int timeLimit = getEndTime(tripInd) + sigma;
           // LOGGER.debug("timeLimit = "+timeLimit);
            int lastTripInd = getIndexByStartTime(timeLimit);
           // LOGGER.debug("returned index = "+lastTripInd+", starts at "+getStartTime(lastTripInd));
            for (int nextTripInd = tripInd + 1; nextTripInd < lastTripInd; nextTripInd++) {
              //  LOGGER.debug("  nextTrip = "+ind2id(nextTripInd) +"; start "+getStartTime(nextTripInd));
                if(getStartTime(nextTripInd) < getEndTime(tripInd)){
                   // LOGGER.error("Next trip starts before the previous ends"+ind2id(tripInd)+" "+ind2id(nextTripInd));
                    continue;
                }
                int bestTravelTimeMs = travelTimeProvider.getTravelTimeInMillis(getEndNodes(tripInd), getStartNodes(nextTripInd));
                int travelTime = bestTimes[tripInd] + bestTravelTimeMs + config.timeBuffer;
               // LOGGER.debug("  travel time = "+travelTime +"; start "+getStartTime(nextTripInd));
                if (getEndTime(tripInd) + travelTime <= getStartTime(nextTripInd) + maxWaitTime) {
                   // LOGGER.debug("  prev end +tt "+ (getEndTime(tripInd)+travelTime)+" next start "+startTimes[nextTripInd]);
                    neighbors.add(nextTripInd);
                }
            }
            adjacency[tripInd] = neighbors.stream().mapToInt(Integer::intValue).sorted().limit(50).toArray();
           // System.out.println("Processed="+tripInd+", total="+C);
        }
        double avg = Arrays.stream(adjacency).map((int[] ns) -> ns.length).mapToInt(Integer::intValue).sum() / N;
        LOGGER.debug("average edges per node " + avg);
        return adjacency;
    }

}
