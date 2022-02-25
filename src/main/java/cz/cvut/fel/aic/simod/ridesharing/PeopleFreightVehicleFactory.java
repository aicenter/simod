package cz.cvut.fel.aic.simod.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PeopleFreightVehicle;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PhysicalPFVehicle;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

@Singleton
public class PeopleFreightVehicleFactory {

	private static final int LENGTH = 4;

	protected final TripsUtil tripsUtil;

	protected final StationsDispatcher onDemandVehicleStationsCentral;

	@Inject(optional = true)
	protected final PhysicalVehicleDriveFactory driveActivityFactory = null;

	protected final VisioPositionUtil positionUtil;

	protected final EventProcessor eventProcessor;

	protected final StandardTimeProvider timeProvider;

	protected final IdGenerator rebalancingIdGenerator;

	protected final PhysicalTransportVehicleStorage vehicleStorage;

	protected final SimodConfig config;

	protected final IdGenerator idGenerator;

	protected final AgentpolisConfig agentpolisConfig;

	@Inject
	public PeopleFreightVehicleFactory(
			PhysicalTransportVehicleStorage vehicleStorage,
			TripsUtil tripsUtil,
			StationsDispatcher onDemandVehicleStationsCentral,
			VisioPositionUtil positionUtil,
			EventProcessor eventProcessor,
			StandardTimeProvider timeProvider,
			IdGenerator rebalancingIdGenerator,
			SimodConfig config,
			IdGenerator idGenerator,
			AgentpolisConfig agentpolisConfig) {
		this.tripsUtil = tripsUtil;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.positionUtil = positionUtil;
		this.eventProcessor = eventProcessor;
		this.timeProvider = timeProvider;
		this.rebalancingIdGenerator = rebalancingIdGenerator;
		this.vehicleStorage = vehicleStorage;
		this.config = config;
		this.idGenerator = idGenerator;
		this.agentpolisConfig = agentpolisConfig;
	}

	public PeopleFreightVehicle create(String vehicleId, SimulationNode startPosition, int parcelCapacity) {
		return	new PeopleFreightVehicle(
				vehicleStorage,
				tripsUtil,
				onDemandVehicleStationsCentral,
				driveActivityFactory,
				positionUtil,
				rebalancingIdGenerator,
				eventProcessor,
				timeProvider,
				rebalancingIdGenerator,
				config,
				idGenerator,
				agentpolisConfig,
				vehicleId,
				startPosition,
				parcelCapacity,
				new PhysicalPFVehicle<>(
						vehicleId,
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						parcelCapacity,
						EGraphType.HIGHWAY,
						startPosition,
						agentpolisConfig.maxVehicleSpeedInMeters));
	}
}


