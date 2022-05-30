package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;

public interface PeopleFreightVehicleFactorySpec extends OnDemandVehicleFactorySpec {
	public OnDemandVehicle create(String vehicleId, SimulationNode startPosition, int packagesCapacity);
}
