/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.statistics.content;

/**
 *
 * @author david
 */
public class RidesharingBatchStatsVGA extends RidesharingBatchStats{
	public final int activeRequestCount;
	
	public final int groupGenerationTime;
	
	public final int solverTime;
	
	public final double gap;
	
	public final GroupSizeData[] groupSizeData;
	
	public final GroupSizeData[] groupSizeDataPlanExists;

	public RidesharingBatchStatsVGA(int activeRequestCount, int groupGenerationTime, int solverTime,  double gap,
			GroupSizeData[] groupSizeData, GroupSizeData[] groupSizeDataPlanExists, int newRequestCount) {
		super(newRequestCount);
		this.activeRequestCount = activeRequestCount;
		this.groupGenerationTime = groupGenerationTime;
		this.solverTime = solverTime;
		this.gap = gap;
		this.groupSizeData = groupSizeData;
		this.groupSizeDataPlanExists = groupSizeDataPlanExists;
	}
	
	
	
	
	
}


