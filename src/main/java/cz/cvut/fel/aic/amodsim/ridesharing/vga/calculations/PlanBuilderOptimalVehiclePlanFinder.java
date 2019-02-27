/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanPickup;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author F.I.D.O.
 * @param <V>
 */
@Singleton
public class PlanBuilderOptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> extends OptimalVehiclePlanFinder<V>{

	@Inject
	public PlanBuilderOptimalVehiclePlanFinder(PlanCostComputation planCostComputation) {
		super(planCostComputation);
	}

	
	
	@Override
	public Plan<V> getOptimalVehiclePlanForGroup(V vehicle, LinkedHashSet<PlanComputationRequest> requests, int startTime,
			boolean ignoreTime) {
		Stack<VGAVehiclePlan> toCheck = new Stack<>();
		
		// group reconstruction
		Set<PlanComputationRequest> group = new LinkedHashSet<>();
		for (PlanComputationRequest request : requests) {
			group.add(request);
		}
		
		VGAVehiclePlan emptyPlan = new VGAVehiclePlan(vehicle, group);
        toCheck.push(emptyPlan);

        double upperBound = Double.POSITIVE_INFINITY;
        VGAVehiclePlan bestPlan = null;

		/* In each iteration, we try to add all reamining actions to the plan, one by one. After addition, there 
		are feasibility tests. If the tests are OK, the new plan is added to queue for check. */
        while(!toCheck.empty()){
			
            VGAVehiclePlan plan = toCheck.pop();
			
			// dropoff actions
            for(PlanComputationRequest request : plan.getOnboardRequests()){

                VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);
                simplerPlan.add(request.getDropOffAction());

                if(request.getMaxDropoffTime() > simplerPlan.getCurrentTime() || ignoreTime) {
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
				for (PlanComputationRequest request : plan.getWaitingRequests()) {

					VGAVehiclePlan simplerPlan = new VGAVehiclePlan(plan);

					// pick up time == demand time
					simplerPlan.add(request.getPickUpAction());

					if(request.getMaxPickupTime() > simplerPlan.getCurrentTime()) {
						toCheck.push(simplerPlan);
					}
				}
			}
        }
		
		if(bestPlan == null){
			return null;
		}
		
        // convert to Plan
		List<VGAVehiclePlanAction> bestPlanActions = bestPlan.getActions();
		
        return new Plan((int) startTime, (int) bestPlan.getCurrentTime(), (int) upperBound, bestPlanActions, vehicle);
	}

    @Override
    public boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, int startTime, int vehicleCapacity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
