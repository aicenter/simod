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

import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStats;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public abstract class DARPSolver {
	
	protected final OnDemandVehicleStorage vehicleStorage;
	
	protected final TravelTimeProvider travelTimeProvider;
	
	protected final PlanCostProvider planCostProvider;
	
	protected final DefaultPlanComputationRequestFactory requestFactory;
	
	protected final List<RidesharingBatchStats> ridesharingStats;
	
	
	protected RidesharingDispatcher ridesharingDispatcher;

	
	
	
	public List<RidesharingBatchStats> getRidesharingStats() {
		return ridesharingStats;
	}
	
	public void setDispatcher(RidesharingDispatcher ridesharingDispatcher){
		this.ridesharingDispatcher = ridesharingDispatcher;
	}

	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider,
			PlanCostProvider travelCostProvider, DefaultPlanComputationRequestFactory requestFactory) {
		this.vehicleStorage = vehicleStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.planCostProvider = travelCostProvider;
		this.requestFactory = requestFactory;
		ridesharingStats = new ArrayList<>();
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<PlanComputationRequest> newRequests,
			List<PlanComputationRequest> waitingRequests);
}
