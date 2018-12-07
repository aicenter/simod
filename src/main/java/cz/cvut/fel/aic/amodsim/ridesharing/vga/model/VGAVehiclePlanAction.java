package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;

public abstract class VGAVehiclePlanAction {

    final PlanComputationRequest request;
	
	public PlanComputationRequest getRequest() { 
		return request; 
	}

    VGAVehiclePlanAction(PlanComputationRequest request) {
        this.request = request;
    }

    

    public abstract SimulationNode getPosition();

}
