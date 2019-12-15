/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
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
