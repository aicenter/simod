package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.PlanComputationRequest;

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

	public boolean canTransport(PlanComputationRequest request) {
		return slots.containsKey(request.getRequiredSlotType());
	}

	public int getSlotCount(SlotType slotType) {
		return slots.get(slotType);
	}
}
