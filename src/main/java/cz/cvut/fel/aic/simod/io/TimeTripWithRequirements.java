package cz.cvut.fel.aic.simod.io;

import cz.cvut.fel.aic.geographtools.WKTPrintableCoord;
import cz.cvut.fel.aic.simod.entity.SlotType;

public class TimeTripWithRequirements<L extends WKTPrintableCoord> extends TimeTrip<L> {

	private final SlotType requiredSlotType;

	public SlotType getRequiredSlotType() {
		return requiredSlotType;
	}

	public TimeTripWithRequirements(int tripId, long startTime, SlotType requiredSlotType, L... locations) {
		super(tripId, startTime, locations);
		this.requiredSlotType = requiredSlotType;
	}


}
