package cz.cvut.fel.aic.amodsim.ridesharing.model;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;


public class DefaultPlanComputationRequest implements PlanComputationRequest{
	
	public final int id;
	
	/**
	 * Request origin time in seconds.
	 */
    private final int originTime;

    private final DemandAgent demandAgent;
	
	private final PlanActionPickup pickUpAction;
	
	private final PlanActionDropoff dropOffAction;
	
	/**
	 * Min travel time in seconds
	 */
	private final int minTravelTime;
	

	private boolean onboard;

	
	
	
	
	@Override
	public PlanActionPickup getPickUpAction() {
		return pickUpAction;
	}

	@Override
	public PlanActionDropoff getDropOffAction() {
		return dropOffAction;
	}

	@Override
	public boolean isOnboard() {
		return onboard;
	}

	public void setOnboard(boolean onboard) {
		this.onboard = onboard;
	}
	
	@Override
	public DemandAgent getDemandAgent() { 
		return demandAgent; 
	}
	


	@Inject
    private DefaultPlanComputationRequest(TravelTimeProvider travelTimeProvider, @Assisted int id, 
			AmodsimConfig amodsimConfig, @Assisted("origin") SimulationNode origin, 
			@Assisted("destination") SimulationNode destination, @Assisted DemandAgent demandAgent){
		this.id = id;
		
		originTime = (int) Math.round(demandAgent.getDemandTime() / 1000.0);
        minTravelTime = (int) Math.round(
				travelTimeProvider.getExpectedTravelTime(origin, destination) / 1000.0);
		
		int maxProlongation;
		if(amodsimConfig.ridesharing.discomfortConstrain.equals("absolute")){
			maxProlongation = amodsimConfig.ridesharing.maxProlongationInSeconds;
		}
		else{
			maxProlongation = (int) Math.round(
				amodsimConfig.ridesharing.maximumRelativeDiscomfort * minTravelTime);
		}		
		
		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;
		
        this.demandAgent = demandAgent;
		onboard = false;
		
		pickUpAction = new PlanActionPickup(this, origin, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(this, destination, maxDropOffTime);
    }

	@Override
    public int getOriginTime() { 
		return originTime; 
	}

    

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DefaultPlanComputationRequest)) return false;
        return demandAgent.toString().equals(((DefaultPlanComputationRequest) obj).demandAgent.toString());
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
	
	
	
	public interface DefaultPlanComputationRequestFactory {
        public DefaultPlanComputationRequest create(int id, @Assisted("origin") SimulationNode origin, 
				@Assisted("destination") SimulationNode destination, DemandAgent demandAgent);
    }

}
