package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

import java.util.*;

import java.util.List;

public class PhysicalPFVehicle<T extends TransportableEntity> extends PhysicalTransportVehicle
{

    protected final List<T> transportedEntities;
    private final int vehiclePassengerCapacity;

    protected final List<T> transportedPackages;
    private final int vehiclePackagesCapacity;

    private boolean highlited;

    public PhysicalPFVehicle(
            String vehicleId,
            EntityType type,
            float lengthInMeters,
            int vehiclePackagesCapacity,
            GraphType usingGraphTypeForMoving,
            SimulationNode position,
            int maxVelocity,
            int vehiclePassengerCapacity
    )
    {
        super(vehicleId, type, lengthInMeters, vehiclePassengerCapacity, usingGraphTypeForMoving, position, maxVelocity);

        transportedEntities = new ArrayList<>();
        this.vehiclePassengerCapacity = 1;
        transportedPackages = new ArrayList<>();
        this.vehiclePackagesCapacity = vehiclePackagesCapacity;

    }


    public void setPosition(SimulationNode position)
    {
        super.setPosition(position);
        Iterator var2 = this.transportedEntities.iterator();

        while (var2.hasNext())
        {
            T transportedEntity = (T) var2.next();        // originally was (TransportableEntity)
            transportedEntity.setPosition(position);
        }
    }


    @Override
    public List<T> getTransportedEntities()
    {
        return transportedEntities;
    }

    public List<T> getTransportedPackages()
    {
        return transportedPackages;
    }

    public int getPassengerCapacity()
    {
        return this.vehiclePassengerCapacity;
    }

    public int getVehiclePackagesCapacity()
    {
        return vehiclePackagesCapacity;
    }


    public boolean isHighlited()
    {
        return this.highlited;
    }

    public void setHighlited(boolean highlited)
    {
        this.highlited = highlited;
    }
}
