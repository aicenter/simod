/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.TravelTimeProviderTaxify;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class PlanBuilder {

	
	private final LinkedList<Action> actions;
	private final LinkedHashSet<Request> waitingRequests;
	private final LinkedHashSet<Request> onboardRequests;
	private final LinkedHashSet<Request> requests;
	
	private int discomfort;
	
	private long endTime;
	
	private long startTime;

	
	
	
	public LinkedHashSet<Request> getWaitingRequests() {
		return waitingRequests;
	}

	public LinkedHashSet<Request> getOnboardRequests() {
		return onboardRequests;
	}

	public long getEndTime() {
		return endTime;
	}
	
	
	
	
	
	
	/**
	 * Empty plan constructor
	 * @param group 
	 */
	public PlanBuilder(Set<Request> group) {
		this.discomfort = 0;
        this.actions = new LinkedList<>();
        this.requests = new LinkedHashSet<>(group);
        this.waitingRequests = new LinkedHashSet<>();
		this.onboardRequests = new LinkedHashSet<>();
		
		for(Request request: requests){
			waitingRequests.add(request);
		}
	}
	
	/**
	 * Copy constructor
	 * @param planBuilder 
	 */
	public PlanBuilder(PlanBuilder planBuilder){
        this.discomfort = planBuilder.discomfort;
		this.startTime = planBuilder.startTime;
		this.endTime = planBuilder.endTime;
        this.actions = new LinkedList<>(planBuilder.actions);
        this.requests = new LinkedHashSet<>(planBuilder.requests);
        this.onboardRequests = new LinkedHashSet<>(planBuilder.onboardRequests);
        this.waitingRequests = new LinkedHashSet<>(planBuilder.waitingRequests);
    }
	
	
	
	public void add(Action action, TravelTimeProviderTaxify travelTimeProvider) {
        recomputeTime(action, travelTimeProvider);
		actions.add(action);
		Request request = action.request;
        if(action instanceof PickUpAction){
            waitingRequests.remove(request);
            onboardRequests.add(request);
        } 
		else if (action instanceof DropOffAction) {
			onboardRequests.remove(request);
			
			long minTravelTime = request.minTravelTime;
			long realTime = endTime - request.time;
			
            discomfort += realTime - minTravelTime;
        }
    }
	
	// TODO update
	double getPlanCost(){
		return discomfort;
	}

	boolean vehicleHasFreeCapacity(int maxCapacity) {
		return onboardRequests.size() < maxCapacity;
	}
	
	Plan getPlan(){
		return new Plan(startTime, endTime);
	}
	
	private void recomputeTime(Action action, TravelTimeProvider travelTimeProvider) {
		if(actions.isEmpty()){
			startTime = action.request.time;
			endTime = startTime;
		}
		else{
			int fromId = actions.getLast().getPossitionId();
			int toId = action.getPossitionId();
			long travelTime = (long) travelTimeProvider.getTravelTimeInMillis(fromId, toId);
			endTime += travelTime;
		}
	}

	boolean reaminingRequestsFeasibile() {
		for (Request onBoardRequest : onboardRequests) {
			if(onBoardRequest.maxDropOffTime < endTime){
				return false;
			}
		}
		
		for(Request waitingRequest: waitingRequests){
			if(waitingRequest.maxPickUpTime < endTime){
				return false;
			}
		}
		
		return true;
	}

	

}
