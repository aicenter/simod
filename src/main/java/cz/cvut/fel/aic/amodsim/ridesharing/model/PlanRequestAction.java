package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public abstract class PlanRequestAction extends PlanAction{

    public final PlanComputationRequest request;
	
	
	/**
	 * Time constraint in seconds
	 */
	private final int maxTime;
	
	public PlanComputationRequest getRequest() { 
		return request; 
	}

	/**
	 * Getter for max time.
	 * @return Time constraint in seconds
	 */
	public int getMaxTime() {
		return maxTime;
	}
	
	

    public PlanRequestAction(PlanComputationRequest request, SimulationNode location, int maxTime) {
		super(location);
        this.request = request;
		this.maxTime = maxTime;
    }


}
