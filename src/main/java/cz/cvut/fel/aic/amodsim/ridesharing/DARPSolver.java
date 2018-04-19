package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.entity.vehicle.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public abstract class DARPSolver {
	
	protected final OnDemandVehicleStorage vehicleStorage;

	
	
	
	
	public DARPSolver(OnDemandVehicleStorage vehicleStorage) {
		this.vehicleStorage = vehicleStorage;
	}
	
	
	
	
	public abstract Map<RideSharingOnDemandVehicle,DriverPlan> solve(List<OnDemandRequest> requests, 
			TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider);
}
