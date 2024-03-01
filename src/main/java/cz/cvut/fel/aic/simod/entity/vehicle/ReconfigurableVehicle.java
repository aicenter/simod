package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

import java.util.List;

public class ReconfigurableVehicle<T extends TransportableEntity> extends PhysicalTransportVehicle<T> {
	private final List<SlotConfiguration> validConfigurations;

	private final ConfigurationFilter configurationFilter;



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
		this.configurationFilter = new ConfigurationFilter(validConfigurations);
	}


	@Override
	public boolean hasCapacityFor(T entity) {
		return configurationFilter.hasCapacityFor(entity);
	}

	@Override
	public void runPostPickUpActions(T entity) {
		configurationFilter.pickUp(entity);
	}

	@Override
	public void runPostDropOffActions(T entity) {
		configurationFilter.dropOff(entity);
	}

	@Override
	public boolean canTransport(T entity) {
		return configurationFilter.canTransport(entity);
	}
}
