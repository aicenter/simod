package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class VGAVehiclePlanDropoff extends VGAVehiclePlanAction {

    public VGAVehiclePlanDropoff(VGARequest request){
        super(request);
    }

    @Override
    public String toString() {
        return "    Drop off " + request.getDemandAgent().toString() + System.getProperty("line.separator");
    }

	@Override
	public SimulationNode getPosition() {
		return request.to;
	}

}
