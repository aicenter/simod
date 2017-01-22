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
    
	public static void main(String[] args) {
		new ConfigBuilder(LocalConfig.CONFIG_FILE).buildConfig();
	}
}
