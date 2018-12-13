package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;

public abstract class VGAVehiclePlanAction {

    final PlanComputationRequest request;
	
	private final SimulationNode node;
	
	private final int maxTime;
	
	public PlanComputationRequest getRequest() { 
		return request; 
	}
	
	public SimulationNode getPosition(){
		return node;
	}

	public int getMaxTime() {
		return maxTime;
	}
	
	

    VGAVehiclePlanAction(PlanComputationRequest request, SimulationNode node, int maxTime) {
        this.request = request;
		this.node = node;
		this.maxTime = maxTime;
    }


}
