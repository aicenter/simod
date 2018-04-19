package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

/**
 *
 * @author F.I.D.O.
 */
public class OnDemandRequest {
	private final DemandAgent demandAgent;

	public DemandAgent getDemandAgent() {
		return demandAgent;
	}
	
	

	public OnDemandRequest(DemandAgent demandAgent) {
		this.demandAgent = demandAgent;
	}
	
	public final SimulationNode getPosition(){
		return demandAgent.getPosition();
	}
}
