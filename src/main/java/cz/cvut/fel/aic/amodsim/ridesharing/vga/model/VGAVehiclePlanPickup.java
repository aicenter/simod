package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class VGAVehiclePlanPickup extends VGAVehiclePlanAction {

    public VGAVehiclePlanPickup(VGARequest request) {
        super(request);
    }

    @Override
    public String toString() {
        return "    Pick up  " + request.getDemandAgent().toString() + System.getProperty("line.separator");
    }

	@Override
	public SimulationNode getPosition() {
		return request.from;
	}

}
