package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PickUp;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.GraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.DemandAgent;

import java.util.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PhysicalPFVehicle extends PhysicalTransportVehicle implements TransportEntity
{
	// the vehiclePassengerCapacity is always 1 for PF vehicles
	private static final int vehiclePassengerCapacity = 4;

	protected final List<TransportableEntity> transportedPackages;
	private final int vehiclePackagesCapacity;
	private int currentPackagesWeight;


	public PhysicalPFVehicle(
			String vehicleId,
			EntityType type,
			float lengthInMeters,
			int vehiclePackagesCapacity,
			GraphType usingGraphTypeForMoving,
			SimulationNode position,
			int maxVelocity)
	{
		super(vehicleId, type, lengthInMeters, vehiclePassengerCapacity, usingGraphTypeForMoving, position, maxVelocity);

		transportedPackages = new ArrayList<>();
		this.vehiclePackagesCapacity = vehiclePackagesCapacity;
	}

	// implements pickUp() for both Agents and Packages
	public void pickUp(TransportableEntity entity) {
		if (entity instanceof DemandAgent) {
			PickUp.pickUp(entity, this.transportedEntities.size() == vehiclePassengerCapacity, this , this.transportedEntities);
		}
		else {
			int newParcelsWeight = getCurrentPackagesWeight() + ((DemandPackage) entity).getWeight();
			if (newParcelsWeight > vehiclePackagesCapacity) {

			}

			PickUp.pickUp(entity, newParcelsWeight > vehiclePackagesCapacity, this, this.transportedPackages);

			updateCurrentPackagesWeight(newParcelsWeight);
		}
	}

	@Override
	public void dropOff(TransportableEntity entityToDropOff) {
		boolean success;
		// if entity is Agent
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
				Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, null, var4);
			}
		}

		entityToDropOff.setTransportingEntity(null);
		if (entityToDropOff instanceof DemandPackage) {
			int newParcelsWeight = getCurrentPackagesWeight() - ((DemandPackage) entityToDropOff).getWeight();
			updateCurrentPackagesWeight(newParcelsWeight);
		}
	}


	public List<TransportableEntity> getTransportedPackages(){
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

	public int getCurrentPackagesWeight() {
		return currentPackagesWeight;
	}

	// TODO maybe the exception is not necessary
	private void updateCurrentPackagesWeight(int newPackagesWeight) {
		try {
			if (newPackagesWeight > vehiclePackagesCapacity) {
				throw new Exception(
						String.format("Vehicle %s cannot carry packages of weight %d - vehicle capacity is %d",
								this, newPackagesWeight, vehiclePackagesCapacity));
			}
			else {
				this.currentPackagesWeight = newPackagesWeight;
			}
		}
		catch (Exception ex) {
			Logger.getLogger(PhysicalPFVehicle.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public boolean isHighlited()
	{
		return super.isHighlited();
	}

	public void setHighlited(boolean highlited)
	{
		super.setHighlited(highlited);
	}

	@Override
	public void setPosition(SimulationNode position) {
		super.setPosition(position);
		for (TransportableEntity transportedPackage : transportedPackages) {
			transportedPackage.setPosition(position);
		}
	}
}
