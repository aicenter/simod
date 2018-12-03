package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;


public class VGARequest {
    private final double originTime;

    private final DemandAgent demandAgent;

	private boolean onboard;
	
	private double discomfort;
	
	public final SimulationNode from;
	
	public final SimulationNode to;
	
	public final double minTravelTime;
	
	public final double maxPickUpTime;
	
	public final double maxDropOffTime;
	
	
	

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
		from = origin;
		to = destination;
		
		originTime = demandAgent.getDemandTime() / 1000.0;
        minTravelTime = MathUtils.getTravelTimeProvider().getExpectedTravelTime(origin, destination) / 1000.0;
        double maxProlongation = (amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort) * minTravelTime;
		
		maxPickUpTime = originTime + maxProlongation;
		maxDropOffTime = originTime + minTravelTime + maxProlongation;
		
        this.demandAgent = demandAgent;
		onboard = false;
		discomfort = 0;
    }

    public double getOriginTime() { return originTime; }

    public DemandAgent getDemandAgent() { return demandAgent; }

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
		return String.format("%s - from: %s to: %s", demandAgent, from, to);
	}
	
	
	
	public interface VGARequestFactory {
        public VGARequest create(@Assisted("origin") SimulationNode origin, 
				@Assisted("destination") SimulationNode destination, DemandAgent demandAgent);
    }

}
