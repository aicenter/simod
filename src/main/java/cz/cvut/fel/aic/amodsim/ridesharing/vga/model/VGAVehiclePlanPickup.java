package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

public class VGAVehiclePlanPickup extends VGAVehiclePlanAction {

    public VGAVehiclePlanPickup(VGARequest request, VGAVehiclePlan plan) {
        super(request, request.from, plan);
    }

    @Override
    public String toString() {
        return "    Pick up  " + request.getDemandAgent().toString() + System.getProperty("line.separator");
    }

}
