package cz.cvut.fel.aic.simod.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionWait extends PlanRequestAction {

    protected final long waitTime;



    public long getWaitTime(){
        return waitTime;
    }



    public PlanActionWait(PlanComputationRequest request, SimulationNode node, int maxTime, long waitTime) {
        super(request, node, maxTime);
        this.waitTime = waitTime;

    }

    @Override
    public String toString() {
        return String.format("Wait for demand %s at node %s", request.getDemandAgent().getId(), location.id);
    }
}
