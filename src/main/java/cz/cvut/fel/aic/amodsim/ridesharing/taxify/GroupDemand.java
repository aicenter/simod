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
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public class GroupDemand extends Demand<GroupPlan>{

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
        index[trip.id] = ind;
        revIndex[ind] = trip.id;
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
        lastInd++;
	}

}
