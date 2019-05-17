/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.planBuilder.VGAVehiclePlan;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class StandardPlanCostProvider implements PlanCostProvider{
	
	
	private final double weight_parameter;

	@Inject
	public StandardPlanCostProvider(AmodsimConfig config) {
		weight_parameter = config.ridesharing.weightParameter;
	}
	
	public double calculatePlanCost(VGAVehiclePlan plan) {
		return MathUtils.round(weight_parameter * plan.getDiscomfort() 
					+ (1 - weight_parameter) * plan.getCurrentTime(), 8);
    }
	
	@Override
	public int calculatePlanCost(int discomfort, int duration) {
        return (int) (weight_parameter * discomfort + (1 - weight_parameter) * duration);
    }
	
}
