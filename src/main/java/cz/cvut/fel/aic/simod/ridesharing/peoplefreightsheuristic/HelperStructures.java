package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanRequestAction;

import java.util.Comparator;
import java.util.List;

/**
 * comparator for sorting Requests
 */
class SortRequestsByOriginTime implements Comparator<PlanComputationRequest> {
	public int compare(PlanComputationRequest a, PlanComputationRequest b) {
		return a.getOriginTime() - b.getOriginTime();
	}
}

class SortActionsByMaxTime implements Comparator<PlanAction> {
	public int compare(PlanAction a, PlanAction b) {
		return ((PlanRequestAction) a).getMaxTime() - ((PlanRequestAction) b).getMaxTime();
	}
}

// structure to store a taxi schedule, it's duration and it's cost
class ScheduleWithDuration {
	protected final List<PlanAction> schedule;
	protected final long duration;
	protected final double planCost;

	public ScheduleWithDuration(List<PlanAction> schedule, long duration, double planCost) {
		this.schedule = schedule;
		this.duration = duration;
		this.planCost = planCost;
	}
}

class TimeWindow {
	protected long earlyTime;
	protected long lateTime;

	public TimeWindow(long earlyTime, long lateTime) {
		this.earlyTime = earlyTime;
		this.lateTime = lateTime;
	}
}
