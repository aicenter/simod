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

import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;

import java.util.*;

public class VGAVehicle implements IOptimalPlanVehicle{

	private static Map<OnDemandVehicle, VGAVehicle> agentpolisVehicleToVGA = new LinkedHashMap<>();

	private final RideSharingOnDemandVehicle onDemandVehicle;
	private final LinkedHashSet<PlanComputationRequest> requestsOnBoard;

	private VGAVehicle(RideSharingOnDemandVehicle v) {
		this.onDemandVehicle = v;
		requestsOnBoard = new LinkedHashSet<>();
		agentpolisVehicleToVGA.put(v, this);
	}

	public static void resetMapping() {
		agentpolisVehicleToVGA = new LinkedHashMap<>();
	}

	public static VGAVehicle newInstance(RideSharingOnDemandVehicle v){
		return new VGAVehicle(v);
	}

	public static VGAVehicle getVGAVehicleByRidesharingOnDemandVehicle( OnDemandVehicle v) {
		return agentpolisVehicleToVGA.get(v);
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
