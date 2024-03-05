package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;

public class SimpleMoDVehicle extends MoDVehicle{

	private final int vehiclePassengerCapacity; // number of passenger, including driver



	public int getCapacity() {
		return vehiclePassengerCapacity;
	}

	public SimpleMoDVehicle(
		String vehicleId,
		EntityType type,
		float lengthInMeters,
		GraphType usingGraphTypeForMoving,
		SimulationNode position,
		int maxVelocity,
		int vehiclePassengerCapacity
	) {
		super(vehicleId, type, lengthInMeters, usingGraphTypeForMoving, position, maxVelocity);
		this.vehiclePassengerCapacity = vehiclePassengerCapacity;
	}

	@Override
	public boolean hasCapacityFor(DemandAgent entity) {
		return getCapacity() > getTransportedEntities().size();
	}

	@Override
	public void runPostPickUpActions(DemandAgent entity) {

	}

	@Override
	public void runPostDropOffActions(DemandAgent entity) {

	}

	@Override
	public boolean canTransport(DemandAgent entity) {
		return true;
	}

	@Override
	public boolean hasCapacityFor(PlanComputationRequest request) {
		return getCapacity() > getTransportedEntities().size();
	}

	public int getFreeCapacity() {
		return getCapacity() - getTransportedEntities().size();
	}
}
