package cz.cvut.fel.aic.simod.ridesharing.insertionheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class IHSolverReconfigurableVehicles extends InsertionHeuristicSolver {

	@Inject
	public IHSolverReconfigurableVehicles(
		TravelTimeProvider travelTimeProvider,
		PlanCostProvider travelCostProvider,
		OnDemandVehicleStorage vehicleStorage,
		PositionUtil positionUtil,
		SimodConfig config,
		TimeProvider timeProvider,
		DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory,
		TypedSimulation eventProcessor,
		DroppedDemandsAnalyzer droppedDemandsAnalyzer,
		OnDemandvehicleStationStorage onDemandvehicleStationStorage,
		AgentpolisConfig agentpolisConfig
	) {
		super(
			travelTimeProvider,
			travelCostProvider,
			vehicleStorage,
			positionUtil,
			config,
			timeProvider,
			requestFactory,
			eventProcessor,
			droppedDemandsAnalyzer,
			onDemandvehicleStationStorage,
			agentpolisConfig
		);
	}
}
