package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStats;

public class RidesharingBatchStatsPFH extends RidesharingBatchStats {
	public final int failFastTime;

	public final long pfhTime;

	public final int logFailTime;

	public RidesharingBatchStatsPFH(int newRequestCount, long pfhTime, int failFastTime, int logFailTime) {
		super(newRequestCount);
		this.pfhTime = pfhTime;
		this.failFastTime = failFastTime;
		this.logFailTime = logFailTime;
	}
}
