package cz.cvut.fel.aic.simod.entity;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;

public interface TransportableEntityManagement<T extends TransportableEntity> {
	boolean isDropped();

	long getDemandTime();

	void tripStarted(OnDemandVehicle vehicle);

	void tripEnded();

	int getSimpleId();

	long getMinDemandServiceDuration();
}
