/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import java.util.LinkedHashSet;

/**
 *
 * @author LocalAdmin
 */
public interface IOptimalPlanVehicle {

	public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard();

	public SimulationNode getPosition();

	public int getCapacity();
	
}
