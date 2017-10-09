/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        config.amodsim.statistics.resultFilePath = 
                appendDir(config.amodsim.statistics.resultFilePath, testDirName); 
        config.amodsim.statistics.allEdgesLoadHistoryFilePath = 
                appendDir(config.amodsim.statistics.allEdgesLoadHistoryFilePath, testDirName);
        config.amodsim.statistics.transitStatisticFilePath = 
                appendDir(config.amodsim.statistics.transitStatisticFilePath, testDirName);
        config.amodsim.statistics.tripDistancesFilePath = 
                appendDir(config.amodsim.statistics.tripDistancesFilePath, testDirName);
        
        // on demand vehicle statistics
        config.amodsim.statistics.onDemandVehicleStatistic.leaveStationFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.leaveStationFilePath, testDirName, 2);
        config.amodsim.statistics.onDemandVehicleStatistic.pickupFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.pickupFilePath, testDirName, 2);
        config.amodsim.statistics.onDemandVehicleStatistic.dropOffFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.dropOffFilePath, testDirName, 2);
        config.amodsim.statistics.onDemandVehicleStatistic.reachNearestStationFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.reachNearestStationFilePath,
                        testDirName, 2);
        config.amodsim.statistics.onDemandVehicleStatistic.startRebalancingFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.startRebalancingFilePath,
                        testDirName, 2);
        config.amodsim.statistics.onDemandVehicleStatistic.finishRebalancingFilePath = 
                insertDir(config.amodsim.statistics.onDemandVehicleStatistic.finishRebalancingFilePath,
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
