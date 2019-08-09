/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.util.LinkedHashSet;

/**
 *
 * @author david
 */
public class VirtualVehicle implements IOptimalPlanVehicle{
	
	private final OnDemandVehicleStation station;
	
	private final int capacity;
	
	private final int carLimit;

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
	
	
	
}
