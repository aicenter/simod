package cz.cvut.fel.aic.simod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;

@Singleton
public class DemandIdGenerator extends IdGenerator {
    private int currentId = 0;

    @Inject
    public DemandIdGenerator() {
    }

    public int getId() {
        int id = this.currentId++;
        return id;
    }
}
