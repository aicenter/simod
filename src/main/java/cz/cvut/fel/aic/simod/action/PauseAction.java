package cz.cvut.fel.aic.simod.action;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;

import java.time.ZonedDateTime;

public class PauseAction extends TimeWindowAction {

	public PauseAction(TimeProvider timeProvider, ZonedDateTime minTime, ZonedDateTime maxTime) {
		super(timeProvider, null, minTime, maxTime);
	}
}
