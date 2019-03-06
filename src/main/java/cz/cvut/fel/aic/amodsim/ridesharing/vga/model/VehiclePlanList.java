/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class VehiclePlanList {
	public final IOptimalPlanVehicle optimalPlanVehicle;
	
	public final List<Plan> feasibleGroupPlans;

	public VehiclePlanList(IOptimalPlanVehicle vGAVehicle, List<Plan> feasibleGroupPlans) {
		this.optimalPlanVehicle = vGAVehicle;
		this.feasibleGroupPlans = feasibleGroupPlans;
	}
	
	
}
