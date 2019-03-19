/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics.content;

/**
 *
 * @author david
 */
public class RidesharingBatchStatsIH extends RidesharingBatchStats{
	public final int failFastTime;
	
	public final int ihTime;
	
	public final int logFailTime;

	public RidesharingBatchStatsIH(int failFastTime, int ihTime, int logFailTime, int newRequestCount) {
		super(newRequestCount);
		this.failFastTime = failFastTime;
		this.ihTime = ihTime;
		this.logFailTime = logFailTime;
	}
	
	
}
