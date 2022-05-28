package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.entity.DemandEntityType;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.statistics.PickupEventContent;

public class PeopleFreightVehicleEventContent extends PickupEventContent {

	private final DemandEntityType type;

	public PeopleFreightVehicleEventContent(long time, int demandId, String onDemandVehicleId, int demandTripLength, DemandEntityType type) {
		super(time, demandId, onDemandVehicleId, demandTripLength);
		this.type = type;
	}

	public DemandEntityType getType() {
		return type;
	}
}
