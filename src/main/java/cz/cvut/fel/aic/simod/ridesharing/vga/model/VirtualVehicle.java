/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.LinkedHashSet;

/**
 *
 * @author david
 */
public class VirtualVehicle implements IOptimalPlanVehicle{
	
	private final OnDemandVehicleStation station;
	
	private final int capacity;
	
	private final int carLimit;
	
	private final RideSharingOnDemandVehicle exampleVehicle;

	public int getCarLimit() {
		return carLimit;
	}

	
	
	
	public OnDemandVehicleStation getStation() {
		return station;
	}

	
	
	
	public VirtualVehicle(OnDemandVehicleStation station, int capacity, int carLimit) {
		this.station = station;
		this.capacity = capacity;
		this.carLimit = carLimit;
		exampleVehicle = (RideSharingOnDemandVehicle) station.getVehicle(0);
	}
	
	

	@Override
	public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() {
		return new LinkedHashSet<>(); 
	}

	@Override
	public SimulationNode getPosition() {
		return station.getPosition();
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public String getId() {
		return "Virtual vehicle for station " + station.getId();
	}

	@Override
	public MovingEntity getRealVehicle() {
		return exampleVehicle;
	}
	
	
	
	
}
