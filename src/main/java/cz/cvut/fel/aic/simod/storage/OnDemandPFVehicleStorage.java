package cz.cvut.fel.aic.simod.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.OnDemandPFVehicle;

@Singleton
public class OnDemandPFVehicleStorage extends EntityStorage<OnDemandPFVehicle>
{
    @Inject
    public OnDemandPFVehicleStorage()
    {
        super();
    }

}
