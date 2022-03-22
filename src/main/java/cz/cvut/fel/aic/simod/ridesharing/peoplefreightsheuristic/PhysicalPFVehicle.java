package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PickUp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.DemandPackage;
import cz.cvut.fel.aic.simod.entity.TransportableEntityManagement;

import java.util.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PhysicalPFVehicle<T extends TransportableEntity> extends PhysicalTransportVehicle<T> implements TransportEntity<T>
{
	private static final int vehiclePassengerCapacity = 1;

	protected final List<DemandPackage> transportedPackages;
	private final int vehiclePackagesCapacity;

	public PhysicalPFVehicle(
			String vehicleId,
			EntityType type,
			float lengthInMeters,
			int vehiclePackagesCapacity,
			GraphType usingGraphTypeForMoving,
			SimulationNode position,
			int maxVelocity)
	{
		// the vehiclePassengerCapacity is always 1 for PF vehicles
		super(vehicleId, type, lengthInMeters, vehiclePassengerCapacity, usingGraphTypeForMoving, position, maxVelocity);

		transportedPackages = new ArrayList<>();
		this.vehiclePackagesCapacity = vehiclePackagesCapacity;
	}

	// implements pickUp() for both Agents and Packages
	@Override
	public void pickUp(TransportableEntity entity) {
		if (entity instanceof DemandAgent) {
			PickUp.pickUp((T) entity, this.transportedEntities.size() == this.vehiclePassengerCapacity, this, this.transportedEntities);
		}
		else {
			PickUp.pickUp((T) entity, this.transportedPackages.size() == this.vehiclePackagesCapacity, this, this.transportedPackages);
		}
	}

	@Override
	public void dropOff(TransportableEntity entityToDropOff) {
		// if entity is Agent
		boolean success;
		if (entityToDropOff instanceof DemandAgent) {
			success = this.transportedEntities.remove(entityToDropOff);
		}
		// if entity is Package
		else {
			success = this.transportedPackages.remove(entityToDropOff);
		}

		if (!success) {
			try {
				throw new Exception(String.format("Cannot drop off entity, it is not transported! [%s]", entityToDropOff));
			} catch (Exception var4) {
				Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, (String)null, var4);
			}
		}

		entityToDropOff.setTransportingEntity(null);
	}


	public List<DemandPackage> getTransportedPackages(){
		return this.transportedPackages;
	}

	public int getPassengerCapacity()
	{
		// the vehiclePassengerCapacity is always 1 for PF vehicles
		return vehiclePassengerCapacity;
	}

	public int getPackagesCapacity()
	{
		return this.vehiclePackagesCapacity;
	}

	public boolean isHighlited()
	{
		return super.isHighlited();
	}

	public void setHighlited(boolean highlited)
	{
		super.setHighlited(highlited);
	}
}
