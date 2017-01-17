/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.agentpolis.AgentPolisConfiguration;
import cz.agents.amodsim.config.Config;
import java.io.File;

/**
 *
 * @author fido
 */
public class AmodsimAgentPolisConfiguration extends AgentPolisConfiguration{
    
    public AmodsimAgentPolisConfiguration(Config config) {
        super(false, "", config.srid, (long) config.agentpolis.simulationDurationInMillis, new File(config.mapFilePath),
                false, false, null, config.agentpolis.showVisio, "", mpsToKmph(config.vehicleSpeedInMeters), "", null);
    }

    private static double mpsToKmph(double speedInMps){
        return speedInMps * 3600 / 1000;
    }
    
}
