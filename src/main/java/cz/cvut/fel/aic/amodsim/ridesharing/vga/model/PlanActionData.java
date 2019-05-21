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

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;

/**
 *
 * @author F.I.D.O.
 */
public class PlanActionData{
		
	private final PlanRequestAction action;

	private final int actionIndex;

	private boolean used;

	private double durationFromPreviousAction;

	private double discomfort;

	private boolean open;

	
	
	public PlanRequestAction getAction() {
		return action;
	}

	public int getActionIndex() {
		return actionIndex;
	}
	
	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public double getDurationFromPreviousAction() {
		return durationFromPreviousAction;
	}

	public void setDurationFromPreviousAction(double durationFromPreviousAction) {
		this.durationFromPreviousAction = durationFromPreviousAction;
	}

	public double getDiscomfort() {
		return discomfort;
	}

	public void setDiscomfort(double discomfort) {
		this.discomfort = discomfort;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}
	
	

	public PlanActionData(PlanRequestAction action, int actionIndex, boolean open) {
		this.action = action;
		this.actionIndex = actionIndex;
		this.open = open;
		used = false;
		durationFromPreviousAction = 0;
		discomfort = 0;
	}

	@Override
	public String toString() {
		return "PlanActionData{" + "action=" + action + '}';
	}
	
}
