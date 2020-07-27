/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim;


import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.init.StationsInitializer;
import java.io.File;

import java.net.MalformedURLException;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class MapVisualizer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapVisualizer.class);
	
	public static void main(String[] args) throws MalformedURLException {
		new MapVisualizer().run(args);
	}


	public void run(String[] args) {
		AmodsimConfig config = new AmodsimConfig();
		
		File localConfigFile = args.length > 0 ? new File(args[0]) : null;
		
                LOGGER.debug("Load ALL metadata");
		Injector injector = new AgentPolisInitializer(new MapVisualiserModule(config, localConfigFile)).initialize();
		
		injector.getInstance(AgentpolisConfig.class).visio.showVisio = true;

		SimulationCreator creator = injector.getInstance(SimulationCreator.class);

		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());
		
		// load stations
		injector.getInstance(StationsInitializer.class).loadStations();


		creator.startSimulation();

	}
}
