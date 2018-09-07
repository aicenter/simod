package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;

public class VGAVehiclePlanAction {

    private double time;
    private SimulationNode position;
    VGARequest request;

    VGAVehiclePlanAction(VGARequest request, SimulationNode position, VGAVehiclePlan plan){
        this.request = request;
        this.position = position;

        time = plan.getCurrentTime() + MathUtils.getTravelTimeProvider().getTravelTime(VehicleGroupAssignmentSolver.getVehicle(), plan.getCurrentPosition(), position);

        if(this instanceof VGAVehiclePlanPickup){
            if(time < request.getOriginTime()){
                time = request.getOriginTime();
            }
        }
    }

    double getTime() { return time; }

    public VGARequest getRequest() { return request; }

    public SimulationNode getPosition() { return position; }

}
