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
package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionCurrentPosition;
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
	
	
	public DriverPlan toDriverPlan() {
		List<PlanAction> tasks = new ArrayList<>(getActions().size() + 1);
		tasks.add(new PlanActionCurrentPosition(getVehicle().getPosition()));
		for(PlanRequestAction action: getActions()){
			tasks.add(action);
		}
		DriverPlan driverPlan = new DriverPlan(tasks, getEndTime() - getStartTime(), getCost());
		
		return driverPlan;
	}
}
