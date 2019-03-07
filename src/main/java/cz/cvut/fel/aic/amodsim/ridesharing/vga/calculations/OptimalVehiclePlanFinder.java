/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import java.util.LinkedHashSet;
import java.util.List;

/**
 *
 * @author LocalAdmin
 * @param <V>
 */
public abstract class OptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> {
	
	protected final StandardPlanCostProvider planCostComputation;
	
	
    public OptimalVehiclePlanFinder(StandardPlanCostProvider planCostComputation) {
		this.planCostComputation = planCostComputation;
	}
	
	public abstract Plan<V> getOptimalVehiclePlanForGroup(V vehicle, LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, boolean ignoreTime);
	
	public abstract boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, int vehicleCapacity);
}
