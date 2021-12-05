package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PickUp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NO_PhysicalPFVehicle2<T extends TransportableEntity> extends PhysicalVehicle implements TransportEntity<T>
{
    protected final List<T> transportedEntities;
    private final int vehiclePassengerCapacity;

    protected final List<T> transportedPackages;
    private final int vehiclePackagesCapacity;

    private boolean highlited;

    public NO_PhysicalPFVehicle2(
            String vehicleId,
            EntityType type,
            float lengthInMeters,
            int vehiclePassengerCapacity,
            GraphType usingGraphTypeForMoving,
            SimulationNode position,
            int maxVelocity,
            int vehiclePackagesCapacity
    )
    {
        super(vehicleId, type, lengthInMeters, usingGraphTypeForMoving, position, maxVelocity);

        transportedEntities = new ArrayList<>();
        this.vehiclePassengerCapacity = 1;
        transportedPackages = new ArrayList<>();
        this.vehiclePackagesCapacity = vehiclePackagesCapacity;

    }


    public void pickUp(T entity)
    {
        PickUp.pickUp(entity, this.transportedEntities.size() == this.vehiclePassengerCapacity, this, this.transportedEntities);
    }

    public void dropOff(T entityToDropOff)
    {
        boolean success = this.transportedEntities.remove(entityToDropOff);
        if (!success)
        {
            try
            {
                throw new Exception(String.format("Cannot drop off entity, it is not transported! [%s]", entityToDropOff));
            }
            catch (Exception var4)
            {
                Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, (String) null, var4);
            }
        }

        entityToDropOff.setTransportingEntity((TransportEntity) null);
    }

    public void setPosition(SimulationNode position)
    {
        super.setPosition(position);
        Iterator var2 = this.transportedEntities.iterator();

        while (var2.hasNext())
        {
            T transportedEntity = (T) ((TransportableEntity) var2.next());
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

    public int getCapacity()
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
