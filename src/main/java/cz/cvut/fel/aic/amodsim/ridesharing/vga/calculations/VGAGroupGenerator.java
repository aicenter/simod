package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;



@Singleton
public class VGAGroupGenerator {
	
	private final double maximumRelativeDiscomfort;

	@Inject
    private VGAGroupGenerator(AmodsimConfig amodsimConfig) {
		maximumRelativeDiscomfort = amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort;
	}

    public Set<VGAVehiclePlan> generateGroupsForVehicle(VGAVehicle vehicle, List<VGARequest> requests, 
			int noOfVehicles) {
		// F_v^{k - 1} - groupes for request adding
		Set<Set<VGARequest>> currentGroups = new LinkedHashSet<>();
		
		// F_v^{1}
        Set<VGARequest> feasibleRequests = new LinkedHashSet<>();
		
		// F_v all groups feasible for vehicle with optimal plan alreadz assigne to them - the output
        Set<VGAVehiclePlan> groups = new LinkedHashSet<>();

		
		if(vehicle.getRequestsOnBoard().isEmpty()){
			// BASE PLAN - for each empty vehicle, an empty plan is valid
			groups.add(new VGAVehiclePlan(vehicle.getRidesharingVehicle(), new LinkedHashSet<>()));
		}
		else{
			// BASE PLAN - for nonempty vehicles, we add a base plan that serves all vehicles
			Set<VGARequest> group = vehicle.getRequestsOnBoard();
			VGAVehiclePlan plan = getOptimalPlan(vehicle, group);
			groups.add(plan);
			
			// onboard request composes the single base group
			currentGroups.add(group);
		}
		
		for (VGARequest r : requests) {
			Set<VGARequest> group = new LinkedHashSet<>();
			group.add(r);

			VGAVehiclePlan plan;
			if((plan = getOptimalPlan(vehicle, group)) != null) {
				feasibleRequests.add(r);
				//if the vehicle is empty, feasible requests are feasible plans and are used as base groups
				if(vehicle.getRequestsOnBoard().isEmpty()){
					currentGroups.add(group);
					groups.add(plan);
				}			
			}
		}
		
		
		// generate other groups
        while(!currentGroups.isEmpty()) {

			// current groups for the next iteration
            Set<Set<VGARequest>> newCurrentGroups = new LinkedHashSet<>();
			
			// set of groups that were already checked
			Set<Set<VGARequest>> currentCheckedGroups = new LinkedHashSet<>();

            for (Set<VGARequest> group : currentGroups) {
                for (VGARequest request : feasibleRequests) {
                    if (group.contains(request)){
						continue;
					}
					
					// G'
                    Set<VGARequest> newGroupToCheck = new LinkedHashSet<>(group);
                    newGroupToCheck.add(request);
					
                    if (currentCheckedGroups.contains(newGroupToCheck)){
						continue;
					}
                    currentCheckedGroups.add(newGroupToCheck);

//                    boolean add = true;
//                    for(VGARequest rq : newGroupToCheck) {
//                        Set<VGARequest> toCheckCase = new LinkedHashSet<>(newGroupToCheck);
//                        toCheckCase.remove(rq);
//                        if (!currentGroups.contains(toCheckCase)) {
//                            add = false;
//                            break;
//                        }
//                    }

                    VGAVehiclePlan plan;
                    if((plan = getOptimalPlan(vehicle, newGroupToCheck)) != null) {
                        newCurrentGroups.add(newGroupToCheck);
                        groups.add(plan);
                        if(groups.size() > 150000 / noOfVehicles){
                            return groups;
                        }
                    }
                }
            }

            currentGroups = newCurrentGroups;
        }

        return groups;
    }

    public static Set<VGAVehiclePlan> generateDroppingVehiclePlans(VGAVehicle v, Set<VGARequest> requests) {
        Set<VGAVehiclePlan> droppingPlans = new LinkedHashSet<>();

        for(VGARequest r : requests) {
            Set<VGARequest> request = new LinkedHashSet<>();
            request.add(r);
            VGAVehiclePlan plan = new VGAVehiclePlan(v.getRidesharingVehicle(), request);
            plan.add(new VGAVehiclePlanRequestDrop(r, plan));
            droppingPlans.add(plan);
        }

        return droppingPlans;
    }

    private VGAVehiclePlan getOptimalPlan(VGAVehicle vehicle, Set<VGARequest> group){
//		// check if all onboard requests are in the group
//		for(VGARequest onboardRequest: vehicle.getRequestsOnBoard()){
//			if(!group.contains(onboardRequest)){
//				return null;
//			}
//		}
		
        Stack<VGAVehiclePlan> toCheck = new Stack<>();
		VGAVehiclePlan emptyPlan = new VGAVehiclePlan(vehicle.getRidesharingVehicle(), group);
        toCheck.push(emptyPlan);

        double upperBound = Double.POSITIVE_INFINITY;
        VGAVehiclePlan bestPlan = null;

		/* In each iteration, we try to add all reamining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
        while(!toCheck.empty()){
			
            VGAVehiclePlan plan = toCheck.pop();
			
			// dropoff actions
            for(VGARequest r : plan.getOnboardRequests()){

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(new VGAVehiclePlanDropoff(r, simplerPlan));

                if(r.getDestination().getWindow().isInWindow(simplerPlan.getCurrentTime())) {
                    double currentCost = simplerPlan.calculateCost();
                    if ((simplerPlan.getCurrentTime() - r.getOriginTime()) <= maximumRelativeDiscomfort *
                            MathUtils.getTravelTimeProvider().getTravelTime(vehicle.getRidesharingVehicle(), r.getOriginSimulationNode(), r.getDestinationSimulationNode()) / 1000.0 + 0.001
                            && currentCost < upperBound) {
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
				for (VGARequest r : plan.getWaitingRequests()) {

					VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);

					// pick up time == demand time
					simplerPlan.add(new VGAVehiclePlanPickup(r, simplerPlan));

					if(r.getOrigin().getWindow().isInWindow(simplerPlan.getCurrentTime())) {
						toCheck.push(simplerPlan);
					}
				}
			}
        }

        return bestPlan;
    }

}
