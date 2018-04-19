package cz.cvut.fel.aic.amodsim.ridesharing;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

/**
 *
 * @author F.I.D.O.
 */
public class OnDemandRequest {
	private final DemandAgent demandAgent;
	
	private final SimulationNode targetLocation;

	public DemandAgent getDemandAgent() {
		return demandAgent;
	}

	public SimulationNode getTargetLocation() {
		return targetLocation;
	}
	
	
	

	public OnDemandRequest(DemandAgent demandAgent, SimulationNode targetLocation) {
		this.demandAgent = demandAgent;
		this.targetLocation = targetLocation;
	}
	
	public final SimulationNode getPosition(){
		return demandAgent.getPosition();
	}
}
