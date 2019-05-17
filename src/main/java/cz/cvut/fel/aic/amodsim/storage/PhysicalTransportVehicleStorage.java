/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;

/**
 *
 * @author fido
 */
@Singleton
public class PhysicalTransportVehicleStorage extends EntityStorage<PhysicalTransportVehicle>{
	
	@Inject
	public PhysicalTransportVehicleStorage() {
		super();
	}
	
}
