package cz.cvut.fel.aic.simod.storage;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;


@Singleton
public class PeopleFreightVehicleStorage extends EntityStorage<OnDemandVehicle>{

    @Inject
    public PeopleFreightVehicleStorage() {
        super();
    }



}
