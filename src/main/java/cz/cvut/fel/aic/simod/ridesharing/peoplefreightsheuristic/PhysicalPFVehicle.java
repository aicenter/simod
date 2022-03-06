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


public class PhysicalPFVehicle<T extends TransportableEntity> extends PhysicalTransportVehicle
{
	private static final int vehiclePassengerCapacity = 1;

	protected final List<T> transportedPackages;
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
	public void pickUp(T entity) {
		if (entity instanceof DemandAgent) {
			PickUp.pickUp(entity, this.transportedEntities.size() == this.vehiclePassengerCapacity, this, this.transportedEntities);
		}
		else {
			PickUp.pickUp(entity, this.transportedPackages.size() == this.vehiclePackagesCapacity, this, this.transportedPackages);
		}
	}

	@Override
	public void dropOff(T entityToDropOff) {
		// if entity is Agent
		if (entityToDropOff instanceof DemandAgent) {
			boolean success = this.transportedEntities.remove(entityToDropOff);
			if (!success) {
				try {
					throw new Exception(String.format("Cannot drop off entity, it is not transported! [%s]", entityToDropOff));
				} catch (Exception var4) {
					Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, (String)null, var4);
				}
			}

			entityToDropOff.setTransportingEntity((TransportEntity)null);
		}
		// if entity is Package
		else {
			boolean success = this.transportedPackages.remove(entityToDropOff);
			if (!success) {
				try {
					throw new Exception(String.format("Cannot drop off entity, it is not transported! [%s]", entityToDropOff));
				} catch (Exception var4) {
					Logger.getLogger(PhysicalPFVehicle.class.getName()).log(Level.SEVERE, (String)null, var4);
				}
			}

			entityToDropOff.setTransportingEntity((TransportEntity)null);
		}
	}


	public List<T> getTransportedPackages(){
		return this.transportedPackages;
	}

	public int getPassengerCapacity()
	{
		// the vehiclePassengerCapacity is always 1 for PF vehicles
		return vehiclePassengerCapacity;
	}

	public int getVehiclePackagesCapacity()
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
