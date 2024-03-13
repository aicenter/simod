package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolutionPlan {
    private int cost;
    private int departure_time;
    private int arrival_time;
    private DarpSolutionPlanVehicle vehicle;
    private DarpSolutionStopAction[] actions;

    public DarpSolutionPlan(int cost, int departure_time, int arrival_time, DarpSolutionPlanVehicle vehicle, DarpSolutionStopAction[] actions) {
        this.cost = cost;
        this.departure_time = departure_time;
        this.arrival_time = arrival_time;
        this.vehicle = vehicle;
        this.actions = actions;
    }
    

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(int departure_time) {
        this.departure_time = departure_time;
    }

    public int getArrival_time() {
        return arrival_time;
    }

    public void setArrival_time(int arrival_time) {
        this.arrival_time = arrival_time;
    }

    public DarpSolutionPlanVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DarpSolutionPlanVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public DarpSolutionStopAction[] getActions() {
        return actions;
    }

    public void setActions(DarpSolutionStopAction[] actions) {
        this.actions = actions;
    }





}
