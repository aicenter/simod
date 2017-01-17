/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.amodsim.configLoader.ConfigBuilder;
import java.io.File;

/**
 *
 * @author fido
 */
public class BuildConfig {
    public static File configFile
			= new File("/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/default.cfg");
	
	public static void main(String[] args) {
		new ConfigBuilder(configFile).buildConfig();
	}
}
