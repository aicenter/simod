/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import cz.cvut.fel.aic.geographtools.Graph;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * Container for grouped demand.
 * Used in MyOfflineVGASolver.
 * @author Olga Kholkovskaia <olga.kholkovskaya@gmail.com>
 */
public class GroupNormalDemand extends Demand<int[]>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupNormalDemand.class);
    
    NormalDemand normalDemand;
    List<int[]> plans;
    
	public GroupNormalDemand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, 
			List<int[]> plans, NormalDemand normalDemand, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, plans, graph);
//        LOGGER.debug("plans for group demand "+ plans.size());
        this.normalDemand = normalDemand;
        this.plans = prepareDemand(plans);
//        if(!plans.isEmpty()){
//            for (int i = 0; i < 20; i++){
//            LOGGER.debug(Arrays.toString(getTripByIndex(i)) +": "+getStartNodeId(i) + " -> " + getEndNodeId(i) + 
//                "; start time " + getStartTime(i) + ", shortest route "+getBestTime(i));
//            }
//        }
    }
   	
    public int[] getPlanByIndex(int ind){
        return plans.get(ind);
    }
    
    public NormalDemand getNormalDemand(){
        return normalDemand;
    }

    private  Map<int[], Integer> mapByStartTime(List<int[]> plans){
        
//        LOGGER.debug("plans for sorting "+ plans.size());
        Map<int[], Integer> planStartTimes = plans.stream().collect(Collectors.toMap((p)->p, (p)->normalDemand.getStartTime(p[1])));
                       
//        LOGGER.debug("map "+ planStartTimes.size());
//        LOGGER.debug("Unsorted");
//        int count = 0;
//        for(int[] p : plans){
//            if( count++ > 20) break;     
//            LOGGER.debug("\nfirst trip "+p[1] + " starts " + planStartTimes.get(p));
//        }
        
        Map<int[], Integer> sortedByStartTime = planStartTimes.entrySet().stream()
            .sorted(Map.Entry.comparingByValue()).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
//       count = 0;
//        LOGGER.debug("Sorted");
//        for(int[] plan: sortedByStartTime.keySet()){       
//            LOGGER.debug("\nfirst trip "+plan[1] + " starts " + planStartTimes.get(plan));
//        }

       return sortedByStartTime;
    }
  
	private List<int[]>  prepareDemand(List<int[]> plans) {
        List<int[]> sortedPlans = new LinkedList<>();
        Map<int[], Integer> startTimesMap = mapByStartTime(plans);
        int timeToStart = config.ridesharing.offline.timeToStart;

        int planCounter = 0;
        
        for(int[] plan: startTimesMap.keySet()){
            int startTime = startTimesMap.get(plan) + timeToStart;
            int planCost = plan[0];
            int planTime = planCost - timeToStart;
            SimulationNode firstNode = normalDemand.getStartNode(plan[1]);
            SimulationNode lastNode = normalDemand.getEndNode(-plan[plan.length-1] -1);
            sortedPlans.add(plan);
            addGroupPlanToIndex(planCounter++, startTime, planTime, firstNode.id, lastNode.id);
        }
        return sortedPlans;
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
