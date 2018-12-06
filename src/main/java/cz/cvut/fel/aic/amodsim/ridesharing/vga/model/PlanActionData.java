/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

/**
 *
 * @author F.I.D.O.
 */
public class PlanActionData{
		
	private final VGAVehiclePlanAction action;

	private final int actionIndex;

	private boolean used;

	private double durationFromPreviousAction;

	private double discomfort;

	private boolean open;

	
	
	public VGAVehiclePlanAction getAction() {
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
	
	

	public PlanActionData(VGAVehiclePlanAction action, int actionIndex, boolean open) {
		this.action = action;
		this.actionIndex = actionIndex;
		this.open = open;
		used = false;
		durationFromPreviousAction = 0;
		discomfort = 0;
	}
}
