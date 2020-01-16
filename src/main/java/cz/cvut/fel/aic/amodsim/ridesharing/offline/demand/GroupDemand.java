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
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * Container for group demand.
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
   	}


	private List<DriverPlan> prepareDemand(List<DriverPlan> demand) {
        Collections.sort(demand, (p1, p2) -> {
            Integer i1 = PlanActionPickup.class.cast(p1.get(0)).request.getOriginTime();  
            Integer i2 = PlanActionPickup.class.cast(p2.get(0)).request.getOriginTime();
            return i1.compareTo(i2);
        });
           
        for (int i = 0; i < demand.size(); i++) {
            DriverPlan plan = demand.get(i); 
            PlanActionPickup firstPickup = (PlanActionPickup) plan.get(0);
            
            SimulationNode firstNode  = firstPickup.getPosition();
            int startTime = firstPickup.request.getOriginTime()*1000;
            
            PlanActionDropoff lastDropoff = (PlanActionDropoff) plan.get(plan.size() -1);
            SimulationNode lastNode  = lastDropoff.getPosition();
            int planTravelTime = (int) plan.cost*1000;   
            
            int bestTime = checkGroupPlan(plan, startTime, firstNode, planTravelTime, lastNode);
            if(bestTime > 0){
            addGroupPlanToIndex(i, startTime, planTravelTime , firstNode.id, lastNode.id);
            }
        }
        
        LOGGER.debug(badPlans.size() +" invalid plans dropped.");
        LOGGER.debug(differentCost +" with generator cost differes from real.");
        LOGGER.debug(duplicatePickups + " duplicate pickups.");
        return demand;
  	}
    
    int tolerance = 1000;
    int differentCost = 0;
    Set<DriverPlan> badPlans = new HashSet<>();
    Set<Integer> seenTrips = new HashSet<>();
    int duplicatePickups = 0;


    private int checkGroupPlan(DriverPlan plan, int startTime, SimulationNode firstNode,
        int planTravelTime,  SimulationNode lastNode){
        int time = startTime;
        SimulationNode node = null;
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
                    if(node.id != firstNode.id){
                            LOGGER.error("Expected start node "+firstNode.id + " != "+node.id);
                            return -1;
                        }
                } else {
                    int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, pick.getPosition());
                    time += travelTime;
                    
                    int ept = pick.request.getOriginTime()*1000;
                    int lpt = pick.request.getMaxPickupTime()*1000;
                    time = time > ept ? time : ept ;
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
                }
                node = pick.getPosition();
                           
            }else if(action instanceof PlanActionDropoff){
                PlanActionDropoff drop = (PlanActionDropoff) action;
                int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, drop.getPosition());
                time += travelTime;
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
        return Math.max(realTravelTime, planTravelTime);
    }
    
    @Override
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
