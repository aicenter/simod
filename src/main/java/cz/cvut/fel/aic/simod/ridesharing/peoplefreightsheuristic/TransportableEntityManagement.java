package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleInterface;

// interface for PeopleFreightVehicle to handle Agents and Packages without difference
public interface TransportableEntityManagement extends TransportableEntity {
	void tripStarted(OnDemandVehicleInterface vehicle);

	void tripEnded();

	boolean isDropped();

	long getDemandTime();

	int getSimpleId();

	long getMinDemandServiceDuration();
}
