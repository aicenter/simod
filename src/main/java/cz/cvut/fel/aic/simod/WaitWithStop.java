package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.simmodel.ActivityInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.TimeConsumingActivity;

public class WaitWithStop<A extends Agent> extends TimeConsumingActivity<A>{

    private final long waitTime;

    protected boolean stoped;


    public WaitWithStop(ActivityInitializer activityInitializer, A agent, long waitTime) {
        super(activityInitializer, agent);
        this.waitTime = waitTime;
        this.stoped = false;
    }

    public boolean isStoped() {
        return stoped;
    }

    public void end(){
        stoped = true;
    }

    @Override
    protected long performPreDelayActions() {
        return waitTime;
    }

    @Override
    protected void performAction() {
        finish();
    }

}
