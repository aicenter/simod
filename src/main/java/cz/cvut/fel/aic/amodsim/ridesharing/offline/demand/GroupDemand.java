/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import cz.cvut.fel.aic.geographtools.Graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F.I.D.O.
 */
public class GroupDemand extends Demand<DriverPlan>{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupDemand.class);
    
     int timeToStart;
     int maxProlongation;
     List<DriverPlan> demand;
     
     
	public GroupDemand(TravelTimeProvider travelTimeProvider, AmodsimConfig config, 
			List<DriverPlan> optimalPlans, Graph<SimulationNode, SimulationEdge> graph) {
		super(travelTimeProvider, config, optimalPlans, graph);
        
        timeToStart = config.ridesharing.offline.timeToStart;
        maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        LOGGER.debug("size of demand "+optimalPlans.size());
        demand = prepareDemand(optimalPlans);
//        
        if(!demand.isEmpty()){
            for (int i = 0; i < 20; i++){
            LOGGER.debug(getPlanByIndex(i).toString() +": "+getStartNodeId(i) + " -> " + getEndNodeId(i) + 
                "; start time " + getStartTime(i) + ", shortest route "+getBestTime(i));
            }
        }
   	}

    private List<DriverPlan> sortByStartTime(List<DriverPlan> demand){
        LOGGER.debug("Sorting demand ");
        List<int[]> times = new LinkedList<>();
        for(int i = 0; i < demand.size(); i++){
            DriverPlan plan = demand.get(i);
            int startTime = PlanActionPickup.class.cast(plan.get(0)).request.getMaxPickupTime();
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

	private List<DriverPlan> prepareDemand(List<DriverPlan> demand) {
        LOGGER.debug("Prepare demand " + demand.size());
        List<DriverPlan> sortedDemand = sortByStartTime(demand);
           
        for (int i = 0; i < sortedDemand.size(); i++) {
            DriverPlan plan = sortedDemand.get(i); 
            PlanActionPickup firstPickup = (PlanActionPickup) plan.get(0);
            SimulationNode firstNode  = firstPickup.getPosition();
            int startTime = firstPickup.request.getOriginTime()*1000;
            
            PlanActionDropoff lastDropoff = (PlanActionDropoff) plan.get(plan.size() -1);
            SimulationNode lastNode  = lastDropoff.getPosition();
            int planTravelTime = (int) plan.cost*1000;   
            
//            LOGGER.debug("\n"+i+": "+firstNode.id+" -> "+lastNode.id+", start at "+startTime+
//                ", length "+planTravelTime);

//            addGroupPlanToIndex(i, startTime, planTravelTime, firstNode.id, lastNode.id);
            int bestTime = checkGroupPlan(plan, startTime, firstNode, planTravelTime, lastNode);
            if(bestTime > 0){
                addGroupPlanToIndex(i, startTime, bestTime , firstNode.id, lastNode.id);
            }
        }
        
        LOGGER.debug(badPlans.size() +" invalid plans dropped.");
        LOGGER.debug(differentCost +" with generator cost differes from real.");
        LOGGER.debug(duplicatePickups + " duplicate pickups.");
        return sortedDemand;
  	}
    
    int tolerance = 1000;
    int differentCost = 0;
    Set<DriverPlan> badPlans = new HashSet<>();
    Set<Integer> seenTrips = new HashSet<>();
    int duplicatePickups = 0;

  
    private int checkGroupPlan(DriverPlan plan, int startTime, SimulationNode firstNode,
        int planTravelTime,  SimulationNode lastNode){
//        LOGGER.debug("PLAN size "+ plan.size()/2 + ", first request time " + startTime +
//            ", time from start of the batch " + ((startTime - 1000) % 30000) + ", solver plan cost " + planTravelTime );
        int time = startTime;
        SimulationNode node = null;
//        LOGGER.debug("\nplan size "+((int) plan.size()/2));
        for(PlanAction action : plan){
            
            if(action instanceof PlanActionPickup){
                PlanActionPickup pick = (PlanActionPickup) action;
                //chceck duplicate trips 
                if(seenTrips.contains(pick.request.getId())){
                    LOGGER.error("Duplicate pickup node "+pick.request.getId());
                    duplicatePickups++;
                    return -1;
                            
                } 
                seenTrips.add(pick.request.getId());
                
                if(node == null){
                   time += timeToStart;
                    node = pick.getPosition();
                   // LOGGER.debug("Pickup "+ pick.request.getId()+". travel time to node " + timeToStart + ", action time " + time);
                    if(node.id != firstNode.id){
                        LOGGER.error("Expected start node "+firstNode.id + " != "+node.id);
                        return -1;
                    }
                } else {
                    int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, pick.getPosition());
                    time += travelTime;
                   // LOGGER.debug("Pickup "+ pick.request.getId()+". travel time to node "+travelTime + ", action time " + time);
                    int ept = pick.request.getOriginTime()*1000;
                    int lpt = pick.request.getMaxPickupTime()*1000;
                    
                    time = Math.max(ept, time);
                    if ((ept - time ) > tolerance){
                        LOGGER.error("plan size "+((int) plan.size()/2));
                        LOGGER.error(pick.request.getId()+"p("+((int) plan.size()/2)+") arrived "
                            +(ept - time) +"ms too early: ept "+ept+", time " +time);
                                badPlans.add(plan);
                           return -1;
                    }
                    if((time - lpt) > tolerance){
                        LOGGER.error("plan size "+((int) plan.size()/2));
                        LOGGER.error(pick.request.getId()+"d("+((int) plan.size()/2)+") arrived "
                            +(time - lpt) +"ms too late: lpt "+ lpt +", time " +time);
                                badPlans.add(plan);
                           return -1;
                    }
//                    if( time < lpt && time > ept){
//                           LOGGER.debug("ok. " + ept + " < " + time+   " > " + lpt);
//                    }
                }
                node = pick.getPosition();
                           
            }else if(action instanceof PlanActionDropoff){
                PlanActionDropoff drop = (PlanActionDropoff) action;
                int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, drop.getPosition());
                time += travelTime;
             //  LOGGER.debug("Dropoff "+ drop.request.getId()+". travel time to node "+travelTime + ", action time "+ time);
                int lpt = drop.request.getMaxDropoffTime()*1000;
                int ept = lpt - maxProlongation;
                    if ((ept - time)  > tolerance){
                        LOGGER.error(drop.request.getId() + "d("+((int) plan.size()/2) + ")  arrived " 
                            + (ept - time) + "ms too early: ept " + ept + ", time " + time);
                            badPlans.add(plan);
                           return -1;
                    }
                    if((time - lpt) > tolerance){
                        LOGGER.error( drop.request.getId() + "d(" + ((int) plan.size()/2) + ") arrived " +
                            (time - lpt) +"ms too late: lpt "+ lpt + ", time " + time);
                            
                            badPlans.add(plan);
                           return -1;
                    }
//                    if( time < lpt && time > ept){
//                           LOGGER.debug("ok. "+ ept + " < " + time+   " > " + lpt);
//                    }
                node = drop.getPosition();
            }
        }//actions

        if(lastNode.id != node.id){
            LOGGER.error("Expected end node "+lastNode.id + " != "+node.id+"; ");
            return -1;
         }
         
        int realTravelTime = time  - startTime;
        if(Math.abs(realTravelTime - planTravelTime) > tolerance){
            LOGGER.warn("Expected end time "+planTravelTime+ " != "+realTravelTime+"; exp - actual = " +(planTravelTime - realTravelTime));
            if(Math.abs(planTravelTime - realTravelTime) > tolerance){
                differentCost++;
            } 
        }
 //        LOGGER.debug("path is correct.");
        return Math.max(realTravelTime, planTravelTime);
    }
    
    public DriverPlan getPlanByIndex(int planIndex){
        return demand.get(planIndex);
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
