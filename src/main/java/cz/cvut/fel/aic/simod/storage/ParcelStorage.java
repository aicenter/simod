package cz.cvut.fel.aic.simod.storage;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.simod.entity.ParcelAgent;

@Singleton
public class ParcelStorage extends EntityStorage<ParcelAgent> {
    public ParcelStorage() {
        super();
    }
}
