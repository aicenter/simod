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
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;
import cz.cvut.fel.aic.simod.entity.vehicle.SpecializedTransportVehicle;
import cz.cvut.fel.aic.simod.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class IHSolverHeterogenousVehicles extends InsertionHeuristicSolver<Integer>{

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
	protected Integer initFreeCapacityForRequest(
		RideSharingOnDemandVehicle vehicle,
		PlanComputationRequest planComputationRequest
	) {
		SpecializedTransportVehicle specializedVehicle = (SpecializedTransportVehicle) vehicle.getVehicle();

		SlotType requiredSlotType = planComputationRequest.getDemandAgent().getRequiredSlotType();
		int usedCapacity = 0;
		for(DemandAgent demandAgent: specializedVehicle.getTransportedEntities()){
			if(demandAgent.getRequiredSlotType().equals(requiredSlotType)){
				usedCapacity++;
			}
		}

		return specializedVehicle.getCapacityFor(planComputationRequest.getDemandAgent().getRequiredSlotType()) - usedCapacity;
	}

	@Override
	protected Integer adjustFreeCapacity(
		PlanRequestAction action,
		PlanComputationRequest planComputationRequest,
		Integer counter
	) {
		SlotType requiredSlotType = planComputationRequest.getDemandAgent().getRequiredSlotType();

		SlotType currentSlotType = action.getRequest().getDemandAgent().getRequiredSlotType();
		if (requiredSlotType.equals(currentSlotType)) {
			if(action instanceof PlanActionPickup) {
				counter--;
			} else {
				counter++;
			}
		}

		return counter;
	}
}
