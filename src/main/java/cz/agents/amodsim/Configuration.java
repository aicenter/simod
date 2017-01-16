package cz.agents.amodsim;

import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.configLoader.ConfigBuilder;
import java.io.File;

/**
 *
 * @author F.I.D.O.
 */
public class Configuration {
	
	public static File configFile
			= new File("/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/default.cfg");
	
	public static void main(String[] args) {
		new ConfigBuilder(configFile).buildConfig();
	}
    
    public Config load(){
        new ConfigBuilder(configFile).buildConfig();
        return new Config();
    }
}
