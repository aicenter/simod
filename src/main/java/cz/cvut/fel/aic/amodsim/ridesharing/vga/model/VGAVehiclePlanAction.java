package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;

public abstract class VGAVehiclePlanAction {

//    private double time;
    private SimulationNode position;
    final VGARequest request;

    VGAVehiclePlanAction(VGARequest request, SimulationNode position, VGAVehiclePlan plan) {
        this.request = request;
        this.position = position;

//		if (plan.getActions().isEmpty()) {
//			time = VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0;
//		}
//
//		time += plan.getCurrentTime() + MathUtils.getTravelTimeProvider().getTravelTime(
//				plan.getVehicle(), plan.getCurrentPosition(), position) / 1000.0;
//        
//        if (this instanceof VGAVehiclePlanPickup) {
//            if (time < request.getOriginTime()) {
//                time = request.getOriginTime();
//            }
//        }
    }

//    double getTime() { return time; }

    public VGARequest getRequest() { return request; }

    public SimulationNode getPosition() { return position; }

}
