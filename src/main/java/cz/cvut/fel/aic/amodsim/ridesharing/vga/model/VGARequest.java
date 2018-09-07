package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.TimeWindow;

public class VGARequest {

    private static long currentId = 0;

    private long id;
    private double originTime;
    private VGANode origin;
    private VGANode destination;
    private DemandAgent demandAgent;

    public static VGARequest newInstance(SimulationNode origin, SimulationNode destination, DemandAgent demandAgent){

        double originTime = demandAgent.getDemandTime();
        double idealTime = MathUtils.getTravelTimeProvider().getTravelTime(VehicleGroupAssignmentSolver.getVehicle(),
                origin, destination);
        double delta = (MathUtils.DELTA_R_MAX - 1) * idealTime;

        VGANode o = VGANode.newInstance(new TimeWindow(originTime, originTime + delta), origin);
        VGANode d = VGANode.newInstance(new TimeWindow(originTime + idealTime, originTime + idealTime + delta), destination);

        return new VGARequest(currentId++, o, d, demandAgent);
    }

    private VGARequest(long id, VGANode origin, VGANode destination, DemandAgent demandAgent){
        this.id = id;
        this.origin = origin;
        this.originTime = demandAgent.getDemandTime();
        this.destination = destination;
        this.demandAgent = demandAgent;
    }

    public long getId() { return id; }

    public double getOriginTime() { return originTime; }

    public VGANode getOrigin() { return origin; }

    public VGANode getDestination() { return destination; }

    public DemandAgent getDemandAgent() { return demandAgent; }

    public SimulationNode getOriginSimulationNode() { return origin.getSimulationNode(); }

    public SimulationNode getDestinationSimulationNode() { return destination.getSimulationNode(); }

    public static void resetIds() { currentId = 0; }

}
