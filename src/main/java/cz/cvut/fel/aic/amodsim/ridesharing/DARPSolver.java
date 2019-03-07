package cz.cvut.fel.aic.amodsim.ridesharing;

import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
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

	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider,
			PlanCostProvider travelCostProvider, DefaultPlanComputationRequestFactory requestFactory) {
		this.vehicleStorage = vehicleStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.planCostProvider = travelCostProvider;
		this.requestFactory = requestFactory;
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<OnDemandRequest> requests);
}
