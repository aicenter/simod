/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;
import java.util.LinkedHashSet;

/**
 *
 * @author david
 */
public class VirtualVehicle implements IOptimalPlanVehicle{
	
	private final OnDemandVehicleStation station;
	
	private final int capacity;
	
	private final int carLimit;

	
	
	
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
