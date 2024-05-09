/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.simod.ridesharing.vga.planBuilder.VGAVehiclePlan;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class StandardPlanCostProvider implements PlanCostProvider{
	
	
	private final double weight_parameter;

	@Inject
	public StandardPlanCostProvider(SimodConfig config) {
		weight_parameter = config.ridesharing.weightParameter;
	}
	
	public double calculatePlanCost(VGAVehiclePlan plan) {
		return MathUtils.round(weight_parameter * plan.getDiscomfort() 
					+ (1 - weight_parameter) * plan.getCurrentTime(), 8);
	}
	
	@Override
	public double calculatePlanCost(int discomfort, int duration) {
		return (weight_parameter * discomfort + (1 - weight_parameter) * duration);
	}
	
}
