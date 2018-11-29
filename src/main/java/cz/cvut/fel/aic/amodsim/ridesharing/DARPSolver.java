package cz.cvut.fel.aic.amodsim.ridesharing;

import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 * @param <T>
 */
public abstract class DARPSolver<T extends TravelTimeProvider> {
	
	protected final OnDemandVehicleStorage vehicleStorage;
	protected final T travelTimeProvider;
	protected final TravelCostProvider travelCostProvider;
	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage, T travelTimeProvider,
			TravelCostProvider travelCostProvider) {
		this.vehicleStorage = vehicleStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.travelCostProvider = travelCostProvider;
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<OnDemandRequest> requests);
    public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve();
}
