/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
