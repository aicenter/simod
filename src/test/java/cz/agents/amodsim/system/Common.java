/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.agents.amodsim.system;

import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import java.io.File;

/**
 *
 * @author fido
 */
public class Common {
	
	public static void setTestResultsDir(AmodsimConfig config, String testDirName){
		config.statistics.resultFilePath = 
				appendDir(config.statistics.resultFilePath, testDirName); 
		config.statistics.allEdgesLoadHistoryFilePath = 
				appendDir(config.statistics.allEdgesLoadHistoryFilePath, testDirName);
		config.statistics.transitStatisticFilePath = 
				appendDir(config.statistics.transitStatisticFilePath, testDirName);
		config.statistics.tripDistancesFilePath = 
				appendDir(config.statistics.tripDistancesFilePath, testDirName);
		
		// on demand vehicle statistics
		config.statistics.onDemandVehicleStatistic.leaveStationFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.leaveStationFilePath, testDirName, 2);
		config.statistics.onDemandVehicleStatistic.pickupFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.pickupFilePath, testDirName, 2);
		config.statistics.onDemandVehicleStatistic.dropOffFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.dropOffFilePath, testDirName, 2);
		config.statistics.onDemandVehicleStatistic.reachNearestStationFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.reachNearestStationFilePath,
						testDirName, 2);
		config.statistics.onDemandVehicleStatistic.startRebalancingFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.startRebalancingFilePath,
						testDirName, 2);
		config.statistics.onDemandVehicleStatistic.finishRebalancingFilePath = 
				insertDir(config.statistics.onDemandVehicleStatistic.finishRebalancingFilePath,
						testDirName, 2);
	}

	private static String appendDir(String path, String dirName) {
		int lastSeparatorIndex = path.lastIndexOf(File.separatorChar);
		String newPath = path.substring(0, lastSeparatorIndex + 1) + dirName + File.separator + path.substring(lastSeparatorIndex + 1);
		return newPath;
	}
	
	private static String insertDir(String path, String dirName, int position) {
		int searchStartIndex = path.length() - 1;
		int separatorIndex = path.length() - 1;
		for (int i = 0; i < position; i++) {
			separatorIndex =  path.lastIndexOf(File.separatorChar, searchStartIndex - 1);
			searchStartIndex = separatorIndex;
		}
		
		String newPath = path.substring(0, separatorIndex + 1) + dirName + File.separator 
				+ path.substring(separatorIndex + 1);
		return newPath;
	}
}
