package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;

import java.util.*;

public class VGAVehicle {

    private static Map<OnDemandVehicle, VGAVehicle> agentpolisVehicleToVGA = new LinkedHashMap<>();

    private RideSharingOnDemandVehicle v;
    private Set<VGARequest> promisedRequests;
    private Set<VGARequest> requestsOnBoard;

    private VGAVehicle(RideSharingOnDemandVehicle v) {
        this.v = v;
        promisedRequests = new LinkedHashSet<>();
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

    public RideSharingOnDemandVehicle getRidesharingVehicle() { return v; }

    public Set<VGARequest> getPromisedRequests() { return promisedRequests; }

    public Set<VGARequest> getRequestsOnBoard() { return requestsOnBoard; }

    public void addPromisedRequest(VGARequest request) { promisedRequests.add(request); }

    public void removePromisedRequest(VGARequest request) { promisedRequests.remove(request); }

    public void addRequestOnBoard(VGARequest request) { requestsOnBoard.add(request); }

    public void removeRequestOnBoard(VGARequest request) { requestsOnBoard.remove(request); }

}
