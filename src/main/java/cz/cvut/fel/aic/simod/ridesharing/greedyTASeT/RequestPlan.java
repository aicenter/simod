package cz.cvut.fel.aic.simod.ridesharing.greedyTASeT;

import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;

import java.util.Iterator;
import java.util.List;

public class RequestPlan implements Iterable<PlanAction>{
    public final List<PlanAction> plan;

    public final long totalTime;

    public final double cost;

    public PlanComputationRequest getRequest() {
        return request;
    }

    public void setRequest(PlanComputationRequest request) {
        this.request = request;
    }

    public PlanComputationRequest request;

    public RequestPlan(List<PlanAction> plan, long totalTime, double cost     ) {
        this.plan = plan;
        this.totalTime = totalTime;
        this.cost = cost;
    }

    @Override
    public Iterator<PlanAction> iterator() {
        return plan.iterator();
    }

    // plan for each request, contains onboarding / offboarding actions
}
