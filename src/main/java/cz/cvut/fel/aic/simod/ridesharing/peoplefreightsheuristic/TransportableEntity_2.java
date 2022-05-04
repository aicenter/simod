package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleInterface;

// interface for PeopleFreightVehicle to handle Agents and Packages without difference
public interface TransportableEntity_2 extends TransportableEntity {
	void tripStarted(OnDemandVehicleInterface vehicle);

	void tripEnded();

	boolean isDropped();

	void setDropped(boolean	dropped);

	long getDemandTime();

	int getSimpleId();

	String getId();

	String toString();

	long getMinDemandServiceDuration();
}
