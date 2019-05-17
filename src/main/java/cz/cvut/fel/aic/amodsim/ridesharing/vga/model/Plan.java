/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 * @param <V>
 */
public class Plan<V extends IOptimalPlanVehicle>{
	private final int startTime;
	
	private final int endTime;

	private final int cost;
	
	private final List<PlanRequestAction> actions;
	
	private final V vehicle;
	

	public int getCost() {
		return cost;
	}

	public List<PlanRequestAction> getActions() {
		return actions;
	}

	public V getVehicle() {
		return vehicle;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}
	
	

	/**
	 * Empty plan constructor
	 * @param startTime
	 * @param vehicle 
	 */
	public Plan(int startTime, V vehicle) {
		this.startTime = startTime;
		this.vehicle = vehicle;
		endTime = startTime;
		cost = 0;
		actions = new ArrayList<>();
	}

	
	
	
	
	public Plan(int startTime, int endTime, int cost, List<PlanRequestAction> actions, V vehicle) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.cost = cost;
		this.actions = actions;
		this.vehicle = vehicle;
	}


	public Plan duplicateForVehicle(V vehicle){
		return new Plan(startTime, endTime, cost, actions, vehicle);
	}
}
