package cz.cvut.fel.aic.simod.init;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.simod.entity.DemandPackage;
import cz.cvut.fel.aic.simod.io.TimeTrip;

public class DemandPackageEventHandler extends EventHandlerAdapter {

	private final IdGenerator demandIdGenerator;

	private final DemandPackage.DemandPackageFactory demandPackageFactory;


	@Inject
	public DemandPackageEventHandler(IdGenerator demandIdGenerator, DemandPackage.DemandPackageFactory demandPackageFactory) {
		this.demandIdGenerator = demandIdGenerator;
		this.demandPackageFactory = demandPackageFactory;
	}


	@Override
	public void handleEvent(Event event) {
		int id = demandIdGenerator.getId();

		TimeTrip<SimulationNode> trip = ((PackageContent) event.getContent()).trip;

		int weight = ((PackageContent) event.getContent()).packageWeight;

		DemandPackage demandPackage = demandPackageFactory.create("Package " + Integer.toString(id), id, trip, weight);
		demandPackage.create();
	}
}
