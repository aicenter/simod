package cz.cvut.fel.aic.simod.action;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class TimeWindowAction extends PlanAction {

	private final int minTime;

	/**
	 * Time constraint in seconds
	 */
	private final int maxTime;



	/**
	 * Getter for max time.
	 * @return Time constraint in seconds
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * Getter for min time.
	 * @return Time constraint in seconds
	 */
	public int getMinTime() {
		return minTime;
	}



	public TimeWindowAction(SimulationNode location, int minTime, int maxTime) {
		super(location);
		this.minTime = minTime;
		this.maxTime = maxTime;
	}
}
