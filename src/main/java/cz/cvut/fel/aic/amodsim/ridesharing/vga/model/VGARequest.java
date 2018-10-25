package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;


public class VGARequest {
    private final double originTime;
    private final VGANode origin;
    private final VGANode destination;
    private final DemandAgent demandAgent;

	private boolean onboard;
	
	private double discomfort;

	public boolean isOnboard() {
		return onboard;
	}

	public void setOnboard(boolean onboard) {
		this.onboard = onboard;
	}

	public double getDiscomfort() {
		return discomfort;
	}

	public void setDiscomfort(double discomfort) {
		this.discomfort = discomfort;
	}
	
	


	@Inject
    private VGARequest(AmodsimConfig amodsimConfig, @Assisted("origin") SimulationNode origin, 
			@Assisted("destination") SimulationNode destination, @Assisted DemandAgent demandAgent){
		double originTime = demandAgent.getDemandTime() / 1000.0;
        double idealTime = MathUtils.getTravelTimeProvider().getExpectedTravelTime(origin, destination) / 1000.0;
        double delta = (amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort - 1) * idealTime;

        VGANode o = VGANode.newInstance(new TimeWindow(originTime, originTime + delta), origin);
        VGANode d = VGANode.newInstance(new TimeWindow(originTime + idealTime, originTime + idealTime + delta), destination);
		
        this.origin = o;
        this.originTime = demandAgent.getDemandTime() / 1000.0;
        this.destination = d;
        this.demandAgent = demandAgent;
		onboard = false;
		discomfort = 0;
    }

    public double getOriginTime() { return originTime; }

    public VGANode getOrigin() { return origin; }

    public VGANode getDestination() { return destination; }

    public DemandAgent getDemandAgent() { return demandAgent; }

    public SimulationNode getOriginSimulationNode() { return origin.getSimulationNode(); }

    public SimulationNode getDestinationSimulationNode() { return destination.getSimulationNode(); }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof VGARequest)) return false;
        return demandAgent.toString().equals(((VGARequest) obj).demandAgent.toString());
    }

    @Override
    public int hashCode() {
        return demandAgent.hashCode();
    }

	@Override
	public String toString() {
		return String.format("%s - from: %s to: %s", demandAgent, origin, destination);
	}
	
	
	
	public interface VGARequestFactory {
        public VGARequest create(@Assisted("origin") SimulationNode origin, 
				@Assisted("destination") SimulationNode destination, DemandAgent demandAgent);
    }

}
