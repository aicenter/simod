/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics.content;

import java.util.Map;

/**
 *
 * @author david
 */
public class RidesharingBatchStatsVGA extends RidesharingBatchStats{
	public final int activeRequestCount;
	
	public final int groupGenerationTime;
	
	public final int solverTime;
	
	public final GroupSizeData[] groupSizeData;
	
	public final GroupSizeData[] groupSizeDataPlanExists;

	public RidesharingBatchStatsVGA(int activeRequestCount, int groupGenerationTime, int solverTime, 
			GroupSizeData[] groupSizeData, GroupSizeData[] groupSizeDataPlanExists, int newRequestCount) {
		super(newRequestCount);
		this.activeRequestCount = activeRequestCount;
		this.groupGenerationTime = groupGenerationTime;
		this.solverTime = solverTime;
		this.groupSizeData = groupSizeData;
		this.groupSizeDataPlanExists = groupSizeDataPlanExists;
	}
	
	
	
	
	
}


