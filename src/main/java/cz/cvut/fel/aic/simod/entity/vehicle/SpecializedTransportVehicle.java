package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.TransportableEntityWithRequirement;

import java.util.HashMap;
import java.util.Objects;


/**
 * Specialized transport vehicle with heterogeneous slots for transportable entities
 */
public class SpecializedTransportVehicle extends PhysicalTransportVehicle<DemandAgent> {

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
    public boolean canTransport(DemandAgent entity) {
        return slots.containsKey(entity.getRequiredSlotType());
    }

    @Override
    public boolean hasCapacityFor(DemandAgent entity) {
        return slots.get(entity.getRequiredSlotType()) > 0;
    }

    @Override
    public void runPostPickUpActions(DemandAgent entity) {
        slots.put(entity.getRequiredSlotType(), slots.get(entity.getRequiredSlotType()) - 1);
    }

    @Override
    public void runPostDropOffActions(DemandAgent entity) {
        slots.put(entity.getRequiredSlotType(), slots.get(entity.getRequiredSlotType()) + 1);
    }

    public int getCapacityFor(SlotType requiredSlotType) {
        if(slots.containsKey(requiredSlotType)){
            return slots.get(requiredSlotType);
        }
        return 0;
    }
}
