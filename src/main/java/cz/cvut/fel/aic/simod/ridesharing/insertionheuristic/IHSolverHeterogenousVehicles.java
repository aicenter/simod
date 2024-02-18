package cz.cvut.fel.aic.simod.ridesharing.insertionheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.SlotType;
import cz.cvut.fel.aic.simod.entity.vehicle.SpecializedTransportVehicle;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class IHSolverHeterogenousVehicles extends InsertionHeuristicSolver{

	@Inject
	public IHSolverHeterogenousVehicles(
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
	protected int getFreeCapacityForRequest(
		RideSharingOnDemandVehicle vehicle,
		PlanComputationRequest planComputationRequest
	) {
		SpecializedTransportVehicle specializedVehicle = (SpecializedTransportVehicle) vehicle.getVehicle();
		return specializedVehicle.getCapacityFor(planComputationRequest.getDemandAgent().getRequiredSlotType());
	}

	@Override
	protected int adjustFreeCapacity(
		int freeCapacity, DriverPlan currentPlan, int evaluatedIndex, PlanComputationRequest planComputationRequest
	) {
		if (evaluatedIndex < currentPlan.getLength()) { // no need to adjust if the evaluated index is the last one
			SlotType requiredSlotType = planComputationRequest.getDemandAgent().getRequiredSlotType();
			PlanRequestAction action = (PlanRequestAction) currentPlan.plan.get(evaluatedIndex);
			SlotType currentSlotType = action.getRequest().getDemandAgent().getRequiredSlotType();
			if (requiredSlotType.equals(currentSlotType)) {
				if (currentPlan.plan.get(evaluatedIndex) instanceof PlanActionPickup) {
					freeCapacity--;
				} else {
					freeCapacity++;
				}
			}
		}
		return freeCapacity;
	}
}
