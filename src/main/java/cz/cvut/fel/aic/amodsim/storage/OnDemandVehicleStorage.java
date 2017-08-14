package cz.cvut.fel.aic.amodsim.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import java.util.HashMap;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class OnDemandVehicleStorage extends EntityStorage<OnDemandVehicle>{
	
	@Inject
	public OnDemandVehicleStorage() {
		super();
	}
	
}