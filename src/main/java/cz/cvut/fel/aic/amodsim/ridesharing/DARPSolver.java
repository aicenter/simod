package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
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

	public List<RidesharingBatchStats> getRidesharingStats() {
		return ridesharingStats;
	}
	
	

	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider,
			PlanCostProvider travelCostProvider, DefaultPlanComputationRequestFactory requestFactory) {
		this.vehicleStorage = vehicleStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.planCostProvider = travelCostProvider;
		this.requestFactory = requestFactory;
		ridesharingStats = new ArrayList<>();
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<OnDemandRequest> requests);
}
