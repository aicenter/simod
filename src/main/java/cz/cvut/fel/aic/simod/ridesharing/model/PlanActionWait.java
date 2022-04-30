package cz.cvut.fel.aic.simod.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class PlanActionWait extends PlanRequestAction {

    protected long waitTime;

    protected boolean waitingStarted = false;

    protected long waitingStartedAt;

    protected boolean waitingPaused = false;

    protected long waitingPausedAt;

    public long getWaitingStartedAt() {
        return waitingStartedAt;
    }

    public void setWaitingStartedAt(long waitingStartedAt) {
        this.waitingStartedAt = waitingStartedAt;
    }

    public long getWaitTime(){
        return waitTime;
    }

    public boolean isWaitingStarted() {
        return waitingStarted;
    }

    public boolean isWaitingPaused() {
        return waitingPaused;
    }

    public void setWaitingPaused(boolean waitingPaused) {
        this.waitingPaused = waitingPaused;
    }

    public long getWaitingPausedAt() {
        return waitingPausedAt;
    }

    public void setWaitingPausedAt(long waitingPausedAt) {
        this.waitingPausedAt = waitingPausedAt;
    }

    public void setWaitingStarted(boolean waitingStarted) {
        this.waitingStarted = waitingStarted;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }



    public PlanActionWait(PlanComputationRequest request, SimulationNode node, int maxTime, long waitTime) {
        super(request, node, maxTime);
        this.waitTime = waitTime;
        this.waitingStarted = false;

    }

    @Override
    public String toString() {
        return String.format("Wait for demand %s at node %s", request.getDemandAgent().getId(), location.id);
    }
}
