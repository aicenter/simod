package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;

public class VGAVehiclePlanDropoff extends VGAVehiclePlanAction {

    public VGAVehiclePlanDropoff(PlanComputationRequest request){
        super(request);
    }

    @Override
    public String toString() {
        return "    Drop off " + request.toString() + System.getProperty("line.separator");
    }

	@Override
	public SimulationNode getPosition() {
		return request.getTo();
	}

}
