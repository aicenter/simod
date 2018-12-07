package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;

import java.util.*;

public class VGAVehicle implements IOptimalPlanVehicle{

    private static Map<OnDemandVehicle, VGAVehicle> agentpolisVehicleToVGA = new LinkedHashMap<>();

    private final RideSharingOnDemandVehicle onDemandVehicle;
    private final LinkedHashSet<PlanComputationRequest> requestsOnBoard;

    private VGAVehicle(RideSharingOnDemandVehicle v) {
        this.onDemandVehicle = v;
        requestsOnBoard = new LinkedHashSet<>();
        agentpolisVehicleToVGA.put(v, this);
    }

    public static void resetMapping() {
        agentpolisVehicleToVGA = new LinkedHashMap<>();
    }

    public static VGAVehicle newInstance(RideSharingOnDemandVehicle v){
        return new VGAVehicle(v);
    }

    public static VGAVehicle getVGAVehicleByRidesharingOnDemandVehicle( OnDemandVehicle v) {
        return agentpolisVehicleToVGA.get(v);
    }

    public RideSharingOnDemandVehicle getRidesharingVehicle() { return onDemandVehicle; }

	@Override
    public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() { 
		return requestsOnBoard; 
	}

    public void addRequestOnBoard(VGARequest request) { 
		requestsOnBoard.add(request); 
	}

    public void removeRequestOnBoard(VGARequest request) { 
		requestsOnBoard.remove(request); 
	}

	@Override
	public String toString() {
		return String.format("VGA vehicle: %s", onDemandVehicle.getId());
	}

	@Override
	public SimulationNode getPosition() {
		return onDemandVehicle.getPosition();
	}

	@Override
	public int getCapacity() {
		return onDemandVehicle.getCapacity();
	}
	
	

}
