package cz.cvut.fel.aic.simod.storage;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PhysicalPFVehicle;


@Singleton
public class PhysicalPFVehicleStorage extends EntityStorage<PhysicalPFVehicle>{

    @Inject
    public PhysicalPFVehicleStorage() {
        super();
    }

}
