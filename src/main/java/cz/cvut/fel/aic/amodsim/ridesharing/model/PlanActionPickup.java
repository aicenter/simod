package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionPickup extends PlanRequestAction {

	public PlanActionPickup(PlanComputationRequest request, SimulationNode node, int maxTime) {
		super(request, node, maxTime);
	}

   

    @Override
    public String toString() {
        return "    Pick up  " + request.toString() + System.getProperty("line.separator");
    }


}
