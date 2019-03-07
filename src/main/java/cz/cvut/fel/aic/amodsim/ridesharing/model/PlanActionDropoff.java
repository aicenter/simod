package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionDropoff extends PlanRequestAction {

	public PlanActionDropoff(PlanComputationRequest request, SimulationNode node, int maxTime) {
		super(request, node, maxTime);
	}



    @Override
    public String toString() {
        return "    Drop off " + request.toString() + System.getProperty("line.separator");
    }



}
