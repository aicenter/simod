/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.configLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fido
 */
public class ConfigExample {
    public static void main(String[] args) {
        ConfigParser parser = new ConfigParser();
        try {
            Config config = parser.parseConfigFile(
                    new File("/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/default.cfg"));
        } catch (IOException ex) {
            Logger.getLogger(ConfigExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
