package cz.cvut.fel.aic.simod;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.ActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.Wait;

@Singleton
public class WaitWithStopActivityFactory extends ActivityFactory {


    public <A extends Agent> void runActivity(A agent, long waitTime) {
        create(agent, waitTime).run();
    }

    public <A extends Agent> WaitWithStop create(A agent, long waitTime) {
        return new WaitWithStop<>(activityInitializer, agent, waitTime);
    }
}