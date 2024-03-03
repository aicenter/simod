package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.DemandAgent;

import java.util.List;

public class ReconfigurableVehicle extends PhysicalTransportVehicle<DemandAgent> {
	private final List<SlotConfiguration> validConfigurations;

	private final ConfigurationFilter configurationFilter;


	public List<SlotConfiguration> getValidConfigurations() {
		return validConfigurations;
	}

	public ReconfigurableVehicle(
		String vehicleId,
		EntityType type,
		float lengthInMeters,
		GraphType usingGraphTypeForMoving,
		SimulationNode position,
		int maxVelocity,
		List<SlotConfiguration> validConfigurations
	) {
		super(
			vehicleId, type, lengthInMeters, usingGraphTypeForMoving, position, maxVelocity
		);
		this.validConfigurations = validConfigurations;
		this.configurationFilter = new ConfigurationFilter(this);
	}


	@Override
	public boolean hasCapacityFor(DemandAgent entity) {
		return configurationFilter.hasCapacityFor(entity);
	}

	@Override
	public void runPostPickUpActions(DemandAgent entity) {
		configurationFilter.pickUp(entity);
	}

	@Override
	public void runPostDropOffActions(DemandAgent entity) {
		configurationFilter.dropOff(entity);
	}

	@Override
	public boolean canTransport(DemandAgent entity) {
		return configurationFilter.canTransport(entity);
	}
}
