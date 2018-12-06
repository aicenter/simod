package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;

import java.util.*;

/**
 * Vehicle plan with current plan state.
 * @author David Prochazka
 */
public class VGAVehiclePlan {

    private double discomfort;
	
	public final IOptimalPlanVehicle vgaVehicle;
	
    private final Set<VGARequest> requests;
    private final Set<VGARequest> waitingRequests;
    private final Set<VGARequest> onboardRequests;
    private final List<VGAVehiclePlanAction> actions;
	
	private double endTime;
	
	private double startTime;
	
	
	

    public VGAVehiclePlan(IOptimalPlanVehicle vgaVehicle, Set<VGARequest> group){
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

    public void add(VGAVehiclePlanAction action) {
        recomputeTime(action.getPosition());
		actions.add(action);
        if(action instanceof VGAVehiclePlanPickup){
            waitingRequests.remove(action.getRequest());
            onboardRequests.add(action.getRequest());
        } else if (action instanceof VGAVehiclePlanDropoff) {
            discomfort += getCurrentTime() - action.request.getOriginTime() -
                    MathUtils.getTravelTimeProvider().getExpectedTravelTime(
							action.getRequest().from, action.getRequest().to) / 1000.0;
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

    

    public DriverPlan toDriverPlan(){
        if(vgaVehicle == null) { return null; }

        List<DriverPlanTask> tasks = new ArrayList<>();

        tasks.add(new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, vgaVehicle.getPosition()));
        for(VGAVehiclePlanAction action : actions){
            if(action instanceof VGAVehiclePlanPickup) {
                tasks.add(new DriverPlanTask(DriverPlanTaskType.PICKUP, action.getRequest().getDemandAgent(), 
						action.getRequest().from));
            } else if (action instanceof VGAVehiclePlanDropoff) {
                tasks.add(new DriverPlanTask(DriverPlanTaskType.DROPOFF, action.getRequest().getDemandAgent(), 
						action.getRequest().to));
            }
        }

        return new DriverPlan(tasks, (long) (getCurrentTime() * 1000));
    }
	
	public boolean vehicleHasFreeCapacity(){
		return onboardRequests.size() < vgaVehicle.getCapacity();
	}

//    public double getDropoffTimeSum() {
//        double sum = 0;
//        for(VGAVehiclePlanAction action : actions) {
//            if(action instanceof VGAVehiclePlanDropoff){
//                sum += action.getTime();
//            }
//        }
//        return sum;
//    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(VGAVehiclePlanAction action : actions){
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

    public Set<VGARequest> getRequests() { return requests; }

    public Set<VGARequest> getWaitingRequests() { return waitingRequests; }

    public Set<VGARequest> getOnboardRequests() { return onboardRequests; }

    public List<VGAVehiclePlanAction> getActions() { return actions; }

	private void updateAccordingToRequests() {
		for(VGARequest request: requests){
			if(request.isOnboard()){
				onboardRequests.add(request);
				discomfort += request.getDiscomfort();
				// mazbe check here if the request match the vehicle?
			}
			else{
				waitingRequests.add(request);
			}
		}
	}

    public enum CostType {
        STANDARD,
        SUM_OF_DROPOFF_TIMES
    }

}
