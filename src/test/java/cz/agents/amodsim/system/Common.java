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
        config.agentpolis.statistics.carLeftStationToServeDemandTimesFilePath = 
                appendDir(config.agentpolis.statistics.carLeftStationToServeDemandTimesFilePath, testDirName);
        config.agentpolis.statistics.demandServiceStatisticFilePath = 
                appendDir(config.agentpolis.statistics.demandServiceStatisticFilePath, testDirName);
        config.agentpolis.statistics.transitStatisticFilePath = 
                appendDir(config.agentpolis.statistics.transitStatisticFilePath, testDirName);
    }

    private static String appendDir(String path, String dirName) {
        int lastSeparatorIndex = path.lastIndexOf(File.separatorChar);
        String newPath = path.substring(0, lastSeparatorIndex + 1) + dirName + File.separator + path.substring(lastSeparatorIndex + 1);
        return newPath;
    }
}
