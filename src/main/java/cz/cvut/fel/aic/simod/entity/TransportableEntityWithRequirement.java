package cz.cvut.fel.aic.simod.entity;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;

/**
 * Transportable entity interface with requirement for a specific slot type
 */
public interface TransportableEntityWithRequirement extends TransportableEntity {
    SlotType getRequiredSlotType();
}
