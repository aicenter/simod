package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public abstract class VGAVehiclePlanAction {

    final VGARequest request;
	
	public VGARequest getRequest() { 
		return request; 
	}

    VGAVehiclePlanAction(VGARequest request) {
        this.request = request;
    }

    

    public abstract SimulationNode getPosition();

}
