package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionDropoff extends PlanRequestAction {

	/**
	 * Pickup action.
	 * @param request Request
	 * @param node Position where action takes place.
	 * @param maxTime Time constraint in seconds.
	 */
	public PlanActionDropoff(PlanComputationRequest request, SimulationNode node, int maxTime) {
		super(request, node, maxTime);
	}



    @Override
    public String toString() {
        return String.format("Drop off demand %s at node %s", request.getDemandAgent().getId(), location.id);
    }



}
