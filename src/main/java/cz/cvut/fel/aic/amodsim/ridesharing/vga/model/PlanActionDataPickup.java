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
 * @author david
 */
public class PlanActionDataPickup extends PlanActionData{
	
	private PlanActionData dropoffActionData;

	public void setDropoffActionData(PlanActionData dropoffActionData) {
		this.dropoffActionData = dropoffActionData;
	}
	
	
	
	public PlanActionDataPickup(PlanRequestAction action, int actionIndex, boolean open) {
		super(action, actionIndex, open);
	}
	
	public void openDropOff(boolean open){
		dropoffActionData.setOpen(open);
	}
	
}
