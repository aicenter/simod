package cz.cvut.fel.aic.amodsim.ridesharing.vga.planBuilder;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;

import java.util.*;

/**
 * Vehicle plan with current plan state.
 * @author David Prochazka
 */
public class VGAVehiclePlan {

    private double discomfort;
	
	public final IOptimalPlanVehicle vgaVehicle;
	
    private final Set<PlanComputationRequest> requests;
    private final Set<PlanComputationRequest> waitingRequests;
    private final Set<PlanComputationRequest> onboardRequests;
    private final List<PlanRequestAction> actions;
	
	private final double startTime;
	
	private double endTime;
	
	
	
	
	

    public VGAVehiclePlan(IOptimalPlanVehicle vgaVehicle, Set<PlanComputationRequest> group){
		this.vgaVehicle = vgaVehicle;
        this.discomfort = 0;
        this.actions = new ArrayList<>();
        this.requests = new LinkedHashSet<>(group);
        this.waitingRequests = new LinkedHashSet<>();
		this.onboardRequests = new LinkedHashSet<>();
		updateAccordingToRequests();
		
		startTime = VehicleGroupAssignmentSolver.getTimeProvider().getCurrentSimTime() / 1000.0;
		endTime = startTime;
    }

    public VGAVehiclePlan(VGAVehiclePlan vehiclePlan){
		this.vgaVehicle = vehiclePlan.vgaVehicle;
        this.discomfort = vehiclePlan.discomfort;
		this.startTime = vehiclePlan.startTime;
		this.endTime = vehiclePlan.endTime;
        this.actions = new ArrayList<>(vehiclePlan.actions);
        this.requests = new LinkedHashSet<>(vehiclePlan.requests);
        this.onboardRequests = new LinkedHashSet<>(vehiclePlan.onboardRequests);
        this.waitingRequests = new LinkedHashSet<>(vehiclePlan.waitingRequests);
    }

    public void add(PlanRequestAction action) {
        recomputeTime(action.getPosition());
		actions.add(action);
        if(action instanceof PlanActionPickup){
            waitingRequests.remove(action.getRequest());
            onboardRequests.add(action.getRequest());
        } else if (action instanceof PlanActionDropoff) {
//            discomfort += getCurrentTime() - action.request.getOriginTime() -
//                    MathUtils.getTravelTimeProvider().getExpectedTravelTime(
//							action.getRequest().getFrom(), action.getRequest().getTo()) / 1000.0;
			discomfort += getCurrentTime() - action.request.getOriginTime() - action.request.getMinTravelTime();
            onboardRequests.remove(action.getRequest());
        }
    }
	
	private void recomputeTime(SimulationNode position) {
		endTime += MathUtils.getTravelTimeProvider().getExpectedTravelTime(getCurrentPosition(), position) / 1000.0;
	}

    SimulationNode getCurrentPosition(){
        if(actions.size() == 0){
            return vgaVehicle.getPosition();
        }

        return actions.get(actions.size() - 1).getPosition();
    }

    public double getDiscomfort() { return discomfort; }

    public double getCurrentTime() {
          return endTime;
    }

	public boolean vehicleHasFreeCapacity(){
		return onboardRequests.size() < vgaVehicle.getCapacity();
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(PlanRequestAction action : actions){
            sb.append(action.toString());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof VGAVehiclePlan)) return false;

        return ((VGAVehiclePlan) obj).vgaVehicle == this.vgaVehicle && obj.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public IOptimalPlanVehicle getVehicle() { return vgaVehicle; }

    public Set<PlanComputationRequest> getRequests() { return requests; }

    public Set<PlanComputationRequest> getWaitingRequests() { return waitingRequests; }

    public Set<PlanComputationRequest> getOnboardRequests() { return onboardRequests; }

    public List<PlanRequestAction> getActions() { return actions; }

	private void updateAccordingToRequests() {
		for(PlanComputationRequest request: requests){
			if(request.isOnboard()){
				onboardRequests.add(request);
				// mazbe check here if the request match the vehicle?
			}
			else{
				waitingRequests.add(request);
			}
		}
	}

}