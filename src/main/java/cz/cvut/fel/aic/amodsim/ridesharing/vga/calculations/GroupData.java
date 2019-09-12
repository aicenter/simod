/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author matal
 */
public class GroupData {
    private final Set<PlanComputationRequest> requests;
    private final IOptimalPlanVehicle vehicle;
    private final Set<PlanComputationRequest> onboardRequestLock;
    private double feasible;
    private int hash;
    public GroupData(Set<PlanComputationRequest> requests) {
        this(requests, null, null);
    }
    public GroupData(Set<PlanComputationRequest> requests, IOptimalPlanVehicle vehicle) {
        this(requests, null, vehicle);
    }
    public GroupData(Set<PlanComputationRequest> requests, 
            Set<PlanComputationRequest> onboardRequestLock) {
        this(requests, onboardRequestLock, null);
    }
    public GroupData(Set<PlanComputationRequest> requests, 
            Set<PlanComputationRequest> onboardRequestLock, IOptimalPlanVehicle vehicle) {
        this.requests = requests;
        this.onboardRequestLock = onboardRequestLock;
        this.feasible = 0;
        this.vehicle = vehicle;
        this.hash = 0;
    }

    @Override
    public int hashCode() {
	if(hash == 0){	
	hash = this.requests.hashCode() % 1_200_000;	
	}
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GroupData other = (GroupData) obj;
        if (!Objects.equals(this.requests, other.requests)) {
            return false;
        }
        return true;
    }

    public Set<PlanComputationRequest> getRequests() {
        return requests;
    }

    public Set<PlanComputationRequest> getOnboardRequestLock() {
        return onboardRequestLock;
    }

    public double getFeasible() {
        return feasible;
    }

    public void setFeasible(double feasible) {
        this.feasible = feasible;
    }

    public IOptimalPlanVehicle getVehicle() {
        return vehicle;
    }


}
