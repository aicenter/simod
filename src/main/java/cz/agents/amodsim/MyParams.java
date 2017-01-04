/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.agentpolis.simulator.creator.SimulationParameters;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.config.ConfigReaderException;
import java.io.File;

/**
 *
 * @author david
 */
public class MyParams extends SimulationParameters{
	
	public final File vehicleDataModelFile;
	
	public MyParams(File experimentPath, ConfigReader configReader) throws ConfigReaderException {
		super(experimentPath, configReader);
		vehicleDataModelFile = new File(dataFolder, configReader.getString("vehicledatamodelPath"));
	}
	
}
