package cz.cvut.fel.aic.amodsim.ridesharing.plan;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlanTask {
	private final DriverPlanTaskType taskType;
	public final DemandAgent demandAgent;
	private final SimulationNode location;
    public DriverPlanTaskType getTaskType() {
		return taskType;
	}

	public DemandAgent getDemandAgent() {
		return demandAgent;
	}
	public SimulationNode getLocation() {
		return location;
	}
	
	public DriverPlanTask(DriverPlanTaskType taskType, DemandAgent demandAgent, SimulationNode location) {
		this.taskType = taskType;
		this.demandAgent = demandAgent;
		this.location = location;
	}
	
	
	
}
