/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.*;

public class VGAVehicle implements IOptimalPlanVehicle{

	private final RideSharingOnDemandVehicle onDemandVehicle;
	private final LinkedHashSet<PlanComputationRequest> requestsOnBoard;

	private VGAVehicle(RideSharingOnDemandVehicle v) {
		this.onDemandVehicle = v;
		requestsOnBoard = new LinkedHashSet<>();
	}

	public static VGAVehicle newInstance(RideSharingOnDemandVehicle v){
		return new VGAVehicle(v);
	}

	public RideSharingOnDemandVehicle getRidesharingVehicle() { return onDemandVehicle; }

	@Override
	public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() { 
		return requestsOnBoard; 
	}

	public void addRequestOnBoard(PlanComputationRequest request) { 
		requestsOnBoard.add(request); 
	}

	public void removeRequestOnBoard(PlanComputationRequest request) { 
		requestsOnBoard.remove(request); 
	}

	@Override
	public String toString() {
		return String.format("VGA vehicle: %s", onDemandVehicle.getId());
	}

	@Override
	public SimulationNode getPosition() {
		return onDemandVehicle.getPosition();
	}

	@Override
	public int getCapacity() {
		return onDemandVehicle.getCapacity();
	}

	@Override
	public String getId() {
		return "VGA vehicle " + onDemandVehicle.getId();
	}

	@Override
	public MovingEntity getRealVehicle() {
		return getRidesharingVehicle();
	}
	
	

}
