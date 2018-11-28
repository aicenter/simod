/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class GroupPlan {
	final Set<Request> requests;
	
	private final Plan plan;

	GroupPlan(Set<Request> requests) {
		this(requests, null);
	}

	GroupPlan(Set<Request> requests, Plan plan) {
		this.requests = requests;
		this.plan = plan;
	}
	
//	long getStartTime(){
//		return plan.startTime;
//	}
//	
//	long getEndTime(){
//		return plan.endTime;
//	}
	
	boolean overlaps(Request request){
		return request.time < plan.endTime && request.maxDropOffTime > plan.startTime;
	}
	
			
}
