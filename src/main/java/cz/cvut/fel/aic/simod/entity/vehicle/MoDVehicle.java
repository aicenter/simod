package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;

public abstract class MoDVehicle extends PhysicalTransportVehicle<DemandAgent> {

	public MoDVehicle(
		String vehicleId,
		EntityType type,
		float lengthInMeters,
		GraphType usingGraphTypeForMoving,
		SimulationNode position,
		int maxVelocity
	) {
		super(vehicleId, type, lengthInMeters, usingGraphTypeForMoving, position, maxVelocity);
	}

	public abstract boolean hasCapacityFor(PlanComputationRequest request);
}
