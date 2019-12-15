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
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import cz.cvut.fel.aic.geographtools.Graph;
import java.util.Arrays;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F.I.D.O.
 */
public class GroupDemand extends Demand<DriverPlan>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupDemand.class);
    
    
	public GroupDemand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, 
			List<DriverPlan> demand, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, demand, graph);
        LOGGER.debug("size of demand "+demand.size());
        prepareDemand();
        
//        if(!demand.isEmpty()){
//            for (int i = 0; i < 20; i++){
//            LOGGER.debug(getTripByIndex(i).toString() +": "+getStartNodeId(i) + " -> " + getEndNodeId(i) + 
//                "; start time " + getStartTime(i) + ", shortest route "+getBestTime(i));
//            }
//        }
   	}

    private List<DriverPlan> sortByStartTime(List<DriverPlan> demand){
        LOGGER.debug("Sorting demand ");
        List<int[]> times = new LinkedList<>();
        for(int i = 0; i < demand.size(); i++){
            Integer startTime = null;
            DriverPlan plan = demand.get(i);
            for(PlanAction action : plan){
                if( action instanceof PlanActionPickup && startTime == null){
                    PlanActionPickup pickup = (PlanActionPickup) action;
                    startTime = pickup.request.getOriginTime();
                    break;
                }
            }
            int[] time = {i, startTime};
            times.add(time);
        }
        Collections.sort(times, (int[] o1, int[] o2) -> {
            Integer i1 = o1[1];
            Integer i2 = o2[1];
            return i1.compareTo(i2);
        });
        
        List<DriverPlan> sortedDemand = new LinkedList<>();
        times.forEach((t) -> {
            sortedDemand.add(demand.get(t[0]));
        }); 
        return sortedDemand;
        
    }

	private void prepareDemand() {
        int timeToStart = config.ridesharing.offline.timeToStart;
        LOGGER.debug("Prepare demand "+demand.size());
        demand = sortByStartTime(demand);
        
        for (int i = 0; i < demand.size(); i++) {
            DriverPlan plan = demand.get(i); 
            PlanActionPickup firstPickup = (PlanActionPickup) plan.get(0);
            SimulationNode firstNode  = firstPickup.getPosition();
            int startTime = firstPickup.request.getOriginTime()*1000 + timeToStart;
            
            PlanActionDropoff lastDropoff = (PlanActionDropoff) plan.get(plan.size() -1);
            SimulationNode lastNode  = lastDropoff.getPosition();
         
        
            addGroupPlanToIndex(i, startTime, (int) plan.cost*1000, firstNode.id, lastNode.id);
        }
  	}
    

	private void addGroupPlanToIndex(int plan_id, int startTime, 
        int bestTime, int startNodeId, int endNodeId  ) {
        int ind = lastInd;
        updateIndex(plan_id, ind);
        updateTime(ind, startTime, bestTime);
        updateNodes(ind, startNodeId, endNodeId);
        lastInd++;
    }
  
}
