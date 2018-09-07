package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

public class VGAVehiclePlanDropoff extends VGAVehiclePlanAction {

    public VGAVehiclePlanDropoff(VGARequest request, VGAVehiclePlan plan){
        super(request, request.getDestinationSimulationNode(), plan);
    }

    @Override
    public String toString() {
        return "    Drop off request id: " + request.getId() + System.getProperty("line.separator");
    }

}
