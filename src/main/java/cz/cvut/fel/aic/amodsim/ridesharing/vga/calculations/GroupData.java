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

    private final Set<PlanComputationRequest> onboardRequestLock;
    private Double feasible;
    public GroupData(Set<PlanComputationRequest> requests) {
        this(requests, null);
    }

    public GroupData(Set<PlanComputationRequest> requests, 
            Set<PlanComputationRequest> onboardRequestLock) {
        this.requests = requests;
        this.onboardRequestLock = onboardRequestLock;
        this.feasible = null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.requests);
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

    public Double getFeasible() {
        return feasible;
    }

    public void setFeasible(double feasible) {
        this.feasible = feasible;
    }


}
