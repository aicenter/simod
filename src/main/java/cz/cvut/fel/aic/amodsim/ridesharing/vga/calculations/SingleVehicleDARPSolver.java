/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import java.util.LinkedHashSet;

/**
 *
 * @author LocalAdmin
 * @param <V>
 */
public abstract class SingleVehicleDARPSolver<V extends IOptimalPlanVehicle> {
	
	protected final StandardPlanCostProvider planCostComputation;
	
	private final boolean recordTime;
	
	protected final TravelTimeProvider travelTimeProvider;
	
	
	public SingleVehicleDARPSolver(StandardPlanCostProvider planCostComputation, AmodsimConfig config,
			TravelTimeProvider travelTimeProvider) {
		this.planCostComputation = planCostComputation;
		this.travelTimeProvider = travelTimeProvider;
		recordTime = config.ridesharing.vga.logPlanComputationalTime;
	}
	
	public abstract Plan<V> computeOptimalVehiclePlanForGroup(V vehicle, 
			LinkedHashSet<PlanComputationRequest> requests, int startTime, boolean ignoreTime);
	
	public abstract boolean groupFeasible(LinkedHashSet<PlanComputationRequest> requests, 
			int startTime, int vehicleCapacity);
}
