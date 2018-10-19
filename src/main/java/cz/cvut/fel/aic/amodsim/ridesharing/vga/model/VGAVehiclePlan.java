package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;

import java.util.*;

public class VGAVehiclePlan {

    private static VGAVehiclePlan.CostType costType = VGAVehiclePlan.CostType.STANDARD;

    private double discomfort;

    private RideSharingOnDemandVehicle vehicle;
    private Set<VGARequest> requests;
    private Set<VGARequest> waitingRequests;
    private Set<VGARequest> activeRequests;
    private Map<VGARequest, Double> pickupTimes;
    private List<VGAVehiclePlanAction> actions;

    public VGAVehiclePlan(RideSharingOnDemandVehicle v, Set<VGARequest> requests){
        this.vehicle = v;
        this.discomfort = 0;
        this.actions = new ArrayList<>();
        this.pickupTimes = new HashMap<>();
        this.activeRequests = new LinkedHashSet<>();
        this.requests = new LinkedHashSet<>(requests);
        this.waitingRequests = new LinkedHashSet<>(requests);
    }

    public VGAVehiclePlan(VGAVehiclePlan vehiclePlan){
        this.vehicle = vehiclePlan.vehicle;
        this.discomfort = vehiclePlan.discomfort;
        this.actions = new ArrayList<>(vehiclePlan.actions);
        this.requests = new LinkedHashSet<>(vehiclePlan.requests);
        this.pickupTimes = new HashMap<>(vehiclePlan.pickupTimes);
        this.activeRequests = new LinkedHashSet<>(vehiclePlan.activeRequests);
        this.waitingRequests = new LinkedHashSet<>(vehiclePlan.waitingRequests);
    }

    public void add(VGAVehiclePlanAction action) {
        actions.add(action);
        if(action instanceof VGAVehiclePlanPickup){
            waitingRequests.remove(action.getRequest());
            activeRequests.add(action.getRequest());
            pickupTimes.put(action.getRequest(), action.getTime());
        } else if (action instanceof VGAVehiclePlanDropoff) {
            discomfort += getCurrentTime() - action.getRequest().getOriginTime() -
                    MathUtils.getTravelTimeProvider().getTravelTime(vehicle,
                        action.getRequest().getOriginSimulationNode(), action.getRequest().getDestinationSimulationNode() ) / 1000.0;
            activeRequests.remove(action.getRequest());
            pickupTimes.remove(action.getRequest());
        }
    }

    SimulationNode getCurrentPosition(){
        if(actions.size() == 0){
            return vehicle.getPosition();
        }

        return actions.get(actions.size() - 1).getPosition();
    }

    public double getDiscomfort() { return discomfort; }

    public double getCurrentTime() {
        if(actions.isEmpty()){
            return 0;
        }

        return actions.get(actions.size() - 1).getTime();
    }

    public double calculateCost() {
        if(costType == VGAVehiclePlan.CostType.STANDARD) {
            return MathUtils.round(
                    MathUtils.MINIMIZE_DISCOMFORT * discomfort +
                            (1 - MathUtils.MINIMIZE_DISCOMFORT) * getCurrentTime(),
                    8);
        } else if (costType == VGAVehiclePlan.CostType.SUM_OF_DROPOFF_TIMES) {
            return MathUtils.round(getDropoffTimeSum(), 8);
        }
        return -1;
    }

    public DriverPlan toDriverPlan(){
        if(vehicle == null) { return null; }

        List<DriverPlanTask> tasks = new ArrayList<>();

        tasks.add(new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, vehicle.getPosition()));
        for(VGAVehiclePlanAction action : actions){
            if(action instanceof VGAVehiclePlanPickup) {
                tasks.add(new DriverPlanTask(DriverPlanTaskType.PICKUP, action.getRequest().getDemandAgent(), action.getRequest().getOriginSimulationNode()));
            } else if (action instanceof VGAVehiclePlanDropoff) {
                tasks.add(new DriverPlanTask(DriverPlanTaskType.DROPOFF, action.getRequest().getDemandAgent(), action.getRequest().getDestinationSimulationNode()));
            }
        }

        return new DriverPlan(tasks, (long) (getCurrentTime() * 1000));
    }

    public void updateRequestsBasedOnCurrentSituation() {
        VGAVehicle v = VGAVehicle.getVGAVehicleByRidesharingOnDemandVehicle(vehicle);
        requests.addAll(v.getRequestsOnBoard());
        for(VGARequest request : v.getRequestsOnBoard()) {
            waitingRequests.remove(request);
            activeRequests.add(request);
            pickupTimes.put(request, request.getDemandAgent().getRealPickupTime() / 1000.0);
        }
    }

    private double getDropoffTimeSum() {
        double sum = 0;
        for(VGAVehiclePlanAction action : actions) {
            if(action instanceof VGAVehiclePlanDropoff){
                sum += action.getTime();
            }
        }
        return sum;
    }

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

        return ((VGAVehiclePlan) obj).vehicle == this.vehicle && obj.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public RideSharingOnDemandVehicle getVehicle() { return vehicle; }

    public Set<VGARequest> getRequests() { return requests; }

    public Set<VGARequest> getWaitingRequests() { return waitingRequests; }

    public Set<VGARequest> getActiveRequests() { return activeRequests; }

    public List<VGAVehiclePlanAction> getActions() { return actions; }

    public static VGAVehiclePlan.CostType getCostType() { return costType; }

    public static void setCostType(VGAVehiclePlan.CostType costType) { VGAVehiclePlan.costType = costType; }

    public enum CostType {
        STANDARD,
        SUM_OF_DROPOFF_TIMES
    }

}
