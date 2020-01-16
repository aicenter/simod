/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.entities;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class IHPlan {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IHPlan.class);
    
    private static int count = 0;
   
    
    /**
     * Unique id of the car.
     */
    public final int id;
    
    private final List<Integer> actions;
    
    public final List<Integer> times;
    
    private final List<Integer> graphNodes;

    
    /**
     * Keeps track of it's route.
     * tripNodes are indices of demand entries in Demand 
     * in the order they were visited by the car.
     *  for nth demand node in demandNodes
     *  times[n] is actual start time
     */

    public IHPlan(){
        id = count++;
        actions = new LinkedList<>();
        times = new LinkedList<>();
        graphNodes = new LinkedList<>();
    }
    
    public IHPlan(int id){
        this.id = id;
        actions = new LinkedList<>();
        times = new LinkedList<>();
        graphNodes = new LinkedList<>();
    }
    
    public IHPlan(int id, IHPlan plan){
        this.id = id;
        this.actions = plan.actions;
        this.times = plan.times;
        this.graphNodes = plan.graphNodes;
    }

    public void addAction(int tripInd, int time, int nodeId){
        actions.add(tripInd);
        times.add(time);
        graphNodes.add(nodeId);
    }
    public List<Integer> getActions() {
        return actions;
    }
    
    public int getPreviousActionTime(int ind){
        if(ind > 0){
            return times.get(ind-1);
        } else{
            return 0;
        }
        
    }
   
    
    public int getId() {
        return id;
    }
    
    /**
     * @return ind of last node.
     */
    public int getLastAction(){
        return actions.get(actions.size() -1);
    }
    /**
     * @return ind of last node.
     */
    public int getFirstAction(){
        return actions.get(0);
    }
    /**
     * @return departure time from the last node.
     */
    public int getLastActionTime(){
        return times.get(actions.size() - 1);
    }

    /**
     * @return car's arrival to the first node in the path.
     */
    public int getFirstActionTime(){
        return times.get(0);
    }
    
    public int getLastActionLocation(){
        return graphNodes.get(actions.size()-1);
    }

    /**
     * @return total number of visited nodes.
     */
    public int getSize() {
        return actions.size();
    }
    
    public int getRequestsNum(){
        return actions.size()/2;
    }
    
    public int getPlanCost(){
        return getLastActionTime() - getFirstActionTime();
    }
    
    
    public void addFirstTrip(int tripNode, int startTime, int bestTime){
        actions.add(tripNode);
        times.add(startTime);
        actions.add(-tripNode - 1);
        times.add(startTime + bestTime);
     }
    
    public String printPlan(){
        String p = "Plan "+id +", cost "+getPlanCost()+", size "+getSize() + "\n";
        for(int i=0; i < getSize(); i++){
            int action = actions.get(i);
            p += ("("+action+", "+times.get(i) + "), ");
            
        }
        p +="\n";
        return p;
    }

}

