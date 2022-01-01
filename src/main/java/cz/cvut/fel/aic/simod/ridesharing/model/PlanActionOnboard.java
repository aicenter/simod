package cz.cvut.fel.aic.simod.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;

public class PlanActionOnboard extends PlanRequestAction {

    private RideSharingOnDemandVehicle toVehicle;

    public PlanActionOnboard(PlanComputationRequest request, SimulationNode location, int maxTime, RideSharingOnDemandVehicle toVehicle) {
        super(request, location, maxTime);
        this.toVehicle = toVehicle;
    }

    public RideSharingOnDemandVehicle getToVehicle() {
        return toVehicle;
    }

    public void setToVehicle(RideSharingOnDemandVehicle toVehicle) {
        this.toVehicle = toVehicle;
    }
}
