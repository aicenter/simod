package cz.cvut.fel.aic.simod.entity.vehicle;


import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.TransportableEntityWithRequirement;

/**
 * Type of the slot for heterogeneous vehicles
 */
public enum SlotType {
    STANDARD_SEAT,
    WHEELCHAIR,
    ELECTRIC_WHEELCHAIR,
    SPECIAL_NEEDS_STROLLER;

    public static SlotType getRequiredSlotType(DemandAgent entity) {
        return entity.getRequiredSlotType();
    }
}


