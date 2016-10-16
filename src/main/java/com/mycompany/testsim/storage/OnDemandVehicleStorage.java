package com.mycompany.testsim.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.OnDemandVehicle;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import java.util.HashMap;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class OnDemandVehicleStorage extends EntityStorage<OnDemandVehicle>{
	
	@Inject
	public OnDemandVehicleStorage() {
		super(new HashMap<>(), new HashMap<>());
	}
	
}
