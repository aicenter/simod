package cz.cvut.fel.aic.simod.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;

public class PlanActionOffboard extends PlanRequestAction{

    private RideSharingOnDemandVehicle fromVehicle;

    public PlanActionOffboard(PlanComputationRequest request, SimulationNode location, int maxTime, RideSharingOnDemandVehicle fromVehicle) {
        super(request, location, maxTime);
        this.fromVehicle = fromVehicle;
    }

    public RideSharingOnDemandVehicle getFromVehicle() {
        return fromVehicle;
    }

    public void setFromVehicle(RideSharingOnDemandVehicle fromVehicle) {
        this.fromVehicle = fromVehicle;
    }
}
