package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.simod.entity.TransportableEntityWithRequirement;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;

import java.util.HashMap;

public class SlotConfiguration {
	private final HashMap<SlotType, Integer> slots;

	private final String id;


	public HashMap<SlotType, Integer> getSlots() {
		return slots;
	}

	public SlotConfiguration(String id, HashMap<SlotType, Integer> slots) {
		this.id = id;
		this.slots = slots;
	}

	public boolean canTransport(TransportableEntity entity) {
		if (entity instanceof TransportableEntityWithRequirement entityWithRequirement) {
			return slots.containsKey(entityWithRequirement.getRequiredSlotType());
		}
		return slots.containsKey(SlotType.STANDARD_SEAT);
	}

	public int getSlotCount(SlotType slotType) {
		return slots.get(slotType);
	}
}
