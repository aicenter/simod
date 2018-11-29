/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.SolverTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.TravelTimeProviderTaxify;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.slf4j.LoggerFactory;

/**
 * This class should generate all possible ridesharing groups. The groups here are not related to any vehicle. 
 * Input should be just the requests. Output is a set of valid groups with an optimal plan assigned to serve the group.
 * 
 * @author F.I.D.O.
 */
@Singleton
public class GroupGenerator {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);
	
	
	
	
	private final TravelTimeProviderTaxify travelTimeProvider;
	
	private final AmodsimConfig config;

	
	
	
	@Inject
	public GroupGenerator(TravelTimeProviderTaxify travelTimeProvider, AmodsimConfig config) {
		this.travelTimeProvider = travelTimeProvider;
		this.config = config;
	}
	
	
	
	public Set<GroupPlan> generateGroups(List<Request> requests) {
		
		// F_v^{k - 1} - groupes for request adding
		Set<GroupPlan> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
        Set<Request> feasibleRequests = new LinkedHashSet<>();
		
		// F_v all feasible groups with optimal plan already assigned to them - the output
        Set<GroupPlan> groupsPlans = new LinkedHashSet<>();

		// BASE PLAN - an EMPTY PLAN is always valid
		groupsPlans.add(new GroupPlan(new LinkedHashSet<>()));

		// groups of size 1 - allways valid
		LOGGER.info("Generating groups of size 1");
		for (Request request : requests) {
			feasibleRequests.add(request);	
			
			Set<Request> group = new LinkedHashSet<>();
			group.add(request);
			Plan plan = getOptimalPlan(group);
			GroupPlan groupPlan = new GroupPlan(group, plan);
			currentGroups.add(groupPlan);
			groupsPlans.add(groupPlan);
		}
		
		// generate other groups
		int currentGroupSize = 1;
        while(!currentGroups.isEmpty()) {
			
			LOGGER.info("Generating groups of size {}", currentGroupSize + 1);
			
			Set<Request> feasibleRequestsForIteration;
			if(currentGroupSize == 1){
				feasibleRequestsForIteration = new LinkedHashSet<>(feasibleRequests);
			}
			else{
				feasibleRequestsForIteration = feasibleRequests;
			}

			// current groups for the next iteration
            Set<GroupPlan> newCurrentGroups = new LinkedHashSet<>();
			
			// set of groups that were already checked
			Set<Set<Request>> currentCheckedGroups = new LinkedHashSet<>();

            for (GroupPlan groupPlan : currentGroups) {
                for (Request request : feasibleRequestsForIteration) {
                    if (groupPlan.requests.contains(request) || !groupPlan.overlaps(request)){
						continue;
					}
					
					// G'
                    Set<Request> newGroupToCheck = new LinkedHashSet<>(groupPlan.requests);
                    newGroupToCheck.add(request);
					
					if(newGroupToCheck.size() > 2){
						if (currentCheckedGroups.contains(newGroupToCheck)){
							continue;
						}
						currentCheckedGroups.add(newGroupToCheck);
					}

                    Plan plan;
                    if((plan = getOptimalPlan(newGroupToCheck)) != null) {
						GroupPlan newGroupPlan = new GroupPlan(newGroupToCheck, plan);
                        newCurrentGroups.add(newGroupPlan);
                        groupsPlans.add(newGroupPlan);
                    }
                }
				
				if(currentGroupSize == 1){
					feasibleRequestsForIteration.remove(groupPlan.requests.iterator().next());
				}
            }

            currentGroups = newCurrentGroups;		
			currentGroupSize++;
			
			if(currentGroupSize >= 2){
				break;
			}
        }
		
		LOGGER.info("{} group plans generated.", groupsPlans.size());

        return groupsPlans;
    }
	
	private Plan getOptimalPlan(Set<Request> group){
        Stack<PlanBuilder> toCheck = new Stack<>();
		PlanBuilder emptyPlan = new PlanBuilder(group);
        toCheck.push(emptyPlan);

        double upperBound = Double.POSITIVE_INFINITY;
        PlanBuilder bestPlan = null;

		/* In each iteration, we try to add all remaining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
        while(!toCheck.empty()){
			
            PlanBuilder plan = toCheck.pop();
			
			// dropoff actions
            for(Request request : plan.getOnboardRequests()){

                PlanBuilder longerPlan = new PlanBuilder(plan);
                longerPlan.add(new DropOffAction(request), travelTimeProvider);

                if(request.maxDropOffTime >= longerPlan.getEndTime() && longerPlan.reaminingRequestsFeasibile()) {
                    double currentCost = longerPlan.getPlanCost();
                    if (currentCost < upperBound) {
                        if (longerPlan.getWaitingRequests().isEmpty() && longerPlan.getOnboardRequests().isEmpty()) {
                            upperBound = currentCost;
                            bestPlan = longerPlan;
                        } else {
                            toCheck.push(longerPlan);
                        }
                    }
                }
            }

			// pickup actions
			if(plan.vehicleHasFreeCapacity(config.amodsim.ridesharing.vehicleCapacity)){
				for (Request request : plan.getWaitingRequests()) {

					PlanBuilder longerPlan = new PlanBuilder(plan);

					// pick up time == demand time
					longerPlan.add(new PickUpAction(request), travelTimeProvider);

					if(longerPlan.getEndTime() >= request.time && longerPlan.getEndTime() <= request.maxPickUpTime
							&&  longerPlan.reaminingRequestsFeasibile()) {
						toCheck.push(longerPlan);
					}
				}
			}
        }

        if(bestPlan != null){
			return bestPlan.getPlan();
		}
		
		return null;
    }
}
