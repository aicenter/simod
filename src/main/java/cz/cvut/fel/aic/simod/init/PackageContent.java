package cz.cvut.fel.aic.simod.init;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.io.TimeTrip;

// structure for content of package in Event
public class PackageContent {

	public final TimeTrip<SimulationNode> trip;

	public final int packageWeight;

	public PackageContent(TimeTrip<SimulationNode> trip, int packageWeight) {
		this.trip = trip;
		this.packageWeight = packageWeight;
	}
}
