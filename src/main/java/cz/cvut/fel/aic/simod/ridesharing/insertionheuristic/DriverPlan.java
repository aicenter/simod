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
package cz.cvut.fel.aic.simod.ridesharing.insertionheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionCurrentPosition;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlan implements Iterable<PlanAction>{
	public final List<PlanAction> plan;

	public final long totalTime;
	
	public final double cost;

	
	
	
	public DriverPlan(List<PlanAction> plan, long totalTime, double cost) {
		this.plan = plan;
		this.totalTime = totalTime;
		this.cost = cost;
	}

	@Override
	public Iterator<PlanAction> iterator() {
		return plan.iterator();
	}
	
	public int getLength(){
		return plan.size();
	}
	
	public void updateCurrentPosition(SimulationNode position){
		plan.set(0, new PlanActionCurrentPosition(position));
	}
	
	public PlanAction getNextTask(){
		return plan.get(1);
	}
	
	public void taskCompleted(){
		plan.remove(1);
	}

	@Override
	public String toString() {
		StringBuilder sb  = new StringBuilder("[");
		int counter = 0;
		for(PlanAction action: this){
			sb.append(String.format("%s: %s, ", counter, action.toString()));
			counter++;
		}
		sb.append("]");
		return sb.toString();
	}
	
	
}
