package cz.cvut.fel.aic.amodsim.ridesharing;

import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
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
	protected final TravelCostProvider travelCostProvider;
	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider,
			TravelCostProvider travelCostProvider) {
		this.vehicleStorage = vehicleStorage;
		this.travelTimeProvider = travelTimeProvider;
		this.travelCostProvider = travelCostProvider;
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<OnDemandRequest> requests);
    public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve();
}
