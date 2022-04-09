package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;

public interface PFPlanCompRequest extends PlanComputationRequest {

	public TransportableEntity_2 getDemandEntity();
}
