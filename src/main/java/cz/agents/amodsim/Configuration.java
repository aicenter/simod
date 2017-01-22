package cz.agents.amodsim;

import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.configLoader.ConfigParser;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author F.I.D.O.
 */
public class Configuration {
    
    public Config load(){
		try {
			return new Config(new ConfigParser().parseConfigFile(LocalConfig.CONFIG_FILE).getConfig());
		} catch (IOException ex) {
			Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
    }
}
