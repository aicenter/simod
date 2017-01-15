package cz.agents.amodsim;

import cz.agents.amodsim.configLoader.ConfigBuilder;
import java.io.File;

/**
 *
 * @author F.I.D.O.
 */
public class BuildConfig {
	
	public static File configFile
			= new File("O:\\AIC data/Shared/amod-data/agentpolis-experiment/Prague/default.cfg");
	
	public static void main(String[] args) {
		new ConfigBuilder(configFile).buildConfig();
	}
}
