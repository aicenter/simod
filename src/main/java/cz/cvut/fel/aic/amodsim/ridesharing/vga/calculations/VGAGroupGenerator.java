package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class VGAGroupGenerator {

    private VGAGroupGenerator() {}

    public static Set<VGAVehiclePlan> generateGroupsForVehicle(VGAVehicle veh, List<VGARequest> requests, int noOfVehicles) {
        RideSharingOnDemandVehicle v = veh.getRidesharingVehicle();
        Set<VGARequest> feasibleRequests = new LinkedHashSet<>();
        Set<VGAVehiclePlan> groups = new LinkedHashSet<>();
        Set<Set<VGARequest>> currentGroups = new LinkedHashSet<>();
        Set<Set<VGARequest>> currentCheckedGroups = new LinkedHashSet<>();
        Set<Set<VGARequest>> newCurrentGroups;

        groups.add(new VGAVehiclePlan(v, new LinkedHashSet<>()));

        for (VGARequest r : requests) {

            Set<VGARequest> g = new LinkedHashSet<>();
            g.add(r);

            VGAVehiclePlan plan;
            if((plan = optimalVehiclePlanPermutations(new VGAVehiclePlan(v, g))) != null) {
                feasibleRequests.add(r);
                currentGroups.add(g);
                groups.add(plan);
            }
        }

        while (currentGroups.size() > 0) {
            currentCheckedGroups.clear();
            newCurrentGroups = new LinkedHashSet<>();

            for (Set<VGARequest> g : currentGroups) {
                for (VGARequest r : feasibleRequests) {

                    if (g.contains(r)) continue;

                    Set<VGARequest> toCheck = new LinkedHashSet<>(g);
                    toCheck.add(r);
                    if (currentCheckedGroups.contains(toCheck)) continue;
                    currentCheckedGroups.add(toCheck);

                    boolean add = true;

                    for(VGARequest rq : toCheck) {
                        Set<VGARequest> toCheckCase = new LinkedHashSet<>(toCheck);
                        toCheckCase.remove(rq);
                        if (!currentGroups.contains(toCheckCase)) {
                            add = false;
                            break;
                        }
                    }

                    VGAVehiclePlan plan;
                    if(add && (plan = optimalVehiclePlanPermutations(new VGAVehiclePlan(v, toCheck))) != null) {
                        newCurrentGroups.add(toCheck);
                        groups.add(plan);
                        if(groups.size() > 150000 / noOfVehicles){
                            return groups;
                        }
                    }
                }
            }

            currentGroups = newCurrentGroups;
        }

        Set<VGAVehiclePlan> toRemove = new LinkedHashSet<>();
        for (VGAVehiclePlan plan : groups) {
            if(!plan.getRequests().containsAll( VGAVehicle.getVGAVehicleByRidesharingOnDemandVehicle(plan.getVehicle()).getPromisedRequests() ) ||
               !plan.getRequests().containsAll( VGAVehicle.getVGAVehicleByRidesharingOnDemandVehicle(plan.getVehicle()).getRequestsOnBoard() )) {
                toRemove.add(plan);
            }
        }
        groups.removeAll(toRemove);

        return groups;
    }

    public static Set<VGAVehiclePlan> generateDroppingVehiclePlans(VGAVehicle v, List<VGARequest> requests) {
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

    private static VGAVehiclePlan optimalVehiclePlanPermutations(VGAVehiclePlan vp){
        Stack<VGAVehiclePlan> toCheck = new Stack<>();
        vp.updateRequestsBasedOnCurrentSituation();
        toCheck.push(vp);

        double upperBound = Double.POSITIVE_INFINITY;
        VGAVehiclePlan bestPlan = null;

        while(!toCheck.empty()){
            VGAVehiclePlan plan = toCheck.pop();

            for(VGARequest r : plan.getActiveRequests()){

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(new VGAVehiclePlanDropoff(r, simplerPlan));

                if(r.getDestination().getWindow().isInWindow(simplerPlan.getCurrentTime())) {
                    double currentCost = simplerPlan.calculateCost();
                    if ((simplerPlan.getCurrentTime() - r.getOriginTime()) <= MathUtils.DELTA_R_MAX *
                            MathUtils.getTravelTimeProvider().getTravelTime(VehicleGroupAssignmentSolver.getVehicle(), r.getOriginSimulationNode(), r.getDestinationSimulationNode()) / 1000.0 + 0.001
                            && currentCost < upperBound) {
                        if (simplerPlan.getWaitingRequests().isEmpty() && simplerPlan.getActiveRequests().isEmpty()) {
                            upperBound = currentCost;
                            bestPlan = simplerPlan;
                        } else {
                            toCheck.push(simplerPlan);
                        }
                    }
                }
            }

            for (VGARequest r : plan.getWaitingRequests()) {

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(new VGAVehiclePlanPickup(r, simplerPlan));

                if(r.getOrigin().getWindow().isInWindow(simplerPlan.getCurrentTime())) {
                    toCheck.push(simplerPlan);
                }
            }
        }

        return bestPlan;
    }

}
