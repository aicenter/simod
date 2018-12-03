/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class VehiclePlanList {
	public final VGAVehicle vGAVehicle;
	
	public final List<VGAVehiclePlan> feasibleGroupPlans;

	public VehiclePlanList(VGAVehicle vGAVehicle,List<VGAVehiclePlan> feasibleGroupPlans) {
		this.vGAVehicle = vGAVehicle;
		this.feasibleGroupPlans = feasibleGroupPlans;
	}
	
	
}
