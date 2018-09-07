package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

public class VGAVehiclePlanRequestDrop extends VGAVehiclePlanAction {

    public VGAVehiclePlanRequestDrop(VGARequest request, VGAVehiclePlan plan) {
        super(request, request.getOriginSimulationNode(), plan);
    }

    @Override
    public String toString() {
        return "Dropping request id: " + request.getId() + System.getProperty("line.separator");
    }

}
