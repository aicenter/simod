/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanAction;
import java.util.List;

/**
 *
 * @author LocalAdmin
 */
public abstract class OptimalVehiclePlanFinder<V extends IOptimalPlanVehicle> {
	
	protected final PlanCostComputation planCostComputation;
	
	
    public OptimalVehiclePlanFinder(PlanCostComputation planCostComputation) {
		this.planCostComputation = planCostComputation;
	}
	
	public abstract Plan<V> getOptimalVehiclePlanForGroup(V vehicle, List<VGAVehiclePlanAction> actions, 
			double startTime, boolean ignoreTime);
	
}
