package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStats;

public class RidesharingBatchStatsPFH extends RidesharingBatchStats {
	public final int failFastTime;

	public final int pfhTime;

	public final int logFailTime;

	public RidesharingBatchStatsPFH(int newRequestCount, int failFastTime, int pfhTime, int logFailTime) {
		super(newRequestCount);
		this.failFastTime = failFastTime;
		this.pfhTime = pfhTime;
		this.logFailTime = logFailTime;
	}
}
