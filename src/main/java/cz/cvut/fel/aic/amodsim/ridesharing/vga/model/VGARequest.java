package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;


public class VGARequest implements PlanComputationRequest{
	
	public final int id;
	
    private final int originTime;

    private final DemandAgent demandAgent;
	
	private final VGAVehiclePlanPickup pickUpAction;
	
	private final VGAVehiclePlanDropoff dropOffAction;
	
	private final int minTravelTime;
	

	private boolean onboard;

	
	
	
	
	@Override
	public VGAVehiclePlanPickup getPickUpAction() {
		return pickUpAction;
	}

	@Override
	public VGAVehiclePlanDropoff getDropOffAction() {
		return dropOffAction;
	}

	@Override
	public boolean isOnboard() {
		return onboard;
	}

	public void setOnboard(boolean onboard) {
		this.onboard = onboard;
	}
	
	


	@Inject
    private VGARequest(@Assisted int id, AmodsimConfig amodsimConfig, @Assisted("origin") SimulationNode origin, 
			@Assisted("destination") SimulationNode destination, @Assisted DemandAgent demandAgent){
		this.id = id;
		
		originTime = (int) Math.round(demandAgent.getDemandTime() / 1000.0);
        minTravelTime = (int) Math.round(
				MathUtils.getTravelTimeProvider().getExpectedTravelTime(origin, destination) / 1000.0);
        int maxProlongation = (int) Math.round(
				amodsimConfig.amodsim.ridesharing.vga.maximumRelativeDiscomfort * minTravelTime);
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;
		
        this.demandAgent = demandAgent;
		onboard = false;
		
		pickUpAction = new VGAVehiclePlanPickup(this, origin, maxPickUpTime);
		dropOffAction = new VGAVehiclePlanDropoff(this, destination, maxDropOffTime);
    }

    public int getOriginTime() { 
		return originTime; 
	}

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
		return String.format("%s - from: %s to: %s", demandAgent, getFrom(), getTo());
	}

	@Override
	public int getMaxPickupTime() {
		return pickUpAction.getMaxTime();
	}

	@Override
	public int getMaxDropoffTime() {
		return dropOffAction.getMaxTime();
	}

	@Override
	public int getMinTravelTime() {
		return minTravelTime;
	}

	@Override
	public SimulationNode getFrom() {
		return pickUpAction.getPosition();
	}

	@Override
	public SimulationNode getTo() {
		return dropOffAction.getPosition();
	}
	
	
	
	public interface VGARequestFactory {
        public VGARequest create(int id, @Assisted("origin") SimulationNode origin, 
				@Assisted("destination") SimulationNode destination, DemandAgent demandAgent);
    }

}
