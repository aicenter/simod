package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.GroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.slf4j.LoggerFactory;



@Singleton
public class VGAGroupGenerator {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupGenerator.class);
	
	
	
	
	private final double maximumRelativeDiscomfort;
	
	private final PlanCostComputation planCostComputation;

	@Inject
    private VGAGroupGenerator(AmodsimConfig amodsimConfig, PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
		maximumRelativeDiscomfort = amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort;
	}

    public List<VGAVehiclePlan> generateGroupsForVehicle(VGAVehicle vehicle, LinkedHashSet<VGARequest> requests) {
		// F_v^{k - 1} - groupes for request adding
		List<Set<VGARequest>> currentGroups = new ArrayList<>();
		
		// F_v^{1}
        List<VGARequest> feasibleRequests = new ArrayList<>();
		
		// F_v all groups feasible for vehicle with optimal plan already assigned to them - the output
        List<VGAVehiclePlan> groups = new ArrayList<>();

		
		if(vehicle.getRequestsOnBoard().isEmpty()){
			// BASE PLAN - for each empty vehicle, an EMPTY PLAN is valid
			groups.add(new VGAVehiclePlan(vehicle, new LinkedHashSet<>()));
		}
		else{
			// BASE PLAN - for non-empty vehicles, we add a base plan that serves all onboard vehicles
			Set<VGARequest> group = vehicle.getRequestsOnBoard();
			
			// currently, the time window has to be ignored, because the planner underestimates the cost
			VGAVehiclePlan plan = getOptimalPlan(vehicle, group, true);
			groups.add(plan);
			
			// onboard request composes the single base group
			currentGroups.add(group);
		}
		
		// groups of size 1
		for (VGARequest r : requests) {
			Set<VGARequest> group = new HashSet<>();
			group.add(r);

			VGAVehiclePlan plan;
			if((plan = getOptimalPlan(vehicle, group, false)) != null) {
				feasibleRequests.add(r);
				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
				if(vehicle.getRequestsOnBoard().isEmpty()){
					currentGroups.add(group);
					groups.add(plan);
				}			
			}
		}
		
		
		// generate other groups
		int currentGroupSize = 1;
        while(!currentGroups.isEmpty()) {

			// current groups for the next iteration
            List<Set<VGARequest>> newCurrentGroups = new ArrayList<>();
			
			// set of groups that were already checked
			Set<Set<VGARequest>> currentCheckedGroups = new LinkedHashSet<>();

            for (Set<VGARequest> group : currentGroups) {
                for (VGARequest request : feasibleRequests) {
                    if (group.contains(request)){
						continue;
					}
					
					// G'
                    Set<VGARequest> newGroupToCheck = new HashSet<>(group);
                    newGroupToCheck.add(request);
					
                    if (currentCheckedGroups.contains(newGroupToCheck)){
						continue;
					}
                    currentCheckedGroups.add(newGroupToCheck);

                    VGAVehiclePlan plan;
                    if((plan = getOptimalPlan(vehicle, newGroupToCheck, false)) != null) {
                        newCurrentGroups.add(newGroupToCheck);
                        groups.add(plan);
//                        if(groups.size() > 50){
//                            return groups;
//                        }
                    }
                }
            }

            currentGroups = newCurrentGroups;
			currentGroupSize++;
//			LOGGER.debug("{} groups of the size {} generated", currentGroups.size(), currentGroupSize);
			if(currentGroupSize >= 2){
				break;
			}
        }

//		LOGGER.debug("Groups generated, total number of groups is {}", groups.size());
		
        return groups;
    }

    private VGAVehiclePlan getOptimalPlan(VGAVehicle vehicle, Set<VGARequest> group, boolean ignoreTime){
        Stack<VGAVehiclePlan> toCheck = new Stack<>();
		VGAVehiclePlan emptyPlan = new VGAVehiclePlan(vehicle, group);
        toCheck.push(emptyPlan);

        double upperBound = Double.POSITIVE_INFINITY;
        VGAVehiclePlan bestPlan = null;

		/* In each iteration, we try to add all reamining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
        while(!toCheck.empty()){
			
            VGAVehiclePlan plan = toCheck.pop();
			
			// dropoff actions
            for(VGARequest request : plan.getOnboardRequests()){

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(new VGAVehiclePlanDropoff(request, simplerPlan));

                if(request.maxDropOffTime > simplerPlan.getCurrentTime() || ignoreTime) {
                    double currentCost = planCostComputation.calculatePlanCost(simplerPlan);
                    if (currentCost < upperBound) {
                        if (simplerPlan.getWaitingRequests().isEmpty() && simplerPlan.getOnboardRequests().isEmpty()) {
                            upperBound = currentCost;
                            bestPlan = simplerPlan;
                        } else {
                            toCheck.push(simplerPlan);
                        }
                    }
                }
            }

			// pickup actions
			if(plan.vehicleHasFreeCapacity()){
				for (VGARequest request : plan.getWaitingRequests()) {

					VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);

					// pick up time == demand time
					simplerPlan.add(new VGAVehiclePlanPickup(request, simplerPlan));

					if(request.maxPickUpTime > simplerPlan.getCurrentTime()) {
						toCheck.push(simplerPlan);
					}
				}
			}
        }

        return bestPlan;
    }

}
