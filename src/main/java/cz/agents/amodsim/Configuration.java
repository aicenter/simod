package cz.agents.amodsim;

import cz.agents.amodsim.config.Config;

/**
 *
 * @author F.I.D.O.
 */
public class Configuration {
	
	
    
    public Config load(){
        new BuildConfig().main(new String[]{});
        return new Config();
    }
}
