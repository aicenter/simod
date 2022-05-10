package cz.cvut.fel.aic.simod;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.ActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.WaitActivityFactory;

@Singleton
public class WaitTransferActivityFactory extends ActivityFactory {


    public <A extends Agent> void runActivity(A agent, long waitTime, WaitActivityFactory waitActivityFactory) {
        create(agent, waitTime, waitActivityFactory).run();
    }

    public <A extends Agent> WaitTransfer<A> create(A agent, long waitTime, WaitActivityFactory waitActivityFactory) {
        return new WaitTransfer<>(activityInitializer, agent, waitTime, waitActivityFactory);
    }
}
