package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PickUp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.transportable.SeatTransportableEntity;
import cz.cvut.fel.aic.simod.entity.transportable.TrunkTransportableEntity;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhysicalTransportVehicleWithTrunk<S extends SeatTransportableEntity, T extends TrunkTransportableEntity>
        extends PhysicalTransportVehicle<S> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PhysicalTransportVehicleWithTrunk.class);

    private final int vehicleTrunkCapacity;
    protected final List<T> transportedTrunkEntities;

    public PhysicalTransportVehicleWithTrunk(String vehicleId, EntityType type, float lengthInMeters,
                                             int vehiclePassengerCapacity, GraphType usingGraphTypeForMoving,
                                             SimulationNode position, int maxVelocity, int vehicleTrunkCapacity) {
        super(vehicleId, type, lengthInMeters, vehiclePassengerCapacity, usingGraphTypeForMoving, position, maxVelocity);
        this.vehicleTrunkCapacity = vehicleTrunkCapacity;
        this.transportedTrunkEntities = new LinkedList<>();
    }

    public int getTrunkCapacity() {
        return vehicleTrunkCapacity;
    }

    public List<T> getTransportedTrunkEntities() {
        return transportedTrunkEntities;
    }

    public void pickUp(T entity) {
//        LOGGER.info("pickUp: before pickup -- transported parcels: " + this.transportedTrunkEntities.size());
        PickUp.pickUp(entity, this.transportedTrunkEntities.size() == this.vehicleTrunkCapacity, this,
                this.transportedTrunkEntities);
//        LOGGER.info("pickUp: after pickup -- transported parcels: " + this.transportedTrunkEntities.size());
    }

    public void dropOff(T entityToDropOff) {
        boolean success = this.transportedTrunkEntities.remove(entityToDropOff);
        if (!success) {
            try {
                throw new Exception(String.format("Cannot drop off entity, it is not transported! [%s]", entityToDropOff));
            } catch (Exception ex) {
                Logger.getLogger(PhysicalTransportVehicleWithTrunk.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        entityToDropOff.setTransportingEntity(null);
    }

    @Override
    public void setPosition(SimulationNode position) {
        super.setPosition(position);
        for (T transportedTrunkEntity : transportedTrunkEntities) {
            transportedTrunkEntity.setPosition(position);
        }
    }
}
