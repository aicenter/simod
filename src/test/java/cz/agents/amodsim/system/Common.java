/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import cz.agents.amodsim.config.Config;
import java.io.File;

/**
 *
 * @author fido
 */
public class Common {
    
    public static void setTestResultsDir(Config config, String testDirName){
        config.agentpolis.statistics.resultFilePath = 
                appendDir(config.agentpolis.statistics.resultFilePath, testDirName); 
        config.agentpolis.statistics.allEdgesLoadHistoryFilePath = 
                appendDir(config.agentpolis.statistics.allEdgesLoadHistoryFilePath, testDirName);
        config.agentpolis.statistics.transitStatisticFilePath = 
                appendDir(config.agentpolis.statistics.transitStatisticFilePath, testDirName);
        config.agentpolis.statistics.tripDistancesFilePath = 
                appendDir(config.agentpolis.statistics.tripDistancesFilePath, testDirName);
        
        // on demand vehicle statistics
        config.agentpolis.statistics.onDemandVehicleStatistic.leaveStationFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.leaveStationFilePath, testDirName, 2);
        config.agentpolis.statistics.onDemandVehicleStatistic.pickupFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.pickupFilePath, testDirName, 2);
        config.agentpolis.statistics.onDemandVehicleStatistic.dropOffFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.dropOffFilePath, testDirName, 2);
        config.agentpolis.statistics.onDemandVehicleStatistic.reachNearestStationFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.reachNearestStationFilePath,
                        testDirName, 2);
        config.agentpolis.statistics.onDemandVehicleStatistic.startRebalancingFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.startRebalancingFilePath,
                        testDirName, 2);
        config.agentpolis.statistics.onDemandVehicleStatistic.finishRebalancingFilePath = 
                insertDir(config.agentpolis.statistics.onDemandVehicleStatistic.finishRebalancingFilePath,
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
