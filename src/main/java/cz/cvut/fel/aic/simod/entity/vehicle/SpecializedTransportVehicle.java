package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.SlotType;
import cz.cvut.fel.aic.simod.entity.TransportableEntityWithRequirement;

import java.util.HashMap;
import java.util.Objects;


/**
 * Specialized transport vehicle with heterogeneous slots for transportable entities
 * @param <T>
 */
public class SpecializedTransportVehicle<T extends TransportableEntity> extends PhysicalTransportVehicle<T> {

    private final HashMap<SlotType, Integer> slots;


    public SpecializedTransportVehicle(
            String vehicleId,
            EntityType type,
            float lengthInMeters,
            GraphType usingGraphTypeForMoving,
            SimulationNode position,
            int maxVelocity,
            HashMap<SlotType, Integer> slots
    ) {
        super(vehicleId, type, lengthInMeters, usingGraphTypeForMoving, position, maxVelocity);
        this.slots = slots;
    }


    @Override
    public boolean canTransport(T entity) {
        if (Objects.requireNonNull(entity) instanceof TransportableEntityWithRequirement entityWithRequirement) {
            return slots.containsKey(entityWithRequirement.getRequiredSlotType());
        }
        return slots.containsKey(SlotType.STANDARD_SEAT);
    }

    @Override
    public boolean hasCapacityFor(T entity) {
        if (Objects.requireNonNull(entity) instanceof TransportableEntityWithRequirement entityWithRequirement) {
            return slots.get(entityWithRequirement.getRequiredSlotType()) > 0;
        }
        return slots.get(SlotType.STANDARD_SEAT) > 0;
    }

    @Override
    public void runPostPickUpActions(T entity) {
        if (Objects.requireNonNull(entity) instanceof TransportableEntityWithRequirement entityWithRequirement) {
            slots.put(entityWithRequirement.getRequiredSlotType(),
                    slots.get(entityWithRequirement.getRequiredSlotType()) - 1);
        } else {
            slots.put(SlotType.STANDARD_SEAT, slots.get(SlotType.STANDARD_SEAT) - 1);
        }
    }

    @Override
    public void runPostDropOffActions(T entity) {
        if (Objects.requireNonNull(entity) instanceof TransportableEntityWithRequirement entityWithRequirement) {
            slots.put(entityWithRequirement.getRequiredSlotType(),
                    slots.get(entityWithRequirement.getRequiredSlotType()) + 1);
        } else {
            slots.put(SlotType.STANDARD_SEAT, slots.get(SlotType.STANDARD_SEAT) + 1);
        }
    }
}
