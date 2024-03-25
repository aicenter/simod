package cz.cvut.fel.aic.simod.action;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

import java.time.ZonedDateTime;

public class TimeWindowAction extends PlanAction {

	private final TimeProvider timeProvider;

	protected final ZonedDateTime minTime;

	/**
	 * Time constraint in seconds
	 */
	private final ZonedDateTime maxTime;


	public ZonedDateTime getMinTime() {
		return minTime;
	}

	public ZonedDateTime getMaxTime() {
		return maxTime;
	}


	public TimeWindowAction(
		TimeProvider timeProvider,
		SimulationNode location,
		ZonedDateTime minTime,
		ZonedDateTime maxTime
	) {
		super(location);
		this.timeProvider = timeProvider;
		this.minTime = minTime;
		this.maxTime = maxTime;
	}


	/**
	 * Getter for max time.
	 *
	 * @return Time constraint in seconds
	 */
	public int getMaxTimeInSimulationTimeSeconds() {
		return (int) (timeProvider.getSimTimeFromDateTime(maxTime) / 1000);
	}

	/**
	 * Getter for min time.
	 *
	 * @return Time constraint in seconds
	 */
	public int getMinTimeInSimulationTimeSeconds() {
		return (int) (timeProvider.getSimTimeFromDateTime(minTime) / 1000);
	}
}
