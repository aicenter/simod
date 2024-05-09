package cz.cvut.fel.aic.simod.ridesharing.insertionheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.action.PlanAction;
import cz.cvut.fel.aic.simod.action.PlanActionPickup;
import cz.cvut.fel.aic.simod.action.PlanRequestAction;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.vehicle.ConfigurationFilter;
import cz.cvut.fel.aic.simod.entity.vehicle.ReconfigurableVehicle;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class IHSolverReconfigurableVehicles extends InsertionHeuristicSolver<ConfigurationFilter> {

//	private ConfigurationFilter configurationFilter;

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

	@Override
	protected ConfigurationFilter initFreeCapacityForRequest(
		RideSharingOnDemandVehicle vehicle,
		PlanComputationRequest planComputationRequest
	){
		ReconfigurableVehicle reconfigurableVehicle = (ReconfigurableVehicle) vehicle.getVehicle();
		return new ConfigurationFilter(reconfigurableVehicle);
	}

	@Override
	protected boolean hasCapacityForRequest(PlanComputationRequest planComputationRequest, ConfigurationFilter counter){
		return counter.hasCapacityFor(planComputationRequest);
	}

	@Override
	protected ConfigurationFilter adjustFreeCapacity(
		PlanRequestAction action,
		PlanComputationRequest planComputationRequest,
		ConfigurationFilter counter
	){
		PlanComputationRequest evaluatedRequest = action.getRequest();
		if (action instanceof PlanActionPickup) {
			counter.pickUp(evaluatedRequest);
		} else {
			counter.dropOff(evaluatedRequest);
		}

		return counter;
	}
}
