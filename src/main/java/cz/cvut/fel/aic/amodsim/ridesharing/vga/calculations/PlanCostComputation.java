/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlan;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class PlanCostComputation {
		
    public static VGAVehiclePlan.CostType COST_TYPE = VGAVehiclePlan.CostType.STANDARD;
	
	
	private final double weight_parameter;

	@Inject
	public PlanCostComputation(AmodsimConfig config) {
		weight_parameter = config.amodsim.ridesharing.vga.weightParameter;
	}
	
	public double calculatePlanCost(VGAVehiclePlan plan) {
        if(COST_TYPE == VGAVehiclePlan.CostType.STANDARD) {
            return MathUtils.round(weight_parameter * plan.getDiscomfort() 
					+ (1 - weight_parameter) * plan.getCurrentTime(), 8);
        } 
//		else if (COST_TYPE == VGAVehiclePlan.CostType.SUM_OF_DROPOFF_TIMES) {
//            return MathUtils.round(plan.getDropoffTimeSum(), 8);
//        }
        return -1;
    }
	
	public int calculatePlanCost(int discomfort, int duration) {
        return (int) (weight_parameter * discomfort + (1 - weight_parameter) * duration);
    }
	
}
