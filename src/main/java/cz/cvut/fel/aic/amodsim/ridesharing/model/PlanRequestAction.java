package cz.cvut.fel.aic.amodsim.ridesharing.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public abstract class PlanRequestAction extends PlanAction{

    public final PlanComputationRequest request;
	
	
	
	private final int maxTime;
	
	public PlanComputationRequest getRequest() { 
		return request; 
	}


	public int getMaxTime() {
		return maxTime;
	}
	
	

    public PlanRequestAction(PlanComputationRequest request, SimulationNode location, int maxTime) {
		super(location);
        this.request = request;
		this.maxTime = maxTime;
    }


}
