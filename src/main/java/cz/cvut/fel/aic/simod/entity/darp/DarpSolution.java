package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolution {
    private boolean feasible;
    private int cost;
    private int cost_minutes;
    private DarpSolutionPlan[] plans;
    private DarpSolutionDroppedRequest[] dropped_requests;

    public DarpSolution(boolean feasible, int cost, int cost_minutes, DarpSolutionPlan[] plans, DarpSolutionDroppedRequest[] dropped_requests) {
        this.feasible = feasible;
        this.cost = cost;
        this.cost_minutes = cost_minutes;
        this.plans = plans;
        this.dropped_requests = dropped_requests;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getCost_minutes() {
        return cost_minutes;
    }

    public void setCost_minutes(int cost_minutes) {
        this.cost_minutes = cost_minutes;
    }

    public DarpSolutionPlan[] getPlans() {
        return plans;
    }

    public void setPlans(DarpSolutionPlan[] plans) {
        this.plans = plans;
    }

    public DarpSolutionDroppedRequest[] getDropped_requests() {
        return dropped_requests;
    }

    public void setDropped_requests(DarpSolutionDroppedRequest[] dropped_requests) {
        this.dropped_requests = dropped_requests;
    }
}
