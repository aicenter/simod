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
		super(vehicleId, type, lengthInMeters, 1, usingGraphTypeForMoving, position, maxVelocity);

		transportedPackages = new ArrayList<>();
		this.vehiclePackagesCapacity = vehiclePackagesCapacity;
	}


	public List<T> getTransportedPackages(){
		return this.transportedPackages;
	}

	public int getPassengerCapacity()
	{
		// the vehiclePassengerCapacity is always 1 for PF vehicles
		return 1;
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
