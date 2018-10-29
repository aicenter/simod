package cz.cvut.fel.aic.amodsim.ridesharing.plan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class RidesharingOnDemandVehicleFactory extends OnDemandVehicleFactory{
	
	@Inject
	public RidesharingOnDemandVehicleFactory(PhysicalTransportVehicleStorage vehicleStorage, TripsUtil tripsUtil, 
			StationsDispatcher onDemandVehicleStationsCentral,
			PhysicalVehicleDriveFactory driveActivityFactory, PositionUtil positionUtil, 
			EventProcessor eventProcessor, StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, 
			AmodsimConfig config) {
		super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, 
				eventProcessor, timeProvider, rebalancingIdGenerator, config);
	}

	@Override
	public OnDemandVehicle create(String vehicleId, SimulationNode startPosition) {
		return new RideSharingOnDemandVehicle(vehicleStorage, tripsUtil, 
                onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider, 
                rebalancingIdGenerator, config, vehicleId, startPosition);
	}
	
	
	
}
